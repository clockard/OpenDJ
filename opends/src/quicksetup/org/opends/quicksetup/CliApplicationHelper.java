/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Portions Copyright 2007 Sun Microsystems, Inc.
 */

package org.opends.quicksetup;


import org.opends.admin.ads.util.ApplicationTrustManager;
import org.opends.admin.ads.util.ConnectionUtils;
import org.opends.guitools.statuspanel.ConnectionProtocolPolicy;
import org.opends.quicksetup.ui.CertificateDialog;
import org.opends.messages.Message;
import org.opends.messages.MessageBuilder;
import static org.opends.messages.AdminToolMessages.*;
import static org.opends.messages.ToolMessages.*;
import static org.opends.messages.QuickSetupMessages.*;
import static org.opends.server.tools.ToolConstants.*;

import org.opends.quicksetup.util.Utils;
import org.opends.server.util.args.ArgumentParser;
import org.opends.server.util.args.ArgumentException;
import org.opends.server.util.args.BooleanArgument;
import org.opends.server.util.PasswordReader;
import org.opends.server.util.StaticUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.ldap.InitialLdapContext;

/**
 * Helper class containing useful methods for processing input and output
 * for a CliApplication.
 */
public abstract class CliApplicationHelper {

  static private final Logger LOG =
          Logger.getLogger(CliApplicationHelper.class.getName());

  /** Format string used for deriving the console prompt. */
  static public final String PROMPT_DEFAULT_FORMAT = "%s%n[%s]:";

  /** Format string used for deriving the console prompt. */
  static public final String PROMPT_NO_DEFAULT_FORMAT = "%s";

  private BooleanArgument noPromptArg = null;

  private BooleanArgument quietArg = null;

  /** The print stream to use for standard error. */
  protected PrintStream err;

  /** The print stream to use for standard output. */
  protected PrintStream out;

  /** The input stream. */
  protected InputStream in;

  /**
   * Constructor for the CliApplicationHelper object.
   *
   * @param  out the print stream to use for standard output.
   * @param  err the print stream to use for standard error.
   * @param  in the input stream to use for standard input.
   */
  protected CliApplicationHelper(PrintStream out, PrintStream err,
      InputStream in)
  {
    this.out = out;
    this.err = err;
    this.in = in;
  }

  /**
   * Interactively prompts (on standard output) the user to provide a string
   * value.  Any non-empty string will be allowed (the empty string will
   * indicate that the default should be used).  The method will display the
   * message until the user provides one of the values in the validValues
   * parameter.
   *
   * @param  prompt        The prompt to present to the user.
   * @param  defaultValue  The default value returned if the user clicks enter.
   * @param  validValues   The valid values that can be accepted as user input.
   *
   * @return The string value read from the user.
   */
  protected Message promptConfirm(Message prompt,
                                  Message defaultValue,
                                  Message[] validValues) {

    boolean isValid = false;
    Message response = null;
    while (!isValid)
    {

      Message msg;

      if (defaultValue == null)
      {

        msg = Message.raw(PROMPT_NO_DEFAULT_FORMAT, prompt, defaultValue);
      }
      else
      {
        msg = Message.raw(PROMPT_DEFAULT_FORMAT, prompt, defaultValue);
      }

      out.print(msg);
      out.flush();

      response = Message.raw(readLine());
      if (response.toString().equals(""))
      {
        response = defaultValue;
      }
      for (int i=0; i<validValues.length && !isValid; i++)
      {
        isValid = validValues[i].toString().
                equalsIgnoreCase(response.toString());
      }
    }
    return response;
  }

  /**
   * Interactively prompts (on standard output) the user to provide select
   * one option from a set of options.
   *
   * @param  prompt        The prompt to present to the user.
   * @param  defaultOption The default value returned if the user clicks enter.
   * @param  options       The valid values that can be accepted as user input.
   *
   * @return index of options that was chosen or -1 if none where chosen
   */
  public int promptOptions(Message prompt,
                           Message defaultOption,
                           Message[] options) {
    Message choiceDefault = null;
    List<Message> choiceList = new ArrayList<Message>();
    MessageBuilder mb = new MessageBuilder(prompt);
    for (int i = 0; i < options.length; i++) {
      Message choice = Message.raw(Integer.toString(i + 1));
      choiceList.add(choice);
      if (options[i].equals(defaultOption)) {
        choiceDefault = choice;
      }
      mb.append("\n");
      mb.append(choice);
      mb.append(". ");
      mb.append(options[i]);
    }
    int ret = -1;
    Message resp = promptConfirm(mb.toMessage(), choiceDefault,
            choiceList.toArray(new Message[0]));
    if (resp != null) {
      for (int i = 0; i < choiceList.size(); i++) {
        if (resp.equals(choiceList.get(i))) {
          ret = i;
          break;
        }
      }
    }
    return ret;
  }

  /**
   * Interactively prompts (on standard output) the user to provide a string
   * value.  Any non-empty string will be allowed (the empty string will
   * indicate that the default should be used, if there is one).
   *
   * @param  prompt        The prompt to present to the user.
   * @param  defaultValue  The default value to assume if the user presses ENTER
   *                       without typing anything, or <CODE>null</CODE> if
   *                       there should not be a default and the user must
   *                       explicitly provide a value.
   *
   * @return  The string value read from the user.
   */
  public String promptForString(Message prompt, String defaultValue) {
    return promptForString(prompt, defaultValue, true);
  }

  /**
   * Interactively prompts (on standard output) the user to provide a string
   * value.  Any non-empty string will be allowed (the empty string will
   * indicate that the default should be used, if there is one).
   *
   * @param  prompt        The prompt to present to the user.
   * @param  defaultValue  The default value to assume if the user presses ENTER
   *                       without typing anything, or <CODE>null</CODE> if
   *                       there should not be a default and the user must
   *                       explicitly provide a value.
   * @param addLineBreakIfDefault adds a line break between the prompt and the
   *                       default value if this is not <CODE>null</CODE>.
   * @return  The string value read from the user.
   */
  protected String promptForString(Message prompt, String defaultValue,
      boolean addLineBreakIfDefault) {
    String wrappedPrompt = StaticUtils.wrapText(prompt,
        Utils.getCommandLineMaxLineWidth());

    while (true) {
      if (defaultValue == null) {
        out.print(wrappedPrompt);
        out.print(" ");
      } else {
        if (addLineBreakIfDefault)
        {
          out.println(wrappedPrompt);
          out.print("[");
          out.print(defaultValue);
          out.print("]: ");
        }
        else
        {
          out.print(wrappedPrompt);
          out.print(" [");
          out.print(defaultValue);
          out.print("]: ");
        }
      }
      out.flush();

      String response = readLine();
      if (response.equals("")) {
        if (defaultValue == null) {
          Message message = INFO_ERROR_EMPTY_RESPONSE.get();
          err.println(StaticUtils.wrapText(message,
                  Utils.getCommandLineMaxLineWidth()));
        } else {
          return defaultValue;
        }
      } else {
        return response;
      }
    }
  }

  /**
   * Interactively prompts (on standard output) the user to provide a password
   * value.
   *
   * @param  msg        The prompt to present to the user.
   *
   * @return  The string value read from the user.
   */
  protected String promptForPassword(Message msg)
  {
    String pwd;
    String wrappedPrompt = StaticUtils.wrapText(msg,
        Utils.getCommandLineMaxLineWidth());
    out.print(wrappedPrompt+" ");
    out.flush();
    try
    {
      char[] pwChars = PasswordReader.readPassword();
      if ((pwChars == null) || pwChars.length == 0)
      {
        pwd = null;
      }
      else
      {
        pwd = new String(pwChars);
      }
      out.flush();
    }
    catch (Throwable t)
    {
      LOG.log(Level.WARNING, "Error reading password: "+t, t);
      pwd = null;
    }
    return pwd;
  }

  /**
   * Prompts the user to provide a port.
   * @param msg the message to be displayed.
   * @param defaultValue the default value to be proposed.
   * @return the user to provide a port.
   */
  protected int promptForPort(Message msg, int defaultValue)
  {
    return promptForPort(msg, defaultValue, true);
  }

  /**
   * Prompts the user to provide a port.
   * @param msg the message to be displayed.
   * @param defaultValue the default value to be proposed.
   * @param addLineBreakIfDefault adds a line break between the prompt and the
   *                       default value if this is not <CODE>null</CODE>.
   * @return the user to provide a port.
   */
  protected int promptForPort(Message msg, int defaultValue,
      boolean addLineBreakIfDefault)
  {
    int port = -1;
    while (port == -1)
    {
      String s = promptForString(msg, String.valueOf(defaultValue),
          addLineBreakIfDefault);
      if ((s != null) && (s.trim().length() > 0))
      {
        try
        {
          port = Integer.parseInt(s);
          if ((port < 0) || (port > 65535))
          {
            port = -1;
          }
        }
        catch (Throwable t)
        {
          port = -1;
        }
        if (port == -1)
        {
          Message message = INFO_CLI_INVALID_PORT.get();
          err.println(StaticUtils.wrapText(message,
                  Utils.getCommandLineMaxLineWidth()));
        }
      }
    }
    return port;
  }

  /**
   * Reads a line of text from standard input.
   * @return  The line of text read from standard input, or <CODE>null</CODE>
   *          if the end of the stream is reached or an error occurs while
   *          attempting to read the response.
   */
  public String readLine() {
    return readLine(in, err);
  }

  /**
   * Reads a line of text from standard input.
   * @param   in InputSteam from which line will be read
   * @param   err PrintSteam where any errors will be printed
   * @return  The line of text read from standard input, or <CODE>null</CODE>
   *          if the end of the stream is reached or an error occurs while
   *          attempting to read the response.
   */
  public String readLine(InputStream in, PrintStream err)
  {
    try
    {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      while (true)
      {
        int b = in.read();
        if ((b < 0) || (b == '\n'))
        {
          break;
        }
        else if (b == '\r')
        {
          int b2 = in.read();
          if (b2 == '\n')
          {
            break;
          }
          else
          {
            baos.write(b);
            baos.write(b2);
          }
        }
        else
        {
          baos.write(b);
        }
      }

      return new String(baos.toByteArray(), "UTF-8");
    }
    catch (Exception e)
    {
      err.println(INFO_CLI_ERROR_READING_STDIN.get().toString());
      return null;
    }
  }

  /**
   * Returns <CODE>true</CODE> if this is a quiet uninstall and
   * <CODE>false</CODE> otherwise.
   * @param args the arguments passed in the command line.
   * @return <CODE>true</CODE> if this is a quiet uninstall and
   * <CODE>false</CODE> otherwise.
   */
  protected boolean isQuiet(String[] args)
  {
    boolean isQuiet = false;
    for (int i=0; i<args.length && !isQuiet; i++)
    {
      if (args[i].equalsIgnoreCase("--quiet") ||
          args[i].equalsIgnoreCase("-Q"))
      {
        isQuiet = true;
      }
    }
    return isQuiet;
  }

  /**
   * Commodity method used to validate the arguments provided by the user in
   * the command line and updating the UserData object accordingly.
   * @param userData the UserData object to be updated.
   * @param args the arguments passed in the command line.
   * @param validArgs arguments that are acceptable by this application.
   * @throws org.opends.quicksetup.UserDataException if there is an error with
   * the data provided by the user.
   */
  protected void validateArguments(UserData userData,
                                 String[] args,
                                 Set<String> validArgs) throws UserDataException
  {
    ArrayList<Message> errors = new ArrayList<Message>();

    for (String arg1 : args) {
      if (validArgs.contains(arg1)) {
        // Ignore
      } else {
        errors.add(INFO_CLI_UNKNOWN_ARGUMENT.get(arg1));
      }
    }

    if (errors.size() > 0)
    {
      MessageBuilder mb = new MessageBuilder();
      for (Message error : errors) {
        mb.append(error);
        mb.append(Constants.LINE_SEPARATOR);
        mb.append(Constants.LINE_SEPARATOR);
      }
      throw new UserDataException(null, mb.toMessage());
    }
  }

  /**
   * Returns <CODE>true</CODE> if this is a quiet session and
   * <CODE>false</CODE> otherwise.  This method relies on the a previous
   * call to createArgumentParser having been made and the parser
   * having been used to parse the arguments.
   * @return <CODE>true</CODE> if this is a quiet uninstall and
   * <CODE>false</CODE> otherwise.
   */
  protected boolean isQuiet() {
    return quietArg != null && quietArg.isPresent();
  }

  /**
   * Returns <CODE>true</CODE> if this is a noninteractive sessions and
   * <CODE>false</CODE> otherwise.  This method relies on the a previous
   * call to createArgumentParser having been made and the parser
   * having been used to parse the arguments.
   * @return <CODE>true</CODE> if this is a noninteractive session and
   * <CODE>false</CODE> otherwise.
   */
  protected boolean isInteractive() {
    return noPromptArg == null || !noPromptArg.isPresent();
  }

  /**
   * Creates an argument parser having common arguments.
   * @param mainClass class of the tool
   * @param description localized description of the tool
   * @param caseSensitive whether long args are case sensitive
   * @return ArgumentParser ready for app specific customization
   * @see org.opends.server.util.args.ArgumentParser#ArgumentParser(String,
   *      Message,boolean)
   */
  protected ArgumentParser createArgumentParser(String mainClass,
                                                Message description,
                                                boolean caseSensitive) {

    // TODO: get rid of this method and user launcher.getArgumentParser

    // Create the command-line argument parser for use with this program.
    ArgumentParser argParser =
         new ArgumentParser(mainClass, description, caseSensitive);

    // Initialize all the common command-line argument types and register
    // them with the parser.
    try {
      noPromptArg = new BooleanArgument(
          OPTION_LONG_NO_PROMPT,
          OPTION_SHORT_NO_PROMPT,
          OPTION_LONG_NO_PROMPT,
          INFO_DESCRIPTION_NO_PROMPT.get());
      argParser.addArgument(noPromptArg);

      quietArg =
        new BooleanArgument(
            OPTION_LONG_QUIET,
            OPTION_SHORT_QUIET,
            OPTION_LONG_QUIET,
            INFO_DESCRIPTION_QUIET.get());
      argParser.addArgument(quietArg);

    } catch (ArgumentException e) {
      LOG.log(Level.INFO, "error", e);
    }

    return argParser;
  }

  /**
   * Displays an error message in the error output (wrapping it if necessary).
   * @param msg the error message to be displayed.
   */
  protected void printErrorMessage(Message msg)
  {
    err.println(org.opends.server.util.StaticUtils.wrapText(msg,
        Utils.getCommandLineMaxLineWidth()));
    LOG.log(Level.SEVERE, msg.toString());
  }

  /**
   * Displays a progress message in the error output (wrapping it if necessary).
   * @param msg the error message to be displayed.
   */
  protected void printProgressMessage(Message msg)
  {
    if (!isQuiet())
    {
      out.print(org.opends.server.util.StaticUtils.wrapText(msg,
          Utils.getCommandLineMaxLineWidth()));
      out.flush();
    }
    LOG.log(Level.INFO, msg.toString());
  }

  /**
   * Displays a progress message in the error output (wrapping it if necessary).
   * @param msg the error message to be displayed.
   */
  protected void printProgressMessage(String msg)
  {
    if (!isQuiet())
    {
      out.print(org.opends.server.util.StaticUtils.wrapText(msg,
          Utils.getCommandLineMaxLineWidth()));
      out.flush();
    }
    LOG.log(Level.INFO, msg);
  }

  /**
   * Displays a progress message in the error output.
   * @param msg the error message to be displayed.
   */
  protected void printProgressMessageNoWrap(String msg)
  {
    if (!isQuiet())
    {
      out.print(msg);
      out.flush();
    }
    LOG.log(Level.INFO, msg);
  }

  /**
   * Displays a progress message in the error output.
   * @param msg the error message to be displayed.
   */
  protected void printProgressMessageNoWrap(Message msg)
  {
    if (!isQuiet())
    {
      out.print(msg);
      out.flush();
    }
    LOG.log(Level.INFO, msg.toString());
  }

  /**
   * Prints a line break in the standard output if we are not in quite mode.
   */
  protected void printProgressLineBreak()
  {
    if (!isQuiet())
    {
      out.println();
    }
  }

  /**
   * Displays a warning message in the error output (wrapping it if necessary).
   * @param msg the warning message to be displayed.
   */
  protected void printWarningMessage(Message msg)
  {
    if (!isQuiet())
    {
      // TODO: decide if even in quiet mode we must display this message or not.
      out.println(org.opends.server.util.StaticUtils.wrapText(msg,
          Utils.getCommandLineMaxLineWidth()));
      out.flush();
    }
    LOG.log(Level.WARNING, msg.toString());
  }

  /**
   * Displays an error message in the error output (wrapping it if necessary).
   * @param msg the error message to be displayed.
   */
  protected void printErrorMessage(String msg)
  {
    err.println(org.opends.server.util.StaticUtils.wrapText(msg,
        Utils.getCommandLineMaxLineWidth()));
  }

  /**
   * Prints a line break in the standard output.
   */
  protected void printLineBreak()
  {
    out.println();
  }

  /**
   * Prints a line message in the standard output.
   * @param msg the error message to be displayed.
   * @param overrideQuietMode whether to override the quiet mode or not.
   */
  protected void printLine(Message msg, boolean overrideQuietMode)
  {
    if (!isQuiet() || overrideQuietMode)
    {
      out.println(org.opends.server.util.StaticUtils.wrapText(msg,
          Utils.getCommandLineMaxLineWidth()));
    }
  }

  /**
   * Prompts the user to give the Global Administrator UID.
   * @param defaultValue the default value that will be proposed in the prompt
   * message.
   * @return the Global Administrator UID as provided by the user.
   */
  protected String askForAdministratorUID(String defaultValue)
  {
    return promptForString(INFO_ADMINISTRATOR_UID_PROMPT.get(), defaultValue,
        false);
  }

  /**
   * Prompts the user to give the Global Administrator password.
   * @return the Global Administrator password as provided by the user.
   */
  protected String askForAdministratorPwd()
  {
    return promptForPassword(INFO_ADMINISTRATOR_PWD_PROMPT.get());
  }

  /**
   * Prompts the user to confirm a question.  The default proposed value as
   * answer is to confirm the question.
   * @param msg the message to be displayed to the user.
   * @return <CODE>true</CODE> if the user accepted the message and
   * <CODE>false</CODE> otherwise.
   */
  protected boolean confirm(Message msg)
  {
    return confirm(msg, true);
  }

  /**
   * Prompts the user to confirm a question.
   * @param msg the message to be displayed to the user.
   * @param defaultTrue whether the default proposed value as answer is to
   * accept the message or not.
   * @return <CODE>true</CODE> if the user accepted the message and
   * <CODE>false</CODE> otherwise.
   */
  protected boolean confirm(Message msg, boolean defaultTrue) {
    boolean confirm = true;
    Message[] validValues = {
        INFO_CLI_YES_SHORT.get(),
        INFO_CLI_NO_SHORT.get(),
        INFO_CLI_YES_LONG.get(),
        INFO_CLI_NO_LONG.get(),
    };
    Message defaultMessage;
    if (defaultTrue)
    {
      defaultMessage = validValues[2];
    }
    else
    {
      defaultMessage = validValues[3];
    }
    Message answer = promptConfirm(msg, defaultMessage, validValues);
    if (INFO_CLI_NO_SHORT.get().toString()
            .equalsIgnoreCase(answer.toString()) ||
        INFO_CLI_NO_LONG.get().toString()
                .equalsIgnoreCase(answer.toString()))
    {
      confirm = false;
    }
    return confirm;
  }

  /**
   * Returns an InitialLdapContext using the provided parameters.  We try
   * to guarantee that the connection is able to read the configuration.
   * @param host the host name.
   * @param port the port to connect.
   * @param useSSL whether to use SSL or not.
   * @param useStartTLS whether to use StartTLS or not.
   * @param bindDn the bind dn to be used.
   * @param pwd the password.
   * @param trustManager the trust manager.
   * @return an InitialLdapContext connected.
   * @throws NamingException if there was an error establishing the connection.
   */
  protected InitialLdapContext createContext(String host, int port,
      boolean useSSL, boolean useStartTLS, String bindDn, String pwd,
      ApplicationTrustManager trustManager)
  throws NamingException
  {
    InitialLdapContext ctx;
    String ldapUrl = ConnectionUtils.getLDAPUrl(host, port, useSSL);
    if (useSSL)
    {
      ctx = Utils.createLdapsContext(ldapUrl, bindDn, pwd,
          Utils.getDefaultLDAPTimeout(), null, trustManager);
    }
    else if (useStartTLS)
    {
      ctx = Utils.createStartTLSContext(ldapUrl, bindDn, pwd,
          Utils.getDefaultLDAPTimeout(), null, trustManager,
          null);
    }
    else
    {
      ctx = Utils.createLdapContext(ldapUrl, bindDn, pwd,
          Utils.getDefaultLDAPTimeout(), null);
    }
    if (!ConnectionUtils.connectedAsAdministrativeUser(ctx))
    {
      throw new NoPermissionException(
          ERR_NOT_ADMINISTRATIVE_USER.get().toString());
    }
    return ctx;
  }

  /**
   * Prompts the user to accept the certificate.
   * @param t the throwable that was generated because the certificate was
   * not trusted.
   * @param usedTrustManager the trustManager used when trying to establish the
   * connection.
   * @param usedUrl the LDAP URL used to connect to the server.
   * @param trustManager the global trustManager that contains the certificates
   * accepted by the user and that will be updated.
   * @return <CODE>true</CODE> if the user accepted the certificate and
   * <CODE>false</CODE> otherwise.
   */
  protected boolean promptForCertificateConfirmation(Throwable t,
      ApplicationTrustManager usedTrustManager, String usedUrl,
      ApplicationTrustManager trustManager)
  {
    boolean returnValue = false;
    ApplicationTrustManager.Cause cause;
    if (usedTrustManager != null)
    {
      cause = usedTrustManager.getLastRefusedCause();
    }
    else
    {
      cause = null;
    }

    LOG.log(Level.INFO, "Certificate exception cause: "+cause);
    UserDataCertificateException.Type excType = null;
    if (cause == ApplicationTrustManager.Cause.NOT_TRUSTED)
    {
      excType = UserDataCertificateException.Type.NOT_TRUSTED;
    }
    else if (cause ==
      ApplicationTrustManager.Cause.HOST_NAME_MISMATCH)
    {
      excType = UserDataCertificateException.Type.HOST_NAME_MISMATCH;
    }
    else
    {
      Message msg = Utils.getThrowableMsg(INFO_ERROR_CONNECTING_TO_LOCAL.get(),
          t);
      printErrorMessage(msg);
    }

    if (excType != null)
    {
      String h;
      int p;
      try
      {
        URI uri = new URI(usedUrl);
        h = uri.getHost();
        p = uri.getPort();
      }
      catch (Throwable t1)
      {
        LOG.log(Level.WARNING, "Error parsing ldap url of ldap url.", t1);
        h = INFO_NOT_AVAILABLE_LABEL.get().toString();
        p = -1;
      }
      UserDataCertificateException udce =
        new UserDataCertificateException(Step.REPLICATION_OPTIONS,
            INFO_CERTIFICATE_EXCEPTION.get(h, String.valueOf(p)), t, h, p,
                usedTrustManager.getLastRefusedChain(),
                usedTrustManager.getLastRefusedAuthType(), excType);

      returnValue = handleCertificateException(udce, trustManager, true);
    }
    return returnValue;
  }

  /**
   * Returns the ConnectionPolicy to be used with the parameters provided
   * by the user.
   * @param useSSL whether the user asked to use SSL or not.
   * @param useStartTLS whether the user asked to use Start TLS or not.
   * @return the ConnectionPolicy to be used with the parameters provided
   * by the user.
   */
  protected ConnectionProtocolPolicy getConnectionPolicy(boolean useSSL,
      boolean useStartTLS)
  {
    ConnectionProtocolPolicy policy;
    if (useStartTLS)
    {
      policy = ConnectionProtocolPolicy.USE_STARTTLS;
    }
    else if (useSSL)
    {
      policy = ConnectionProtocolPolicy.USE_LDAPS;
    }
    else
    {
      policy = ConnectionProtocolPolicy.USE_LESS_SECURE_AVAILABLE;
    }
    return policy;
  }

  /**
   * Prompts the user to accept the certificate that generated the provided
   * UserDataCertificateException.
   * @param trustManager the global trustManager that contains the certificates
   * accepted by the user.
   * @param udce the UserDataCertificateException that was generated.
   * @param trustManager the global trustManager that contains the certificates
   * accepted by the user and that will be updated if the user accepts the
   * certificate.
   * @param displayErrorMessage whether to display the message describing
   * the error encountered (certificate not trusted) or only prompt to accept
   * the certificate.
   * @return <CODE>true</CODE> if the user accepted the certificate and
   * <CODE>false</CODE> otherwise.
   */
  private boolean handleCertificateException(
      UserDataCertificateException udce, ApplicationTrustManager trustManager,
      boolean displayErrorMessage)
  {
    boolean accepted = false;
    Message msg;
    if (udce.getType() == UserDataCertificateException.Type.NOT_TRUSTED)
    {
      msg = INFO_CERTIFICATE_NOT_TRUSTED_TEXT_CLI.get(
          udce.getHost(), String.valueOf(udce.getPort()),
          udce.getHost(), String.valueOf(udce.getPort()));
    }
    else
    {
      msg = INFO_CERTIFICATE_NAME_MISMATCH_TEXT_CLI.get(
          udce.getHost(), String.valueOf(udce.getPort()),
          udce.getHost(),
          udce.getHost(), String.valueOf(udce.getPort()),
          udce.getHost(), String.valueOf(udce.getPort()));
    }
    if (displayErrorMessage)
    {
      printErrorMessage(msg);
    }
    Message[] validValues = {
        INFO_CLI_ACCEPT_CERTIFICATE_LONG.get(),
        INFO_CLI_REJECT_CERTIFICATE_LONG.get(),
        INFO_CLI_VIEW_CERTIFICATE_LONG.get(),
        INFO_CLI_ACCEPT_CERTIFICATE_SHORT.get(),
        INFO_CLI_REJECT_CERTIFICATE_SHORT.get(),
        INFO_CLI_VIEW_CERTIFICATE_SHORT.get()
    };
    Message answer = promptConfirm(INFO_CLI_ACCEPT_CERTIFICATE_PROMPT.get(),
        validValues[0], validValues);

    if (INFO_CLI_REJECT_CERTIFICATE_LONG.get().toString().equalsIgnoreCase(
        answer.toString()) ||
        INFO_CLI_REJECT_CERTIFICATE_SHORT.get().toString().equalsIgnoreCase(
            answer.toString()))
    {
      accepted = false;
    }
    else if (INFO_CLI_VIEW_CERTIFICATE_LONG.get().toString().equalsIgnoreCase(
        answer.toString()) ||
        INFO_CLI_VIEW_CERTIFICATE_SHORT.get().toString().equalsIgnoreCase(
            answer.toString()))
    {
      printLineBreak();
      displayCertificate(udce);
      accepted = handleCertificateException(udce, trustManager, false);
    }
    else
    {
      X509Certificate[] chain = udce.getChain();
      String authType = udce.getAuthType();
      String host = udce.getHost();

      if ((chain != null) && (authType != null) && (host != null))
      {
        LOG.log(Level.INFO, "Accepting certificate presented by host "+host);
        if (trustManager != null)
        {
          trustManager.acceptCertificate(chain, authType, host);
        }
        accepted = true;
      }
      else
      {
        if (chain == null)
        {
          LOG.log(Level.WARNING,
          "The chain is null for the UserDataCertificateException");
        }
        if (authType == null)
        {
          LOG.log(Level.WARNING,
          "The auth type is null for the UserDataCertificateException");
        }
        if (host == null)
        {
          LOG.log(Level.WARNING,
          "The host is null for the UserDataCertificateException");
        }
      }
    }
    return accepted;
  }

  private void displayCertificate(UserDataCertificateException udce)
  {
    Message[] labels =
    {
        INFO_CERTIFICATE_SUBJECT_LABEL.get(),
        INFO_CERTIFICATE_ISSUED_BY_LABEL.get(),
        INFO_CERTIFICATE_VALID_FROM_LABEL.get(),
        INFO_CERTIFICATE_EXPIRES_ON_LABEL.get(),
        INFO_CERTIFICATE_TYPE_LABEL.get(),
        INFO_CERTIFICATE_SERIAL_NUMBER_LABEL.get(),
        INFO_CERTIFICATE_MD5_FINGERPRINT_LABEL.get(),
        INFO_CERTIFICATE_SHA1_FINGERPRINT_LABEL.get()
    };
    for (int i=0; i<udce.getChain().length; i++)
    {
      X509Certificate cert = udce.getChain()[i];
      String[] values =
      {
          cert.getSubjectX500Principal().getName().toString(),
          cert.getIssuerX500Principal().getName().toString(),
          CertificateDialog.getValidFrom(cert),
          CertificateDialog.getExpiresOn(cert),
          cert.getType(),
          String.valueOf(cert.getSerialNumber()),
          CertificateDialog.getMD5FingerPrint(cert).toString(),
          CertificateDialog.getSHA1FingerPrint(cert).toString()
      };
      for (int j=0; j<labels.length; j++)
      {
        out.println(StaticUtils.wrapText(labels[j]+" "+values[j],
            Utils.getCommandLineMaxLineWidth()));
      }
    }
    out.flush();
  }
}
