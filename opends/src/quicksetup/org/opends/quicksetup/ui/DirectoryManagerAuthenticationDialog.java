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
 * by brackets "[]" replaced with your own identifying * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Portions Copyright 2006 Sun Microsystems, Inc.
 */

package org.opends.quicksetup.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.InitialLdapContext;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import org.opends.quicksetup.CurrentInstallStatus;
import org.opends.quicksetup.event.MinimumSizeComponentListener;
import org.opends.quicksetup.i18n.ResourceProvider;
import org.opends.quicksetup.util.BackgroundTask;
import org.opends.quicksetup.util.Utils;

/**
 * This class is a dialog that appears when the user must provide authentication
 * of Directory Manager to connect to the Directory Server in order to be
 * able to do a shutdown. This is currently required for Windows.
 */
public class DirectoryManagerAuthenticationDialog extends JDialog
{
  private static final long serialVersionUID = 9049409381101152000L;

  private JFrame parent;

  private CurrentInstallStatus installStatus;

  private JLabel lDn;
  private JLabel lPwd;

  private JTextField tfDn;
  private JTextField tfPwd;

  private JButton cancelButton;
  private JButton shutDownButton;

  private boolean isCancelled;

  private boolean isServerRunning;

  /**
   * Constructor of the DirectoryManagerAuthenticationDialog.
   * @param parent the parent frame for this dialog.
   * @param installStatus the object describing the current installation
   * status.
   */
  public DirectoryManagerAuthenticationDialog(JFrame parent,
      CurrentInstallStatus installStatus)
  {
    super(parent);
    setTitle(getMsg("shutdown-directory-manager-dialog-title"));
    this.parent = parent;
    this.installStatus = installStatus;
    getContentPane().add(createPanel());
//  TODO: find a way to calculate this dynamically
    setPreferredSize(new Dimension(500, 300));
    addComponentListener(new MinimumSizeComponentListener(this,
        500, 300));
  }

  /**
   * Returns <CODE>true</CODE> if the user clicked on cancel and
   * <CODE>false</CODE> otherwise.
   * @return <CODE>true</CODE> if the user clicked on cancel and
   * <CODE>false</CODE> otherwise.
   */
  public boolean isCancelled()
  {
    return isCancelled;
  }

  /**
   * Packs and displays this dialog.
   *
   */
  public void packAndShow()
  {
    pack();
    Utils.centerOnComponent(this, parent);
    tfPwd.requestFocusInWindow();
    getRootPane().setDefaultButton(shutDownButton);
    setVisible(true);
  }

  /**
   * Tells whether we must stop the server or not.
   * @return <CODE>true</CODE> if we must stop the server and <CODE>false</CODE>
   * otherwise
   */
  public boolean getStopServer()
  {
    return isServerRunning;
  }

  /**
   * Returns the Directory Manager DN provided by the user.
   * @return the Directory Manager DN provided by the user.
   */
  public String getDirectoryManagerDn()
  {
    return tfDn.getText();
  }

  /**
   * Returns the Directory Manager password provided by the user.
   * @return the Directory Manager password provided by the user.
   */
  public String getDirectoryManagerPwd()
  {
    return tfPwd.getText();
  }

  /* The following three methods are just commodity methods to retrieve
   * localized messages */
  private String getMsg(String key)
  {
    return getI18n().getMsg(key);
  }

  private String getMsg(String key, String[] args)
  {
    return getI18n().getMsg(key, args);
  }

  private ResourceProvider getI18n()
  {
    return ResourceProvider.getInstance();
  }

  /**
   * Creates and returns the panel of the dialog.
   * @return the panel of the dialog.
   */
  private JPanel createPanel()
  {
    JPanel p1 = new JPanel(new GridBagLayout());
    p1.setBackground(UIFactory.CURRENT_STEP_PANEL_BACKGROUND);
    p1.setBorder(UIFactory.DIALOG_PANEL_BORDER);
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridwidth = GridBagConstraints.RELATIVE;
    gbc.anchor = GridBagConstraints.NORTHWEST;
    gbc.insets = UIFactory.getCurrentStepPanelInsets();
    p1.add(UIFactory.makeJLabel(UIFactory.IconType.WARNING_LARGE, null,
        UIFactory.TextStyle.NO_STYLE), gbc);
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.insets.left = 0;
    String msg = getMsg("shutdown-directory-manager-dialog-msg");
    JTextComponent textPane =
      UIFactory.makeHtmlPane(msg, UIFactory.INSTRUCTIONS_FONT);
    textPane.setOpaque(false);
    p1.add(textPane, gbc);

    JPanel p2 = new JPanel(new GridBagLayout());
    p2.setOpaque(false);
    gbc.gridwidth = GridBagConstraints.RELATIVE;
    gbc.weightx = 0.0;
    gbc.insets.top = UIFactory.TOP_INSET_PRIMARY_FIELD;
    gbc.insets.left = 0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    lDn = UIFactory.makeJLabel(UIFactory.IconType.NO_ICON,
        getMsg("shutdown-directory-manager-dn-label"),
        UIFactory.TextStyle.PRIMARY_FIELD_VALID);
    p2.add(lDn, gbc);
    gbc.weightx = 1.0;
    gbc.insets.left = UIFactory.LEFT_INSET_PRIMARY_FIELD;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    tfDn = UIFactory.makeJTextField(getProposedDirectoryManagerDn(),
        getMsg("shutdown-directory-manager-dn-tooltip"),
        UIFactory.DN_FIELD_SIZE, UIFactory.TextStyle.TEXTFIELD);
    p2.add(tfDn, gbc);

    gbc.insets.top = 0;
    gbc.gridwidth = GridBagConstraints.RELATIVE;
    gbc.weightx = 0.0;
    gbc.insets.left = 0;
    lPwd = UIFactory.makeJLabel(UIFactory.IconType.NO_ICON,
        getMsg("shutdown-directory-manager-pwd-label"),
        UIFactory.TextStyle.PRIMARY_FIELD_VALID);
    p2.add(lPwd, gbc);
    gbc.weightx = 1.0;
    gbc.insets.left = UIFactory.LEFT_INSET_PRIMARY_FIELD;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    tfPwd = UIFactory.makeJPasswordField(null,
        getMsg("shutdown-directory-manager-pwd-tooltip"),
        UIFactory.PASSWORD_FIELD_SIZE, UIFactory.TextStyle.PASSWORD_FIELD);
    p2.add(tfPwd, gbc);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = UIFactory.getEmptyInsets();
    gbc.gridwidth = GridBagConstraints.RELATIVE;
    gbc.weightx = 0.0;
    gbc.insets.top = 0;
    p1.add(Box.createHorizontalGlue(), gbc);
    gbc.weightx = 1.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    p1.add(p2, gbc);
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.VERTICAL;
    p1.add(Box.createVerticalGlue(), gbc);

    JPanel buttonPanel = new JPanel(new GridBagLayout());
    buttonPanel.setOpaque(false);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.insets = UIFactory.getEmptyInsets();
    gbc.gridwidth = 3;
    buttonPanel.add(Box.createHorizontalGlue(), gbc);
    gbc.gridwidth = GridBagConstraints.RELATIVE;
    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    shutDownButton =
      UIFactory.makeJButton(getMsg("shutdown-button-label"),
          getMsg("shutdown-directory-manager-shutdown-button-tooltip"));
    buttonPanel.add(shutDownButton, gbc);
    shutDownButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent ev)
      {
        shutDownClicked();
      }
    });

    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.insets.left = UIFactory.HORIZONTAL_INSET_BETWEEN_BUTTONS;
    cancelButton =
      UIFactory.makeJButton(getMsg("cancel-button-label"),
          getMsg("shutdown-directory-manager-cancel-button-tooltip"));
    buttonPanel.add(cancelButton, gbc);
    cancelButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent ev)
      {
        cancelClicked();
      }
    });

    JPanel p = new JPanel(new GridBagLayout());
    p.setBackground(UIFactory.DEFAULT_BACKGROUND);
    gbc.insets = UIFactory.getEmptyInsets();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    p.add(p1, gbc);
    gbc.weighty = 0.0;
    gbc.insets = UIFactory.getButtonsPanelInsets();
    p.add(buttonPanel, gbc);

    return p;
  }

  private String getProposedDirectoryManagerDn()
  {
    String dn;
    Set<String> dns = installStatus.getDirectoryManagerDns();
    if (dns.size() > 0)
    {
      dn = dns.iterator().next();
    }
    else
    {
      dn = null;
    }
    return dn;
  }

  private void cancelClicked()
  {
    isCancelled = true;
    dispose();
  }

  private void shutDownClicked()
  {
    isCancelled = false;
    BackgroundTask worker = new BackgroundTask()
    {
      public Object processBackgroundTask() throws NamingException
      {
        Boolean isServerRunning = Boolean.TRUE;
        try
        {
          InitialLdapContext ctx =
            Utils.createLdapContext(installStatus.getLdapUrl(), tfDn.getText(),
              tfPwd.getText(), 3000, null);

          /*
           * Search for the config to check that it is the directory manager.
           */
          SearchControls searchControls = new SearchControls();
          searchControls.setCountLimit(1);
          searchControls.setSearchScope(
          SearchControls. OBJECT_SCOPE);
          searchControls.setReturningAttributes(
          new String[] {"dn"});
          ctx.search("cn=config", "objectclass=*", searchControls);

        } catch (NamingException ne)
        {
          if (installStatus.isServerRunning())
          {
            throw ne;
          }
          isServerRunning = Boolean.FALSE;
        } catch (Throwable t)
        {
          throw new IllegalStateException("Unexpected throwable.", t);
        }
        return isServerRunning;
      }

      public void backgroundTaskCompleted(Object returnValue,
          Throwable throwable)
      {
        if (throwable != null)
        {
          if (throwable instanceof NamingException)
          {
            boolean dnInvalid = false;
            boolean pwdInvalid = false;

            String dn = tfDn.getText();
            ArrayList<String> possibleCauses = new ArrayList<String>();
            if ("".equals(dn.trim()))
            {
              dnInvalid = true;
              possibleCauses.add(getMsg("empty-directory-manager-dn"));
            }
            else if (!Utils.isDn(dn))
            {
              dnInvalid = true;
              possibleCauses.add(getMsg("not-a-directory-manager-dn"));
            }
            else
            {
              boolean found = false;
              Iterator<String> it =
                installStatus.getDirectoryManagerDns().iterator();
              while (it.hasNext() && !found)
              {
                found = Utils.areDnsEqual(dn, it.next());
              }
              if (!found)
              {
                dnInvalid = true;
                possibleCauses.add(getMsg("not-a-directory-manager-in-config"));
              }
            }

            if ("".equals(tfPwd.getText()))
            {
              pwdInvalid = true;
              possibleCauses.add(getMsg("empty-pwd"));
            }
            if (possibleCauses.size() > 0)
            {
              // Message with causes
              String[] arg = {
                  Utils.getStringFromCollection(possibleCauses, "\n")
              };
              displayError(
                  getMsg("cannot-connect-to-shutdown-with-cause", arg),
                  getMsg("error-title"));
            }
            else
            {
              // Generic message
              displayError(
                  getMsg("cannot-connect-to-shutdown-without-cause"),
                  getMsg("error-title"));
            }

            if (dnInvalid)
            {
              UIFactory.setTextStyle(lDn,
                UIFactory.TextStyle.PRIMARY_FIELD_INVALID);
            }
            else
            {
              UIFactory.setTextStyle(lDn,
                  UIFactory.TextStyle.PRIMARY_FIELD_VALID);
              pwdInvalid = true;
            }
            if (pwdInvalid)
            {
              UIFactory.setTextStyle(lPwd,
                UIFactory.TextStyle.PRIMARY_FIELD_INVALID);
            }
            else
            {
              UIFactory.setTextStyle(lPwd,
                  UIFactory.TextStyle.PRIMARY_FIELD_VALID);
            }
          }
          else
          {
            // This is a bug
            throwable.printStackTrace();
            displayError(
                Utils.getThrowableMsg(getI18n(), "bug-msg", null, throwable),
                getMsg("error-title"));
          }
          cancelButton.setEnabled(true);
          shutDownButton.setEnabled(true);
        } else
        {
          isServerRunning = Boolean.TRUE.equals(returnValue);
          if (!isServerRunning)
          {
            displayInformationMessage(getMsg("server-not-running-msg"),
                getMsg("server-not-running-title"));
          }
          isCancelled = false;
          dispose();
        }
      }
    };
    cancelButton.setEnabled(false);
    shutDownButton.setEnabled(false);
    worker.startBackgroundTask();
  }

  /**
   * Displays an error message dialog.
   *
   * @param msg
   *          the error message.
   * @param title
   *          the title for the dialog.
   */
  private void displayError(String msg, String title)
  {
    Utils.displayError(parent, msg, title);
  }

  /**
   * Displays an information message dialog.
   *
   * @param msg
   *          the information message.
   * @param title
   *          the title for the dialog.
   */
  private void displayInformationMessage(String msg, String title)
  {
    Utils.displayInformationMessage(parent, msg, title);
  }

  /**
   * Method written for testing purposes.
   * @param args the arguments to be passed to the test program.
   */
  public static void main(String[] args)
  {
    try
    {
      // UIFactory.initialize();
      DirectoryManagerAuthenticationDialog dlg =
          new DirectoryManagerAuthenticationDialog(new JFrame(),
              new CurrentInstallStatus());
      dlg.packAndShow();
    } catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }
}
