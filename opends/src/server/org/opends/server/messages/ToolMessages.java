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
package org.opends.server.messages;



import org.opends.server.config.ConfigFileHandler;

import static org.opends.server.messages.MessageHandler.*;
import static org.opends.server.tools.ToolConstants.*;
import static org.opends.server.util.DynamicConstants.*;



/**
 * This class defines the set of message IDs and default format strings for
 * messages associated with the tools.
 */
public class ToolMessages
{
  /**
   * The message ID for the message that will be used if an SSL connection
   * could not be created to the server.
   */
  public static final int MSGID_TOOLS_CANNOT_CREATE_SSL_CONNECTION =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 1;



  /**
   * The message ID for the message that will be used if an SSL connection
   * could not be created to the server because the connection factory was
   * not initialized.
   */
  public static final int MSGID_TOOLS_SSL_CONNECTION_NOT_INITIALIZED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 2;



  /**
   * The message ID for the message that will be used if the SSL keystore
   * could not be loaded.
   */
  public static final int MSGID_TOOLS_CANNOT_LOAD_KEYSTORE_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 3;


  /**
   * The message ID for the message that will be used if the key manager
   * could not be initialized.
   */
  public static final int MSGID_TOOLS_CANNOT_INIT_KEYMANAGER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 4;

  /**
   * The message ID for the message that will be used if the SSL trust store
   * could not be loaded.
   */
  public static final int MSGID_TOOLS_CANNOT_LOAD_TRUSTSTORE_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 5;


  /**
   * The message ID for the message that will be used if the trust manager
   * could not be initialized.
   */
  public static final int MSGID_TOOLS_CANNOT_INIT_TRUSTMANAGER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 6;



  /**
   * The message ID for the message that will be used as the description of the
   * listSchemes argument.  This does not take any arguments.
   */
  public static final int MSGID_ENCPW_DESCRIPTION_LISTSCHEMES =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 7;



  /**
   * The message ID for the message that will be used as the description of the
   * clearPassword argument.  This does not take any arguments.
   */
  public static final int MSGID_ENCPW_DESCRIPTION_CLEAR_PW =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 8;



  /**
   * The message ID for the message that will be used as the description of the
   * clearPasswordFile argument.  This does not take any arguments.
   */
  public static final int MSGID_ENCPW_DESCRIPTION_CLEAR_PW_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 9;



  /**
   * The message ID for the message that will be used as the description of the
   * encodedPassword argument.  This does not take any arguments.
   */
  public static final int MSGID_ENCPW_DESCRIPTION_ENCODED_PW =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 10;



  /**
   * The message ID for the message that will be used as the description of the
   * encodedPasswordFile argument.  This does not take any arguments.
   */
  public static final int MSGID_ENCPW_DESCRIPTION_ENCODED_PW_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 11;



  /**
   * The message ID for the message that will be used as the description of the
   * configClass argument.  This does not take any arguments.
   */
  public static final int MSGID_ENCPW_DESCRIPTION_CONFIG_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 12;



  /**
   * The message ID for the message that will be used as the description of the
   * configFile argument.  This does not take any arguments.
   */
  public static final int MSGID_ENCPW_DESCRIPTION_CONFIG_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 13;



  /**
   * The message ID for the message that will be used as the description of the
   * storageScheme argument.  This does not take any arguments.
   */
  public static final int MSGID_ENCPW_DESCRIPTION_SCHEME =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 14;



  /**
   * The message ID for the message that will be used as the description of the
   * help argument.  This does not take any arguments.
   */
  public static final int MSGID_ENCPW_DESCRIPTION_USAGE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 15;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the command-line arguments.  This takes a single argument,
   * which is an explanation of the problem that occurred.
   */
  public static final int MSGID_ENCPW_CANNOT_INITIALIZE_ARGS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 16;



  /**
   * The message ID for the message that will be used if an error occurs while
   * parsing the command-line arguments.  This takes a single argument, which is
   * an explanation of the problem that occurred.
   */
  public static final int MSGID_ENCPW_ERROR_PARSING_ARGS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 17;



  /**
   * The message ID for the message that will be used if no clear-text password
   * was provided in the arguments.  This takes two arguments, which are the
   * long identifier for the clear password argument and the long identifier for
   * the clear password file argument.
   */
  public static final int MSGID_ENCPW_NO_CLEAR_PW =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 18;



  /**
   * The message ID for the message that will be used if no password storage
   * scheme was provided in the arguments.  This takes a single argument, which
   * is the long identifier for the password storage scheme argument.
   */
  public static final int MSGID_ENCPW_NO_SCHEME =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 19;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to bootstrap the Directory Server.  This takes a single argument,
   * which is a string representation of the exception that was caught.
   */
  public static final int MSGID_ENCPW_SERVER_BOOTSTRAP_ERROR =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 20;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying load the Directory Server configuration.  This takes a single
   * argument which is a message with information about the problem that
   * occurred.
   */
  public static final int MSGID_ENCPW_CANNOT_LOAD_CONFIG =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 21;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to load the Directory Server schema.  This takes a single argument,
   * which is a message with information about the problem that occurred.
   */
  public static final int MSGID_ENCPW_CANNOT_LOAD_SCHEMA =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 22;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to initialize the core Directory Server configuration.  This takes a
   * single argument, which is a message with information about the problem that
   * occurred.
   */
  public static final int MSGID_ENCPW_CANNOT_INITIALIZE_CORE_CONFIG =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 23;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to initialize the Directory Server password storage schemes.  This
   * takes a single argument, which is a message with information about the
   * problem that occurred.
   */
  public static final int MSGID_ENCPW_CANNOT_INITIALIZE_STORAGE_SCHEMES =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 24;



  /**
   * The message ID for the message that will be used if no storage schemes have
   * been defined in the Directory Server.  This does not take any arguments.
   */
  public static final int MSGID_ENCPW_NO_STORAGE_SCHEMES =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 25;



  /**
   * The message ID for the message that will be used if the requested storage
   * scheme is not configured for use in the Directory Server.  This takes a
   * single argument, which is the name of the requested storage scheme.
   */
  public static final int MSGID_ENCPW_NO_SUCH_SCHEME =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 26;



  /**
   * The message ID for the message that will be used if the clear-text and
   * encoded passwords match.  This does not take any arguments.
   */
  public static final int MSGID_ENCPW_PASSWORDS_MATCH =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 27;



  /**
   * The message ID for the message that will be used if the clear-text and
   * encoded passwords do not match.  This does not take any arguments.
   */
  public static final int MSGID_ENCPW_PASSWORDS_DO_NOT_MATCH =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 28;



  /**
   * The message ID for the message that will be used to display the encoded
   * password.  This takes a single argument, which is the encoded password.
   */
  public static final int MSGID_ENCPW_ENCODED_PASSWORD =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 29;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to encode the clear-text password.  This takes a single argument,
   * which is an explanation of the problem that occurred.
   */
  public static final int MSGID_ENCPW_CANNOT_ENCODE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 30;



  /**
   * The message ID for the message that will be used as the description of the
   * configClass argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFEXPORT_DESCRIPTION_CONFIG_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 31;



  /**
   * The message ID for the message that will be used as the description of the
   * configFile argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFEXPORT_DESCRIPTION_CONFIG_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 32;



  /**
   * The message ID for the message that will be used as the description of the
   * ldifFile argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFEXPORT_DESCRIPTION_LDIF_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 33;



  /**
   * The message ID for the message that will be used as the description of the
   * appendToLDIF argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFEXPORT_DESCRIPTION_APPEND_TO_LDIF =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 34;



  /**
   * The message ID for the message that will be used as the description of the
   * backendID argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFEXPORT_DESCRIPTION_BACKEND_ID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 35;



  /**
   * The message ID for the message that will be used as the description of the
   * excludeBranch argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFEXPORT_DESCRIPTION_EXCLUDE_BRANCH =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 36;



  /**
   * The message ID for the message that will be used as the description of the
   * includeAttribute argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFEXPORT_DESCRIPTION_INCLUDE_ATTRIBUTE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 37;



  /**
   * The message ID for the message that will be used as the description of the
   * excludeAttribute argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFEXPORT_DESCRIPTION_EXCLUDE_ATTRIBUTE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 38;



  /**
   * The message ID for the message that will be used as the description of the
   * includeFilter argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFEXPORT_DESCRIPTION_INCLUDE_FILTER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 39;



  /**
   * The message ID for the message that will be used as the description of the
   * excludeFilter argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFEXPORT_DESCRIPTION_EXCLUDE_FILTER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 40;



  /**
   * The message ID for the message that will be used as the description of the
   * wrapColumn argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFEXPORT_DESCRIPTION_WRAP_COLUMN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 41;



  /**
   * The message ID for the message that will be used as the description of the
   * compressLDIF argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFEXPORT_DESCRIPTION_COMPRESS_LDIF =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 42;



  /**
   * The message ID for the message that will be used as the description of the
   * encryptLDIF argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFEXPORT_DESCRIPTION_ENCRYPT_LDIF =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 43;



  /**
   * The message ID for the message that will be used as the description of the
   * signHash argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFEXPORT_DESCRIPTION_SIGN_HASH =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 44;



  /**
   * The message ID for the message that will be used as the description of the
   * help argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFEXPORT_DESCRIPTION_USAGE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 45;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the command-line arguments.  This takes a single argument,
   * which is an explanation of the problem that occurred.
   */
  public static final int MSGID_LDIFEXPORT_CANNOT_INITIALIZE_ARGS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 46;



  /**
   * The message ID for the message that will be used if an error occurs while
   * parsing the command-line arguments.  This takes a single argument, which is
   * an explanation of the problem that occurred.
   */
  public static final int MSGID_LDIFEXPORT_ERROR_PARSING_ARGS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 47;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to bootstrap the Directory Server.  This takes a single argument,
   * which is a string representation of the exception that was caught.
   */
  public static final int MSGID_LDIFEXPORT_SERVER_BOOTSTRAP_ERROR =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 48;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying load the Directory Server configuration.  This takes a single
   * argument which is a message with information about the problem that
   * occurred.
   */
  public static final int MSGID_LDIFEXPORT_CANNOT_LOAD_CONFIG =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 49;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to load the Directory Server schema.  This takes a single argument,
   * which is a message with information about the problem that occurred.
   */
  public static final int MSGID_LDIFEXPORT_CANNOT_LOAD_SCHEMA =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 50;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to initialize the core Directory Server configuration.  This takes a
   * single argument, which is a message with information about the problem that
   * occurred.
   */
  public static final int MSGID_LDIFEXPORT_CANNOT_INITIALIZE_CORE_CONFIG =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 51;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to decode an exclude filter.  This takes two arguments, which are
   * the provided filter string and a message explaining the problem that was
   * encountered.
   */
  public static final int MSGID_LDIFEXPORT_CANNOT_PARSE_EXCLUDE_FILTER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 52;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to decode an include filter.  This takes two arguments, which are
   * the provided filter string and a message explaining the problem that was
   * encountered.
   */
  public static final int MSGID_LDIFEXPORT_CANNOT_PARSE_INCLUDE_FILTER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 53;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to decode the base DN string.  This takes two arguments, which are
   * the provided base DN string and a message explaining the problem that
   * was encountered.
   */
  public static final int MSGID_LDIFEXPORT_CANNOT_DECODE_BASE_DN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 54;



  /**
   * The message ID for the message that will be used if multiple backends claim
   * to have the requested backend ID.  This takes a single argument, which is
   * the requested backend ID.
   */
  public static final int MSGID_LDIFEXPORT_MULTIPLE_BACKENDS_FOR_ID=
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 55;



  /**
   * The message ID for the message that will be used if no backends claim to
   * have the requested backend ID.  This takes a single argument, which is the
   * requested backend ID.
   */
  public static final int MSGID_LDIFEXPORT_NO_BACKENDS_FOR_ID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 56;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to decode the exclude branch string.  This takes two arguments,
   * which are the provided base DN string and a message explaining the problem
   * that was encountered.
   */
  public static final int MSGID_LDIFEXPORT_CANNOT_DECODE_EXCLUDE_BASE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 57;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to decode the wrap column as an integer.  This takes a single
   * argument, which is the string representation of the wrap column.
   */
  public static final int
       MSGID_LDIFEXPORT_CANNOT_DECODE_WRAP_COLUMN_AS_INTEGER =
            CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 58;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to perform the export.  This takes a single argument, which is a
   * message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFEXPORT_ERROR_DURING_EXPORT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 59;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to decode the backend configuration base DN string.  This takes two
   * arguments, which are the backend config base DN string and a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_LDIFEXPORT_CANNOT_DECODE_BACKEND_BASE_DN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 60;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to retrieve the backend configuration base entry.  This takes two
   * arguments, which are the DN of the entry and a message explaining the
   * problem that occurred.
   */
  public static final int MSGID_LDIFEXPORT_CANNOT_RETRIEVE_BACKEND_BASE_ENTRY =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 61;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to determine the backend class name.  This takes two arguments,
   * which are the DN of the backend configuration entry and a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_LDIFEXPORT_CANNOT_DETERMINE_BACKEND_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 62;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to load a class for a Directory Server backend.  This takes three
   * arguments, which are the class name, the DN of the configuration entry, and
   * a message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFEXPORT_CANNOT_LOAD_BACKEND_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 63;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to instantiate a class for a Directory Server backend.  This takes
   * three arguments, which are the class name, the DN of the configuration
   * entry, and a message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFEXPORT_CANNOT_INSTANTIATE_BACKEND_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 64;



  /**
   * The message ID for the message that will be used if a backend configuration
   * entry does not define any base DNs.  This takes a single argument, which is
   * the DN of the backend configuration entry.
   */
  public static final int MSGID_LDIFEXPORT_NO_BASES_FOR_BACKEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 65;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to determine the set of base DNs for a backend.  This takes two
   * arguments, which are the DN of the backend configuration entry and a string
   * representation of the exception that was caught.
   */
  public static final int MSGID_LDIFEXPORT_CANNOT_DETERMINE_BASES_FOR_BACKEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 66;



  /**
   * The message ID for the message that will be used as the description of the
   * configClass argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFIMPORT_DESCRIPTION_CONFIG_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 67;



  /**
   * The message ID for the message that will be used as the description of the
   * configFile argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFIMPORT_DESCRIPTION_CONFIG_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 68;



  /**
   * The message ID for the message that will be used as the description of the
   * ldifFile argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFIMPORT_DESCRIPTION_LDIF_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 69;



  /**
   * The message ID for the message that will be used as the description of the
   * append argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFIMPORT_DESCRIPTION_APPEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 70;



  /**
   * The message ID for the message that will be used as the description of the
   * replaceExisting argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFIMPORT_DESCRIPTION_REPLACE_EXISTING =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 71;



  /**
   * The message ID for the message that will be used as the description of the
   * backendID argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFIMPORT_DESCRIPTION_BACKEND_ID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 72;



  /**
   * The message ID for the message that will be used as the description of the
   * excludeBranch argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFIMPORT_DESCRIPTION_EXCLUDE_BRANCH =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 73;



  /**
   * The message ID for the message that will be used as the description of the
   * includeAttribute argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFIMPORT_DESCRIPTION_INCLUDE_ATTRIBUTE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 74;



  /**
   * The message ID for the message that will be used as the description of the
   * excludeAttribute argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFIMPORT_DESCRIPTION_EXCLUDE_ATTRIBUTE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 75;



  /**
   * The message ID for the message that will be used as the description of the
   * includeFilter argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFIMPORT_DESCRIPTION_INCLUDE_FILTER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 76;



  /**
   * The message ID for the message that will be used as the description of the
   * excludeFilter argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFIMPORT_DESCRIPTION_EXCLUDE_FILTER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 77;



  /**
   * The message ID for the message that will be used as the description of the
   * rejectFile argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFIMPORT_DESCRIPTION_REJECT_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 78;



  /**
   * The message ID for the message that will be used as the description of the
   * overwriteRejects argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFIMPORT_DESCRIPTION_OVERWRITE_REJECTS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 79;



  /**
   * The message ID for the message that will be used as the description of the
   * isCompressed argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFIMPORT_DESCRIPTION_IS_COMPRESSED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 80;



  /**
   * The message ID for the message that will be used as the description of the
   * isEncrypted argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFIMPORT_DESCRIPTION_IS_ENCRYPTED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 81;



  /**
   * The message ID for the message that will be used as the description of the
   * help argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFIMPORT_DESCRIPTION_USAGE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 82;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the command-line arguments.  This takes a single argument,
   * which is an explanation of the problem that occurred.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_INITIALIZE_ARGS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 83;



  /**
   * The message ID for the message that will be used if an error occurs while
   * parsing the command-line arguments.  This takes a single argument, which is
   * an explanation of the problem that occurred.
   */
  public static final int MSGID_LDIFIMPORT_ERROR_PARSING_ARGS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 84;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to bootstrap the Directory Server.  This takes a single argument,
   * which is a string representation of the exception that was caught.
   */
  public static final int MSGID_LDIFIMPORT_SERVER_BOOTSTRAP_ERROR =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 85;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying load the Directory Server configuration.  This takes a single
   * argument which is a message with information about the problem that
   * occurred.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_LOAD_CONFIG =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 86;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to load the Directory Server schema.  This takes a single argument,
   * which is a message with information about the problem that occurred.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_LOAD_SCHEMA =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 87;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to initialize the core Directory Server configuration.  This takes a
   * single argument, which is a message with information about the problem that
   * occurred.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_INITIALIZE_CORE_CONFIG =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 88;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to decode an exclude filter.  This takes two arguments, which are
   * the provided filter string and a message explaining the problem that was
   * encountered.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_PARSE_EXCLUDE_FILTER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 89;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to decode an include filter.  This takes two arguments, which are
   * the provided filter string and a message explaining the problem that was
   * encountered.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_PARSE_INCLUDE_FILTER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 90;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to decode the base DN string.  This takes two arguments, which are
   * the provided base DN string and a message explaining the problem that
   * was encountered.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_DECODE_BASE_DN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 91;



  /**
   * The message ID for the message that will be used if multiple backends claim
   * to have the requested backend ID.  This takes a single argument, which is
   * the requested base DN.
   */
  public static final int MSGID_LDIFIMPORT_MULTIPLE_BACKENDS_FOR_ID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 92;



  /**
   * The message ID for the message that will be used if no backends claim to
   * have the requested backend ID.  This takes a single argument, which is the
   * requested base DN.
   */
  public static final int MSGID_LDIFIMPORT_NO_BACKENDS_FOR_ID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 93;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to decode the exclude branch string.  This takes two arguments,
   * which are the provided base DN string and a message explaining the problem
   * that was encountered.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_DECODE_EXCLUDE_BASE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 94;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to open the rejects file.  This takes two arguments, which are the
   * path to the rejects file and a string representation of the exception that
   * was caught.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_OPEN_REJECTS_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 95;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to perform the import.  This takes a single argument, which is a
   * message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFIMPORT_ERROR_DURING_IMPORT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 96;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to decode the backend configuration base DN string.  This takes two
   * arguments, which are the backend config base DN string and a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_DECODE_BACKEND_BASE_DN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 97;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to retrieve the backend configuration base entry.  This takes two
   * arguments, which are the DN of the entry and a message explaining the
   * problem that occurred.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_RETRIEVE_BACKEND_BASE_ENTRY =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 98;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to determine the backend class name.  This takes two arguments,
   * which are the DN of the backend configuration entry and a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_DETERMINE_BACKEND_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 99;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to load a class for a Directory Server backend.  This takes three
   * arguments, which are the class name, the DN of the configuration entry, and
   * a message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_LOAD_BACKEND_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 100;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to instantiate a class for a Directory Server backend.  This takes
   * three arguments, which are the class name, the DN of the configuration
   * entry, and a message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_INSTANTIATE_BACKEND_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 101;



  /**
   * The message ID for the message that will be used if a backend configuration
   * entry does not define any base DNs.  This takes a single argument, which is
   * the DN of the backend configuration entry.
   */
  public static final int MSGID_LDIFIMPORT_NO_BASES_FOR_BACKEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 102;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to determine the set of base DNs for a backend.  This takes two
   * arguments, which are the DN of the backend configuration entry and a string
   * representation of the exception that was caught.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_DETERMINE_BASES_FOR_BACKEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 103;


  /**
   * The message ID for the message that will be give information
   * on what operation is being processed.
   */
  public static final int MSGID_PROCESSING_OPERATION =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 104;


  /**
   * The message ID for the message that will be give information
   * on the failure of an operation.
   */
  public static final int MSGID_OPERATION_FAILED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 105;


  /**
   * The message ID for the message that will be give information
   * when an operation is successful.
   */
  public static final int MSGID_OPERATION_SUCCESSFUL =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 106;

  /**
   * The message ID for the message that will be give information
   * when a compare operation is processed.
   */
  public static final int MSGID_PROCESSING_COMPARE_OPERATION =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 107;


  /**
   * The message ID for the message that will be give information
   * when a compare operation returns false.
   */
  public static final int MSGID_COMPARE_OPERATION_RESULT_FALSE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 108;


  /**
   * The message ID for the message that will be give information
   * when a compare operation returns true.
   */
  public static final int MSGID_COMPARE_OPERATION_RESULT_TRUE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 109;


  /**
   * The message ID for the message that will be give information
   * when an invalid protocol operation is returned in a
   * search result.
   */
  public static final int MSGID_SEARCH_OPERATION_INVALID_PROTOCOL =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 110;



  /**
   * The message ID for the message that will be used as the description of the
   * trustAll argument.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_TRUSTALL =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 111;


  /**
   * The message ID for the message that will be used as the description of the
   * bindDN argument.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_BINDDN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 112;


  /**
   * The message ID for the message that will be used as the description of the
   * bindPassword argument.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_BINDPASSWORD =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 113;


  /**
   * The message ID for the message that will be used as the description of the
   * bindPasswordFile argument.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_BINDPASSWORDFILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 114;


  /**
   * The message ID for the message that will be used as the description of the
   * encoding argument.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_ENCODING =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 115;


  /**
   * The message ID for the message that will be used as the description of the
   * verbose argument.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_VERBOSE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 116;


  /**
   * The message ID for the message that will be used as the description of the
   * keystorePath argument.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_KEYSTOREPATH =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 117;


  /**
   * The message ID for the message that will be used as the description of the
   * trustStorePath argument.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_TRUSTSTOREPATH =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 118;


  /**
   * The message ID for the message that will be used as the description of the
   * keystorePassword argument.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_KEYSTOREPASSWORD =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 119;


  /**
   * The message ID for the message that will be used as the description of the
   * host argument.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_HOST =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 120;


  /**
   * The message ID for the message that will be used as the description of the
   * port argument.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_PORT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 121;


  /**
   * The message ID for the message that will be used as the description of the
   * showUsage argument.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_SHOWUSAGE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 122;


  /**
   * The message ID for the message that will be used as the description of the
   * controls argument.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_CONTROLS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 123;


  /**
   * The message ID for the message that will be used as the description of the
   * continueOnError argument.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_CONTINUE_ON_ERROR =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 124;


  /**
   * The message ID for the message that will be used as the description of the
   * useSSL argument.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_USE_SSL =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 125;


  /**
   * The message ID for the message that will be used as the description of the
   * startTLS argument.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_START_TLS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 126;


  /**
   * The message ID for the message that will be used as the description of the
   * useSASLExternal argument.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_USE_SASL_EXTERNAL =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 127;


  /**
   * The message ID for the message that will be used as the description of the
   * filename argument.  This does not take any arguments.
   */
  public static final int MSGID_DELETE_DESCRIPTION_FILENAME =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 128;



  /**
   * The message ID for the message that will be used as the description of the
   * deleteSubtree argument.  This does not take any arguments.
   */
  public static final int MSGID_DELETE_DESCRIPTION_DELETE_SUBTREE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 129;


  /**
   * The message ID for the message that will be used as the description of the
   * defaultAdd argument.  This does not take any arguments.
   */
  public static final int MSGID_MODIFY_DESCRIPTION_DEFAULT_ADD =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 130;


  /**
   * The message ID for the message that will be used as the description of the
   * baseDN argument.  This does not take any arguments.
   */
  public static final int MSGID_SEARCH_DESCRIPTION_BASEDN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 131;


  /**
   * The message ID for the message that will be used as the description of the
   * sizeLimit argument.  This does not take any arguments.
   */
  public static final int MSGID_SEARCH_DESCRIPTION_SIZE_LIMIT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 132;


  /**
   * The message ID for the message that will be used as the description of the
   * timeLimit argument.  This does not take any arguments.
   */
  public static final int MSGID_SEARCH_DESCRIPTION_TIME_LIMIT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 133;


  /**
   * The message ID for the message that will be used as the description of the
   * searchScope argument.  This does not take any arguments.
   */
  public static final int MSGID_SEARCH_DESCRIPTION_SEARCH_SCOPE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 134;


  /**
   * The message ID for the message that will be used as the description of the
   * dereferencePolicy argument.  This does not take any arguments.
   */
  public static final int MSGID_SEARCH_DESCRIPTION_DEREFERENCE_POLICY =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 135;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to send a simple bind request to the Directory Server.  This takes a
   * single argument, which is a string representation of the exception that was
   * caught.
   */
  public static final int MSGID_LDAPAUTH_CANNOT_SEND_SIMPLE_BIND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 136;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to read the bind response from the Directory Server.  This takes a
   * single argument, which is a string representation of the exception that was
   * caught.
   */
  public static final int MSGID_LDAPAUTH_CANNOT_READ_BIND_RESPONSE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 137;



  /**
   * The message ID for the message that will be used if the server sends a
   * notice of disconnection unsolicited response.  This takes two arguments,
   * which are the result code and error message from the notice of
   * disconnection response.
   */
  public static final int MSGID_LDAPAUTH_SERVER_DISCONNECT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 138;



  /**
   * The message ID for the message that will be used if the server sends an
   * unexpected extended response to the client.  This takes a single argument,
   * which is a string representation of the extended response that was
   * received.
   */
  public static final int MSGID_LDAPAUTH_UNEXPECTED_EXTENDED_RESPONSE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 139;



  /**
   * The message ID for the message that will be used if the server sends an
   * unexpected response to the client.  This takes a single argument, which is
   * a string representation of the extended response that was received.
   */
  public static final int MSGID_LDAPAUTH_UNEXPECTED_RESPONSE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 140;



  /**
   * The message ID for the message that will be used if the simple bind attempt
   * fails.  This takes three arguments, which are the integer and string
   * representations of the result code and the error message from the bind
   * response.
   */
  public static final int MSGID_LDAPAUTH_SIMPLE_BIND_FAILED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 141;



  /**
   * The message ID for the message that will be used if an attempt was made to
   * process a SASL bind without specifying which SASL mechanism to use.  This
   * does not take any arguments.
   */
  public static final int MSGID_LDAPAUTH_NO_SASL_MECHANISM =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 142;



  /**
   * The message ID for the message that will be used if an unsupported SASL
   * mechanism was requested.  This takes a single argument, which is the
   * requested mechanism.
   */
  public static final int MSGID_LDAPAUTH_UNSUPPORTED_SASL_MECHANISM =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 143;



  /**
   * The message ID for the message that will be used if multiple values are
   * provided for the trace property when performing a SASL ANONYMOUS bind.
   * This does not take any arguments.
   */
  public static final int MSGID_LDAPAUTH_TRACE_SINGLE_VALUED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 144;



  /**
   * The message ID for the message that will be used if an invalid SASL
   * property is provided.  This takes two arguments, which are the name of the
   * invalid property and the name of the SASL mechanism.
   */
  public static final int MSGID_LDAPAUTH_INVALID_SASL_PROPERTY =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 145;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to send a SASL bind request to the Directory Server.  This takes two
   * arguments, which are the name of the SASL mechanism and a string
   * representation of the exception that was caught.
   */
  public static final int MSGID_LDAPAUTH_CANNOT_SEND_SASL_BIND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 146;



  /**
   * The message ID for the message that will be used if a SASL bind attempt
   * fails.  This takes four arguments, which are the SASL mechanism name,
   * integer and string representations of the result code and the error message
   * from the bind response.
   */
  public static final int MSGID_LDAPAUTH_SASL_BIND_FAILED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 147;



  /**
   * The message ID for the message that will be used if no SASL properties are
   * provided for a SASL mechanism that requires at least one such property.
   * This takes a single argument, which is the name of the SASL mechanism.
   */
  public static final int MSGID_LDAPAUTH_NO_SASL_PROPERTIES =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 148;



  /**
   * The message ID for the message that will be used if an attempt is made to
   * specify multiple values for the authID property.  This does not take any
   * arguments.
   */
  public static final int MSGID_LDAPAUTH_AUTHID_SINGLE_VALUED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 149;



  /**
   * The message ID for the message that will be used if no value is provided
   * for the required authID SASL property.  This takes a single argument, which
   * is the name of the SASL mechanism.
   */
  public static final int MSGID_LDAPAUTH_SASL_AUTHID_REQUIRED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 150;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to send the initial bind request in a multi-stage SASL bind.  This
   * takes two arguments, which are the name of the SASL mechanism and a string
   * representation of the exception that was caught.
   */
  public static final int MSGID_LDAPAUTH_CANNOT_SEND_INITIAL_SASL_BIND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 151;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to read the initial bind response in a multi-stage SASL bind.  This
   * takes two arguments, which are the name of the SASL mechanism and a string
   * representation of the exception that was caught.
   */
  public static final int MSGID_LDAPAUTH_CANNOT_READ_INITIAL_BIND_RESPONSE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 152;



  /**
   * The message ID for the message that will be used if the initial response
   * message in a multi-stage SASL bind had a result code other than "SASL bind
   * in progress".  This takes four arguments, which are the name of the SASL
   * mechanism, integer and string representations of the result code, and the
   * error message from the bind response.
   */
  public static final int MSGID_LDAPAUTH_UNEXPECTED_INITIAL_BIND_RESPONSE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 153;



  /**
   * The message ID for the message that will be used if the initial CRAM-MD5
   * bind response does not include server SASL credentials.  This does not take
   * any arguments.
   */
  public static final int MSGID_LDAPAUTH_NO_CRAMMD5_SERVER_CREDENTIALS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 154;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to initialize the MD5 message digest.  This takes a single argument,
   * which is a string representation of the exception that was caught.
   */
  public static final int MSGID_LDAPAUTH_CANNOT_INITIALIZE_MD5_DIGEST =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 155;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to send the second bind request in a multi-stage SASL bind.  This
   * takes two arguments, which are the name of the SASL mechanism and a string
   * representation of the exception that was caught.
   */
  public static final int MSGID_LDAPAUTH_CANNOT_SEND_SECOND_SASL_BIND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 156;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to read the second bind response in a multi-stage SASL bind.  This
   * takes two arguments, which are the name of the SASL mechanism and a string
   * representation of the exception that was caught.
   */
  public static final int MSGID_LDAPAUTH_CANNOT_READ_SECOND_BIND_RESPONSE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 157;



  /**
   * The message ID for the message that will be used if one or more SASL
   * properties were provided for a mechanism that does not support them.  This
   * takes a single argument, which is the name of the SASL mechanism.
   */
  public static final int MSGID_LDAPAUTH_NO_ALLOWED_SASL_PROPERTIES =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 158;



  /**
   * The message ID for the message that will be used if an attempt is made to
   * specify multiple values for the authzID property.  This does not take any
   * arguments.
   */
  public static final int MSGID_LDAPAUTH_AUTHZID_SINGLE_VALUED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 159;



  /**
   * The message ID for the message that will be used if an attempt is made to
   * specify multiple values for the realm property.  This does not take any
   * arguments.
   */
  public static final int MSGID_LDAPAUTH_REALM_SINGLE_VALUED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 160;



  /**
   * The message ID for the message that will be used if an attempt is made to
   * specify multiple values for the QoP property.  This does not take any
   * arguments.
   */
  public static final int MSGID_LDAPAUTH_QOP_SINGLE_VALUED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 161;



  /**
   * The message ID for the message that will be used if either the auth-int or
   * auth-conf QoP is requested for DIGEST-MD5 authentication.  This takes a
   * single value, which is the requested QoP.
   */
  public static final int MSGID_LDAPAUTH_DIGESTMD5_QOP_NOT_SUPPORTED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 162;



  /**
   * The message ID for the message that will be used if an invalid QoP mode is
   * requested.  This takes a single value, which is the requested QoP.
   */
  public static final int MSGID_LDAPAUTH_DIGESTMD5_INVALID_QOP =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 163;



  /**
   * The message ID for the message that will be used if an attempt is made to
   * specify multiple values for the digest-URI property.  This does not take
   * any arguments.
   */
  public static final int MSGID_LDAPAUTH_DIGEST_URI_SINGLE_VALUED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 164;



  /**
   * The message ID for the message that will be used if the initial DIGEST-MD5
   * bind response does not include server SASL credentials.  This does not take
   * any arguments.
   */
  public static final int MSGID_LDAPAUTH_NO_DIGESTMD5_SERVER_CREDENTIALS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 165;



  /**
   * The message ID for the message that will be used if the client cannot parse
   * the server SASL credentials because an invalid token was encountered.  This
   * takes two arguments, which are the invalid token and the position at which
   * it starts in the server SASL credentials.
   */
  public static final int
       MSGID_LDAPAUTH_DIGESTMD5_INVALID_TOKEN_IN_CREDENTIALS =
            CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 166;



  /**
   * The message ID for the message that will be used if the server SASL
   * credentials includes an invalid character set name.  This takes a single
   * argument, which is the name of the invalid character set.
   */
  public static final int MSGID_LDAPAUTH_DIGESTMD5_INVALID_CHARSET =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 167;



  /**
   * The message ID for the message that will be used if the QoP mode that the
   * client intends to use is not supported by the server.  This takes two
   * arguments, which are the client's requested QoP mode and the server's list
   * of supported QoP modes.
   */
  public static final int MSGID_LDAPAUTH_REQUESTED_QOP_NOT_SUPPORTED_BY_SERVER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 168;



  /**
   * The message ID for the message that will be used if the server SASL
   * credentials for a DIGEST-MD5 bind do not include a nonce.  This does not
   * take any arguments.
   */
  public static final int MSGID_LDAPAUTH_DIGESTMD5_NO_NONCE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 169;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to generate the DIGEST-MD5 response digest.  This takes a single
   * argument, which is a string representation of the exception that was
   * caught.
   */
  public static final int
       MSGID_LDAPAUTH_DIGESTMD5_CANNOT_CREATE_RESPONSE_DIGEST =
            CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 170;



  /**
   * The message ID for the message that will be used if the server SASL
   * credentials for a DIGEST-MD5 second-stage bind response does not include
   * the rspauth element.  This does not take any arguments.
   */
  public static final int MSGID_LDAPAUTH_DIGESTMD5_NO_RSPAUTH_CREDS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 171;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to decode the rspauth value provided by the server in the DIGEST-MD5
   * bind.  This takes a single argument, which is a string representation of
   * the exception that was caught.
   */
  public static final int MSGID_LDAPAUTH_DIGESTMD5_COULD_NOT_DECODE_RSPAUTH =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 172;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to calculate the rspauth value to compare with the version provided
   * by the server in the DIGEST-MD5 bind.  This takes a single argument, which
   * is a string representation of the exception that was caught.
   */
  public static final int MSGID_LDAPAUTH_DIGESTMD5_COULD_NOT_CALCULATE_RSPAUTH =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 173;



  /**
   * The message ID for the message that will be used if the server-provided
   * rspauth value differs from the value calculated by the client.  This does
   * not take any arguments.
   */
  public static final int MSGID_LDAPAUTH_DIGESTMD5_RSPAUTH_MISMATCH =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 174;



  /**
   * The message ID for the message that will be used if the server SASL
   * credentials contains a closing quotation mark in an unexpected location.
   * This takes a single argument, which is the position of the unexpected
   * quote.
   */
  public static final int MSGID_LDAPAUTH_DIGESTMD5_INVALID_CLOSING_QUOTE_POS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 175;



  /**
   * The message ID for the message that will be used as the description of the
   * trace SASL property.  This does not take any arguments.
   */
  public static final int MSGID_LDAPAUTH_PROPERTY_DESCRIPTION_TRACE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 176;



  /**
   * The message ID for the message that will be used as the description of the
   * authID SASL property.  This does not take any arguments.
   */
  public static final int MSGID_LDAPAUTH_PROPERTY_DESCRIPTION_AUTHID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 177;



  /**
   * The message ID for the message that will be used as the description of the
   * realm SASL property.  This does not take any arguments.
   */
  public static final int MSGID_LDAPAUTH_PROPERTY_DESCRIPTION_REALM =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 178;



  /**
   * The message ID for the message that will be used as the description of the
   * QoP SASL property.  This does not take any arguments.
   */
  public static final int MSGID_LDAPAUTH_PROPERTY_DESCRIPTION_QOP =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 179;



  /**
   * The message ID for the message that will be used as the description of the
   * digest URI SASL property.  This does not take any arguments.
   */
  public static final int MSGID_LDAPAUTH_PROPERTY_DESCRIPTION_DIGEST_URI =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 180;



  /**
   * The message ID for the message that will be used as the description of the
   * authzID SASL property.  This does not take any arguments.
   */
  public static final int MSGID_LDAPAUTH_PROPERTY_DESCRIPTION_AUTHZID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 181;


  /**
   * The message ID for the message that will be used as the description of the
   * SASL properties.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_SASL_PROPERTIES =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 182;



  /**
   * The message ID for the message that will be used as the description of the
   * KDC SASL property.  This does not take any arguments.
   */
  public static final int MSGID_LDAPAUTH_PROPERTY_DESCRIPTION_KDC =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 183;



  /**
   * The message ID for the message that will be used if an attempt is made to
   * specify multiple values for the KDC property.  This does not take any
   * arguments.
   */
  public static final int MSGID_LDAPAUTH_KDC_SINGLE_VALUED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 184;



  /**
   * The message ID for the message that will be used if an invalid QoP mode is
   * requested.  This takes a single value, which is the requested QoP.
   */
  public static final int MSGID_LDAPAUTH_GSSAPI_INVALID_QOP =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 185;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to create the JAAS temporary configuration for GSSAPI
   * authentication.  This takes a single argument, which is a string
   * representation of the exception that was caught.
   */
  public static final int MSGID_LDAPAUTH_GSSAPI_CANNOT_CREATE_JAAS_CONFIG =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 186;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to perform Kerberos authentication on the client system.  This takes
   * a single argument, which is a string representation of the exception that
   * was caught.
   */
  public static final int MSGID_LDAPAUTH_GSSAPI_LOCAL_AUTHENTICATION_FAILED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 187;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to perform GSSAPI authentication to the Directory Server.  This
   * takes a single argument, which is a string representation of the exception
   * that was caught.
   */
  public static final int MSGID_LDAPAUTH_GSSAPI_REMOTE_AUTHENTICATION_FAILED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 188;



  /**
   * The message ID for the message that will be used if the
   * LDAPAuthenticationHandler.run method is called for a non-SASL bind.  This
   * takes a single argument, which is a backtrace of the current thread showing
   * the invalid call.
   */
  public static final int MSGID_LDAPAUTH_NONSASL_RUN_INVOCATION =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 189;



  /**
   * The message ID for the message that will be used if the
   * LDAPAuthenticationHandler.run method is called for a SASL bind using an
   * unexpected mechanism.  This takes two arguments, which are the SASL
   * mechanism and a backtrace of the current thread showing the invalid call.
   */
  public static final int MSGID_LDAPAUTH_UNEXPECTED_RUN_INVOCATION =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 190;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to instantiate a SASL client to handle the GSSAPI authentication.
   * This takes a single argument, which is a string representation of the
   * exception that was caught.
   */
  public static final int MSGID_LDAPAUTH_GSSAPI_CANNOT_CREATE_SASL_CLIENT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 191;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to create the initial challenge for GSSAPI authentication.  This
   * takes a single argument, which is a string representation of the exception
   * that was caught.
   */
  public static final int
       MSGID_LDAPAUTH_GSSAPI_CANNOT_CREATE_INITIAL_CHALLENGE =
            CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 192;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to validate the server SASL credentials included in a GSSAPI bind
   * response.  This takes a single argument, which is a string representation
   * of the exception that was caught.
   */
  public static final int MSGID_LDAPAUTH_GSSAPI_CANNOT_VALIDATE_SERVER_CREDS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 193;



  /**
   * The message ID for the message that will be used if the Directory Server
   * indicates that a GSSAPI bind is complete when the SASL client does not
   * believe that to be the case.  This does not take any arguments.
   */
  public static final int MSGID_LDAPAUTH_GSSAPI_UNEXPECTED_SUCCESS_RESPONSE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 194;



  /**
   * The message ID for the message that will be used if the Directory Server
   * sent a bind response that was neither "success" nor "SASL bind in
   * progress".  This takes three arguments, which are the result code, a
   * string representation of the result code, and the error message from the
   * bind response (if any).
   */
  public static final int MSGID_LDAPAUTH_GSSAPI_BIND_FAILED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 195;



  /**
   * The message ID for the message that will be used if the
   * LDAPAuthenticationHandler.handle method is called for a non-SASL bind.
   * This takes a single argument, which is a backtrace of the current thread
   * showing the invalid call.
   */
  public static final int MSGID_LDAPAUTH_NONSASL_CALLBACK_INVOCATION =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 196;



  /**
   * The message ID for the message that will be used if the
   * LDAPAuthenticationHandler.handle method is called for the GSSAPI mechanism
   * but with an unexpected callback type.  This takes a single argument, which
   * is a string representation of the callback type.
   */
  public static final int MSGID_LDAPAUTH_UNEXPECTED_GSSAPI_CALLBACK =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 197;



  /**
   * The message ID for the message that will be used if the
   * LDAPAuthenticationHandler.handle method is called for a SASL bind with an
   * unexpected mechanism.  This takes two arguments, which are the SASL
   * mechanism and a backtrace of the current thread showing the invalid call.
   */
  public static final int MSGID_LDAPAUTH_UNEXPECTED_CALLBACK_INVOCATION =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 198;



  /**
   * The message ID for the message that will be used to interactively prompt
   * a user for an authentication password.  This takes a single argument, which
   * is the username or bind DN for which to retrieve the password.
   */
  public static final int MSGID_LDAPAUTH_PASSWORD_PROMPT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 199;


  /**
   * The message ID for the message that will be used as the description of the
   * version argument.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_VERSION =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 200;


  /**
   * The message ID for the message that will be used as the description of the
   * invalid version message.
   */
  public static final int MSGID_DESCRIPTION_INVALID_VERSION =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 201;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to send a "Who Am I?" request to the Directory Server.  This takes a
   * single argument, which is a string representation of the exception that was
   * caught.
   */
  public static final int MSGID_LDAPAUTH_CANNOT_SEND_WHOAMI_REQUEST =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 202;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to read the "Who Am I?" request to the Directory Server.  This takes
   * a single argument, which is a string representation of the exception that
   * was caught.
   */
  public static final int MSGID_LDAPAUTH_CANNOT_READ_WHOAMI_RESPONSE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 203;



  /**
   * The message ID for the message that will be used if the "Who Am I?" request
   * was rejected by the server.  This takes three arguments, which are the
   * result code from the response, a string representation of that result code,
   * and the error message from the response.
   */
  public static final int MSGID_LDAPAUTH_WHOAMI_FAILED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 204;


  /**
   * The message ID for the message that will be used if an invalid search
   * scope is provided.
   */
  public static final int MSGID_SEARCH_INVALID_SEARCH_SCOPE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 205;


  /**
   * The message ID for the message that will be used if no filters
   * are specified for the search request.
   */
  public static final int MSGID_SEARCH_NO_FILTERS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 206;


  /**
   * The message ID for the message that will be used as the description of the
   * index name argument.  This does not take any arguments.
   */
  public static final int MSGID_VERIFYINDEX_DESCRIPTION_BASE_DN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 207;


  /**
   * The message ID for the message that will be used as the description of the
   * index name argument.  This does not take any arguments.
   */
  public static final int MSGID_VERIFYINDEX_DESCRIPTION_INDEX_NAME =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 208;


  /**
   * The message ID for the message that will be used as the description of the
   * argument requesting that an index should be verified to ensure it is clean.
   * This does not take any arguments.
   */
  public static final int MSGID_VERIFYINDEX_DESCRIPTION_VERIFY_CLEAN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 209;


  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to perform index verification.  This takes a single argument,
   * which is a message explaining the problem that occurred.
   */
  public static final int MSGID_VERIFYINDEX_ERROR_DURING_VERIFY =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 210;


  /**
   * The message ID for the message that will be used if a request to
   * verify an index for cleanliness does not specify a single index.
   */
  public static final int MSGID_VERIFYINDEX_VERIFY_CLEAN_REQUIRES_SINGLE_INDEX =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 211;


  /**
   * The message ID for the message that will be used if a request to
   * verify indexes specifies the base DN of a backend that does not
   * support indexing.
   */
  public static final int MSGID_VERIFYINDEX_WRONG_BACKEND_TYPE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 212;



  /**
   * The message ID for the message that will be used if the backend selected
   * for an LDIF export does not support that operation.  This takes a single
   * argument, which is the requested backend ID.
   */
  public static final int MSGID_LDIFEXPORT_CANNOT_EXPORT_BACKEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 213;



  /**
   * The message ID for the message that will be used if the backend selected
   * for an LDIF import does not support that operation.  This takes a single
   * argument, which is the requested base DN.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_IMPORT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 214;



  /**
   * The message ID for the message that will be used as the description of the
   * dontWrap property.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_DONT_WRAP =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 215;



  /**
   * The message ID for the message that will be used as the description of the
   * includeBranch argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFIMPORT_DESCRIPTION_INCLUDE_BRANCH =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 216;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to determine the backend ID.  This takes two arguments, which are
   * the DN of the backend configuration entry and a message explaining the
   * problem that occurred.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_DETERMINE_BACKEND_ID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 217;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to decode the include branch string.  This takes two arguments,
   * which are the provided base DN string and a message explaining the problem
   * that was encountered.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_DECODE_INCLUDE_BASE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 218;



  /**
   * The message ID for the message that will be used a requested include base
   * does not exist in the targeted backend.  This takes two arguments, which
   * are the specified include branch DN and the requested backend ID.
   */
  public static final int MSGID_LDIFIMPORT_INVALID_INCLUDE_BASE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 219;



  /**
   * The message ID for the message that will be used as the description of the
   * configClass argument.  This does not take any arguments.
   */
  public static final int MSGID_VERIFYINDEX_DESCRIPTION_CONFIG_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 220;



  /**
   * The message ID for the message that will be used as the description of the
   * configFile argument.  This does not take any arguments.
   */
  public static final int MSGID_VERIFYINDEX_DESCRIPTION_CONFIG_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 221;



  /**
   * The message ID for the message that will be used as the description of the
   * help argument.  This does not take any arguments.
   */
  public static final int MSGID_VERIFYINDEX_DESCRIPTION_USAGE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 222;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the command-line arguments.  This takes a single argument,
   * which is an explanation of the problem that occurred.
   */
  public static final int MSGID_VERIFYINDEX_CANNOT_INITIALIZE_ARGS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 223;



  /**
   * The message ID for the message that will be used if an error occurs while
   * parsing the command-line arguments.  This takes a single argument, which is
   * an explanation of the problem that occurred.
   */
  public static final int MSGID_VERIFYINDEX_ERROR_PARSING_ARGS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 224;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to bootstrap the Directory Server.  This takes a single argument,
   * which is a string representation of the exception that was caught.
   */
  public static final int MSGID_VERIFYINDEX_SERVER_BOOTSTRAP_ERROR =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 225;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying load the Directory Server configuration.  This takes a single
   * argument which is a message with information about the problem that
   * occurred.
   */
  public static final int MSGID_VERIFYINDEX_CANNOT_LOAD_CONFIG =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 226;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to load the Directory Server schema.  This takes a single argument,
   * which is a message with information about the problem that occurred.
   */
  public static final int MSGID_VERIFYINDEX_CANNOT_LOAD_SCHEMA =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 227;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to initialize the core Directory Server configuration.  This takes a
   * single argument, which is a message with information about the problem that
   * occurred.
   */
  public static final int MSGID_VERIFYINDEX_CANNOT_INITIALIZE_CORE_CONFIG =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 228;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to decode the base DN string.  This takes two arguments, which are
   * the provided base DN string and a message explaining the problem that
   * was encountered.
   */
  public static final int MSGID_VERIFYINDEX_CANNOT_DECODE_BASE_DN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 229;



  /**
   * The message ID for the message that will be used if multiple backends claim
   * to support the requested base DN.  This takes a single argument, which is
   * the requested base DN.
   */
  public static final int MSGID_VERIFYINDEX_MULTIPLE_BACKENDS_FOR_BASE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 230;



  /**
   * The message ID for the message that will be used if no backends claim to
   * support the requested base DN.  This takes a single argument, which is the
   * requested base DN.
   */
  public static final int MSGID_VERIFYINDEX_NO_BACKENDS_FOR_BASE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 231;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to decode the backend configuration base DN string.  This takes two
   * arguments, which are the backend config base DN string and a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_VERIFYINDEX_CANNOT_DECODE_BACKEND_BASE_DN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 232;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to retrieve the backend configuration base entry.  This takes two
   * arguments, which are the DN of the entry and a message explaining the
   * problem that occurred.
   */
  public static final int MSGID_VERIFYINDEX_CANNOT_RETRIEVE_BACKEND_BASE_ENTRY =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 233;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to determine the backend class name.  This takes two arguments,
   * which are the DN of the backend configuration entry and a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_VERIFYINDEX_CANNOT_DETERMINE_BACKEND_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 234;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to load a class for a Directory Server backend.  This takes three
   * arguments, which are the class name, the DN of the configuration entry, and
   * a message explaining the problem that occurred.
   */
  public static final int MSGID_VERIFYINDEX_CANNOT_LOAD_BACKEND_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 235;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to instantiate a class for a Directory Server backend.  This takes
   * three arguments, which are the class name, the DN of the configuration
   * entry, and a message explaining the problem that occurred.
   */
  public static final int MSGID_VERIFYINDEX_CANNOT_INSTANTIATE_BACKEND_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 236;



  /**
   * The message ID for the message that will be used if a backend configuration
   * entry does not define any base DNs.  This takes a single argument, which is
   * the DN of the backend configuration entry.
   */
  public static final int MSGID_VERIFYINDEX_NO_BASES_FOR_BACKEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 237;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to determine the set of base DNs for a backend.  This takes two
   * arguments, which are the DN of the backend configuration entry and a string
   * representation of the exception that was caught.
   */
  public static final int MSGID_VERIFYINDEX_CANNOT_DETERMINE_BASES_FOR_BACKEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 238;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to determine the backend ID.  This takes two arguments, which are
   * the DN of the backend configuration entry and a message explaining the
   * problem that occurred.
   */
  public static final int MSGID_LDIFEXPORT_CANNOT_DETERMINE_BACKEND_ID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 239;



  /**
   * The message ID for the message that will be used as the description of the
   * includeBranch argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFEXPORT_DESCRIPTION_INCLUDE_BRANCH =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 240;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to decode the include branch string.  This takes two arguments,
   * which are the provided base DN string and a message explaining the problem
   * that was encountered.
   */
  public static final int MSGID_LDIFEXPORT_CANNOT_DECODE_INCLUDE_BASE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 241;



  /**
   * The message ID for the message that will be used a requested include base
   * does not exist in the targeted backend.  This takes two arguments, which
   * are the specified include branch DN and the requested backend ID.
   */
  public static final int MSGID_LDIFEXPORT_INVALID_INCLUDE_BASE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 242;



  /**
   * The message ID for the message that will be used as the description of the
   * configClass argument.  This does not take any arguments.
   */
  public static final int MSGID_BACKUPDB_DESCRIPTION_CONFIG_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 243;



  /**
   * The message ID for the message that will be used as the description of the
   * configFile argument.  This does not take any arguments.
   */
  public static final int MSGID_BACKUPDB_DESCRIPTION_CONFIG_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 244;



  /**
   * The message ID for the message that will be used as the description of the
   * backendID argument.  This does not take any arguments.
   */
  public static final int MSGID_BACKUPDB_DESCRIPTION_BACKEND_ID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 245;



  /**
   * The message ID for the message that will be used as the description of the
   * backupID argument.  This does not take any arguments.
   */
  public static final int MSGID_BACKUPDB_DESCRIPTION_BACKUP_ID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 246;



  /**
   * The message ID for the message that will be used as the description of the
   * backupDirectory argument.  This does not take any arguments.
   */
  public static final int MSGID_BACKUPDB_DESCRIPTION_BACKUP_DIR =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 247;



  /**
   * The message ID for the message that will be used as the description of the
   * incremental argument.  This does not take any arguments.
   */
  public static final int MSGID_BACKUPDB_DESCRIPTION_INCREMENTAL =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 248;



  /**
   * The message ID for the message that will be used as the description of the
   * compress argument.  This does not take any arguments.
   */
  public static final int MSGID_BACKUPDB_DESCRIPTION_COMPRESS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 249;



  /**
   * The message ID for the message that will be used as the description of the
   * encrypt argument.  This does not take any arguments.
   */
  public static final int MSGID_BACKUPDB_DESCRIPTION_ENCRYPT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 250;



  /**
   * The message ID for the message that will be used as the description of the
   * hash argument.  This does not take any arguments.
   */
  public static final int MSGID_BACKUPDB_DESCRIPTION_HASH =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 251;



  /**
   * The message ID for the message that will be used as the description of the
   * signHash argument.  This does not take any arguments.
   */
  public static final int MSGID_BACKUPDB_DESCRIPTION_SIGN_HASH =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 252;



  /**
   * The message ID for the message that will be used as the description of the
   * help argument.  This does not take any arguments.
   */
  public static final int MSGID_BACKUPDB_DESCRIPTION_USAGE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 253;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the command-line arguments.  This takes a single argument,
   * which is an explanation of the problem that occurred.
   */
  public static final int MSGID_BACKUPDB_CANNOT_INITIALIZE_ARGS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 254;



  /**
   * The message ID for the message that will be used if an error occurs while
   * parsing the command-line arguments.  This takes a single argument, which is
   * an explanation of the problem that occurred.
   */
  public static final int MSGID_BACKUPDB_ERROR_PARSING_ARGS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 255;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to bootstrap the Directory Server.  This takes a single argument,
   * which is a string representation of the exception that was caught.
   */
  public static final int MSGID_BACKUPDB_SERVER_BOOTSTRAP_ERROR =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 256;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying load the Directory Server configuration.  This takes a single
   * argument which is a message with information about the problem that
   * occurred.
   */
  public static final int MSGID_BACKUPDB_CANNOT_LOAD_CONFIG =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 257;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to load the Directory Server schema.  This takes a single argument,
   * which is a message with information about the problem that occurred.
   */
  public static final int MSGID_BACKUPDB_CANNOT_LOAD_SCHEMA =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 258;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to initialize the core Directory Server configuration.  This takes a
   * single argument, which is a message with information about the problem that
   * occurred.
   */
  public static final int MSGID_BACKUPDB_CANNOT_INITIALIZE_CORE_CONFIG =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 259;



  /**
   * The message ID for the message that will be used if multiple backends claim
   * to have the requested backend ID.  This takes a single argument, which is
   * the requested backend ID.
   */
  public static final int MSGID_BACKUPDB_MULTIPLE_BACKENDS_FOR_ID=
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 260;



  /**
   * The message ID for the message that will be used if no backends claim to
   * have the requested backend ID.  This takes a single argument, which is the
   * requested backend ID.
   */
  public static final int MSGID_BACKUPDB_NO_BACKENDS_FOR_ID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 261;



  /**
   * The message ID for the message that will be used if the DN of the
   * configuration entry for the backend to archive does not match the DN of
   * the configuration entry used to generate previous backups in the same
   * backup directory.  This takes four arguments, which are the backend ID for
   * the backend to archive, the DN of the configuration entry for the backend
   * to archive, the path to the backup directory, and the DN of the
   * configuration entry for the backend used in previous backups into that
   * target directory.
   */
  public static final int MSGID_BACKUPDB_CONFIG_ENTRY_MISMATCH =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 262;



  /**
   * The message ID for the message that will be used if a problem occurs while
   * trying to use the requested path as a backup directory.  This takes two
   * arguments, which are the provided backup directory path, and a string
   * representation of the exception that was caught.
   */
  public static final int MSGID_BACKUPDB_INVALID_BACKUP_DIR =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 263;



  /**
   * The message ID for the message that will be used if the requested backend
   * cannot be backed up with the specified configuration.  This takes two
   * arguments, which are the backend ID for the target backend and a message
   * explaining the reason that the backup cannot be created.
   */
  public static final int MSGID_BACKUPDB_CANNOT_BACKUP =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 264;



  /**
   * The message ID for the message that will be used if an error occurs while
   * attempting to perform a backup.  This takes two arguments, which are the
   * backend ID for the target backend and a message explaining the problem that
   * occurred.
   */
  public static final int MSGID_BACKUPDB_ERROR_DURING_BACKUP =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 265;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to decode the backend configuration base DN string.  This takes two
   * arguments, which are the backend config base DN string and a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_BACKUPDB_CANNOT_DECODE_BACKEND_BASE_DN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 266;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to retrieve the backend configuration base entry.  This takes two
   * arguments, which are the DN of the entry and a message explaining the
   * problem that occurred.
   */
  public static final int MSGID_BACKUPDB_CANNOT_RETRIEVE_BACKEND_BASE_ENTRY =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 267;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to determine the backend class name.  This takes two arguments,
   * which are the DN of the backend configuration entry and a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_BACKUPDB_CANNOT_DETERMINE_BACKEND_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 268;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to load a class for a Directory Server backend.  This takes three
   * arguments, which are the class name, the DN of the configuration entry, and
   * a message explaining the problem that occurred.
   */
  public static final int MSGID_BACKUPDB_CANNOT_LOAD_BACKEND_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 269;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to instantiate a class for a Directory Server backend.  This takes
   * three arguments, which are the class name, the DN of the configuration
   * entry, and a message explaining the problem that occurred.
   */
  public static final int MSGID_BACKUPDB_CANNOT_INSTANTIATE_BACKEND_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 270;



  /**
   * The message ID for the message that will be used if a backend configuration
   * entry does not define any base DNs.  This takes a single argument, which is
   * the DN of the backend configuration entry.
   */
  public static final int MSGID_BACKUPDB_NO_BASES_FOR_BACKEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 271;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to determine the set of base DNs for a backend.  This takes two
   * arguments, which are the DN of the backend configuration entry and a string
   * representation of the exception that was caught.
   */
  public static final int MSGID_BACKUPDB_CANNOT_DETERMINE_BASES_FOR_BACKEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 272;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to determine the backend ID.  This takes two arguments, which are
   * the DN of the backend configuration entry and a message explaining the
   * problem that occurred.
   */
  public static final int MSGID_BACKUPDB_CANNOT_DETERMINE_BACKEND_ID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 273;



  /**
   * The message ID for the message that will be used as the description of the
   * backUpAll argument.  This does not take any arguments.
   */
  public static final int MSGID_BACKUPDB_DESCRIPTION_BACKUP_ALL =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 274;



  /**
   * The message ID for the message that will be used if both the backUpAll and
   * backendID arguments were used.  This takes two arguments, which are the
   * long identifiers for the backUpAll and backendID arguments.
   */
  public static final int MSGID_BACKUPDB_CANNOT_MIX_BACKUP_ALL_AND_BACKEND_ID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 275;



  /**
   * The message ID for the message that will be used if neither the backUpAll
   * nor the backendID arguments was used.  This takes two arguments, which are
   * the long identifiers for the backUpAll and backendID arguments.
   */
  public static final int MSGID_BACKUPDB_NEED_BACKUP_ALL_OR_BACKEND_ID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 276;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to create the backup directory structure.  This takes two arguments,
   * which are the path to the backup directory and a string representation of
   * the exception that was caught.
   */
  public static final int MSGID_BACKUPDB_CANNOT_CREATE_BACKUP_DIR =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 277;



  /**
   * The message ID for the message that will be used if a request is made to
   * back up a backend that does not provide such a mechanism.  This takes a
   * single argument, which is the backend ID of the target backend.
   */
  public static final int MSGID_BACKUPDB_BACKUP_NOT_SUPPORTED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_WARNING | 278;



  /**
   * The message ID for the message that will be used if none of the requested
   * backends support a backup mechanism.  This does not take any arguments.
   */
  public static final int MSGID_BACKUPDB_NO_BACKENDS_TO_ARCHIVE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_WARNING | 279;



  /**
   * The message ID for the message that will be used when starting the backup
   * process for a backend.  This takes a single argument, which is the backend
   * ID for that backend.
   */
  public static final int MSGID_BACKUPDB_STARTING_BACKUP =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_NOTICE | 280;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to parse a backup descriptor file.  This takes two arguments, which
   * are the path to the descriptor file and a message explaining the problem
   * that occurred.
   */
  public static final int MSGID_BACKUPDB_CANNOT_PARSE_BACKUP_DESCRIPTOR =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 281;



  /**
   * The message ID for the message that will be used when the backup process
   * is complete but one or more errors were encountered during processing.
   * This does not take any arguments.
   */
  public static final int MSGID_BACKUPDB_COMPLETED_WITH_ERRORS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_NOTICE | 282;



  /**
   * The message ID for the message that will be used when the backup process
   * completes without any errors.  This does not take any arguments.
   */
  public static final int MSGID_BACKUPDB_COMPLETED_SUCCESSFULLY =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_NOTICE | 283;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the crypto manager.  This takes a single argument, which is a
   * message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFEXPORT_CANNOT_INITIALIZE_CRYPTO_MANAGER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 284;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the crypto manager.  This takes a single argument, which is a
   * message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_INITIALIZE_CRYPTO_MANAGER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 285;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the crypto manager.  This takes a single argument, which is a
   * message explaining the problem that occurred.
   */
  public static final int MSGID_BACKUPDB_CANNOT_INITIALIZE_CRYPTO_MANAGER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 286;



  /**
   * The message ID for the message that will be used as the description of the
   * incrementalBaseID argument.  This does not take any arguments.
   */
  public static final int MSGID_BACKUPDB_DESCRIPTION_INCREMENTAL_BASE_ID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 287;



  /**
   * The message ID for the message that will be used if an incremental base ID
   * is specified for a full backup.  This takes two arguments, which are the
   * long identifiers for the incremental base ID and the incremental arguments.
   */
  public static final int MSGID_BACKUPDB_INCREMENTAL_BASE_REQUIRES_INCREMENTAL =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 288;



  /**
   * The message ID for the message that will be used as the description of the
   * configClass argument.  This does not take any arguments.
   */
  public static final int MSGID_RESTOREDB_DESCRIPTION_CONFIG_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 289;



  /**
   * The message ID for the message that will be used as the description of the
   * configFile argument.  This does not take any arguments.
   */
  public static final int MSGID_RESTOREDB_DESCRIPTION_CONFIG_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 290;



  /**
   * The message ID for the message that will be used as the description of the
   * backendID argument.  This does not take any arguments.
   */
  public static final int MSGID_RESTOREDB_DESCRIPTION_BACKEND_ID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 291;



  /**
   * The message ID for the message that will be used as the description of the
   * backupID argument.  This does not take any arguments.
   */
  public static final int MSGID_RESTOREDB_DESCRIPTION_BACKUP_ID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 292;



  /**
   * The message ID for the message that will be used as the description of the
   * backupDirectory argument.  This does not take any arguments.
   */
  public static final int MSGID_RESTOREDB_DESCRIPTION_BACKUP_DIR =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 293;



  /**
   * The message ID for the message that will be used as the description of the
   * listBackups argument.  This does not take any arguments.
   */
  public static final int MSGID_RESTOREDB_DESCRIPTION_LIST_BACKUPS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 294;



  /**
   * The message ID for the message that will be used as the description of the
   * verifyOnly argument.  This does not take any arguments.
   */
  public static final int MSGID_RESTOREDB_DESCRIPTION_VERIFY_ONLY =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 295;



  /**
   * The message ID for the message that will be used as the description of the
   * help argument.  This does not take any arguments.
   */
  public static final int MSGID_RESTOREDB_DESCRIPTION_USAGE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 296;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the command-line arguments.  This takes a single argument,
   * which is an explanation of the problem that occurred.
   */
  public static final int MSGID_RESTOREDB_CANNOT_INITIALIZE_ARGS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 297;



  /**
   * The message ID for the message that will be used if an error occurs while
   * parsing the command-line arguments.  This takes a single argument, which is
   * an explanation of the problem that occurred.
   */
  public static final int MSGID_RESTOREDB_ERROR_PARSING_ARGS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 298;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to bootstrap the Directory Server.  This takes a single argument,
   * which is a string representation of the exception that was caught.
   */
  public static final int MSGID_RESTOREDB_SERVER_BOOTSTRAP_ERROR =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 299;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying load the Directory Server configuration.  This takes a single
   * argument which is a message with information about the problem that
   * occurred.
   */
  public static final int MSGID_RESTOREDB_CANNOT_LOAD_CONFIG =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 300;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to load the Directory Server schema.  This takes a single argument,
   * which is a message with information about the problem that occurred.
   */
  public static final int MSGID_RESTOREDB_CANNOT_LOAD_SCHEMA =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 301;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to initialize the core Directory Server configuration.  This takes a
   * single argument, which is a message with information about the problem that
   * occurred.
   */
  public static final int MSGID_RESTOREDB_CANNOT_INITIALIZE_CORE_CONFIG =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 302;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the crypto manager.  This takes a single argument, which is a
   * message explaining the problem that occurred.
   */
  public static final int MSGID_RESTOREDB_CANNOT_INITIALIZE_CRYPTO_MANAGER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 303;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to parse the backup descriptor contained in the specified backup
   * directory.  This takes two arguments, which are the path to the backup
   * directory and a string representation of the exception that was caught.
   */
  public static final int MSGID_RESTOREDB_CANNOT_READ_BACKUP_DIRECTORY =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 304;



  /**
   * The message ID for the message that will be to display the backup ID when
   * obtaining a list of available backups in a given directory.  This takes a
   * single argument, which is the backup ID.
   */
  public static final int MSGID_RESTOREDB_LIST_BACKUP_ID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 305;



  /**
   * The message ID for the message that will be to display the backup date when
   * obtaining a list of available backups in a given directory.  This takes a
   * single argument, which is a string representation of the backup date.
   */
  public static final int MSGID_RESTOREDB_LIST_BACKUP_DATE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 306;



  /**
   * The message ID for the message that will be to indicate whether a backup is
   * incremental when obtaining a list of available backups in a given
   * directory.  This takes a single argument, which is a string representation
   * of whether the backup is incremental.
   */
  public static final int MSGID_RESTOREDB_LIST_INCREMENTAL =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 307;



  /**
   * The message ID for the message that will be to indicate whether a backup is
   * compressed when obtaining a list of available backups in a given
   * directory.  This takes a single argument, which is a string representation
   * of whether the backup is compressed.
   */
  public static final int MSGID_RESTOREDB_LIST_COMPRESSED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 308;



  /**
   * The message ID for the message that will be to indicate whether a backup is
   * encrypted when obtaining a list of available backups in a given
   * directory.  This takes a single argument, which is a string representation
   * of whether the backup is encrypted.
   */
  public static final int MSGID_RESTOREDB_LIST_ENCRYPTED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 309;



  /**
   * The message ID for the message that will be to indicate whether a backup is
   * hashed when obtaining a list of available backups in a given directory.
   * This takes a single argument, which is a string representation of whether
   * the backup is hashed.
   */
  public static final int MSGID_RESTOREDB_LIST_HASHED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 310;



  /**
   * The message ID for the message that will be to indicate whether a backup is
   * signed when obtaining a list of available backups in a given directory.
   * This takes a single argument, which is a string representation of whether
   * the backup is signed.
   */
  public static final int MSGID_RESTOREDB_LIST_SIGNED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 311;



  /**
   * The message ID for the message that will be to display the set of
   * dependencies when obtaining a list of available backups in a given
   * directory.  This takes a single argument, which is a comma-separated list
   * of the  dependencies for the backup.
   */
  public static final int MSGID_RESTOREDB_LIST_DEPENDENCIES =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 312;



  /**
   * The message ID for the message that will be used if the user requested a
   * backup ID that does not exist.  This takes two arguments, which are the
   * provided backup ID and the path to the backup directory.
   */
  public static final int MSGID_RESTOREDB_INVALID_BACKUP_ID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 313;



  /**
   * The message ID for the message that will be used if the specified backup
   * directory does not contain any backups.  This takes a single argument,
   * which is the path tot he backup directory.
   */
  public static final int MSGID_RESTOREDB_NO_BACKUPS_IN_DIRECTORY =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 314;



  /**
   * The message ID for the message that will be used if the backup directory
   * is associated with a backend that does not exist.  This takes two
   * arguments, which are the path to the backup directory and the DN of the
   * configuration entry for the backups contained in that directory.
   */
  public static final int MSGID_RESTOREDB_NO_BACKENDS_FOR_DN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 315;



  /**
   * The message ID for the message that will be used if the selected backend
   * does not support the ability to perform restore operations.  This takes a
   * single argument, which is the backend ID for the selected backend.
   */
  public static final int MSGID_RESTOREDB_CANNOT_RESTORE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 316;



  /**
   * The message ID for the message that will be used if an error occurred while
   * attempting to restore the backup.  This takes three arguments, which are
   * the backup ID, the path to the backup directory, and a message explaining
   * the problem that occurred.
   */
  public static final int MSGID_RESTOREDB_ERROR_DURING_BACKUP =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 317;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to decode the backend configuration base DN string.  This takes two
   * arguments, which are the backend config base DN string and a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_RESTOREDB_CANNOT_DECODE_BACKEND_BASE_DN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 318;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to retrieve the backend configuration base entry.  This takes two
   * arguments, which are the DN of the entry and a message explaining the
   * problem that occurred.
   */
  public static final int MSGID_RESTOREDB_CANNOT_RETRIEVE_BACKEND_BASE_ENTRY =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 319;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to determine the backend class name.  This takes two arguments,
   * which are the DN of the backend configuration entry and a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_RESTOREDB_CANNOT_DETERMINE_BACKEND_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 320;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to load a class for a Directory Server backend.  This takes three
   * arguments, which are the class name, the DN of the configuration entry, and
   * a message explaining the problem that occurred.
   */
  public static final int MSGID_RESTOREDB_CANNOT_LOAD_BACKEND_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 321;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to instantiate a class for a Directory Server backend.  This takes
   * three arguments, which are the class name, the DN of the configuration
   * entry, and a message explaining the problem that occurred.
   */
  public static final int MSGID_RESTOREDB_CANNOT_INSTANTIATE_BACKEND_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 322;



  /**
   * The message ID for the message that will be used if a backend configuration
   * entry does not define any base DNs.  This takes a single argument, which is
   * the DN of the backend configuration entry.
   */
  public static final int MSGID_RESTOREDB_NO_BASES_FOR_BACKEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 323;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to determine the set of base DNs for a backend.  This takes two
   * arguments, which are the DN of the backend configuration entry and a string
   * representation of the exception that was caught.
   */
  public static final int MSGID_RESTOREDB_CANNOT_DETERMINE_BASES_FOR_BACKEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 324;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to determine the backend ID.  This takes two arguments, which are
   * the DN of the backend configuration entry and a message explaining the
   * problem that occurred.
   */
  public static final int MSGID_RESTOREDB_CANNOT_DETERMINE_BACKEND_ID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 325;



  /**
   * The message ID for the message that will be used if the signHash option was
   * used without the hash option.  This takes two arguments, which are the
   * long identifiers for the signHash and the hash arguments.
   */
  public static final int MSGID_BACKUPDB_SIGN_REQUIRES_HASH =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 326;


  /**
   * The message ID for the message that will be used as the description of the
   * no-op argument.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_NOOP =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 327;


  /**
   * The message ID for the message that will be used if an error occurs while
   * attempting to acquire a lock for the backend to be archived.  This takes
   * two arguments, which are the backend ID and a message explaining the
   * problem that occurred.
   */
  public static final int MSGID_BACKUPDB_CANNOT_LOCK_BACKEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 328;



  /**
   * The message ID for the message that will be used if an error occurs while
   * attempting to release a lock for the backend to be archived.  This takes
   * two arguments, which are the backend ID and a message explaining the
   * problem that occurred.
   */
  public static final int MSGID_BACKUPDB_CANNOT_UNLOCK_BACKEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_WARNING | 329;



  /**
   * The message ID for the message that will be used if an error occurs while
   * attempting to acquire a lock for the backend to be restored.  This takes
   * two arguments, which are the backend ID and a message explaining the
   * problem that occurred.
   */
  public static final int MSGID_RESTOREDB_CANNOT_LOCK_BACKEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 330;



  /**
   * The message ID for the message that will be used if an error occurs while
   * attempting to release a lock for the backend to be restored.  This takes
   * two arguments, which are the backend ID and a message explaining the
   * problem that occurred.
   */
  public static final int MSGID_RESTOREDB_CANNOT_UNLOCK_BACKEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_WARNING | 331;



  /**
   * The message ID for the message that will be used if an error occurs while
   * attempting to acquire a lock for the backend to be imported.  This takes
   * two arguments, which are the backend ID and a message explaining the
   * problem that occurred.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_LOCK_BACKEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 332;



  /**
   * The message ID for the message that will be used if an error occurs while
   * attempting to release a lock for the backend to be imported.  This takes
   * two arguments, which are the backend ID and a message explaining the
   * problem that occurred.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_UNLOCK_BACKEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_WARNING | 333;



  /**
   * The message ID for the message that will be used if an error occurs while
   * attempting to acquire a lock for the backend to be exported.  This takes
   * two arguments, which are the backend ID and a message explaining the
   * problem that occurred.
   */
  public static final int MSGID_LDIFEXPORT_CANNOT_LOCK_BACKEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 334;



  /**
   * The message ID for the message that will be used if an error occurs while
   * attempting to release a lock for the backend to be exported.  This takes
   * two arguments, which are the backend ID and a message explaining the
   * problem that occurred.
   */
  public static final int MSGID_LDIFEXPORT_CANNOT_UNLOCK_BACKEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_WARNING | 335;



  /**
   * The message ID for the message that will be used if an error occurs while
   * attempting to acquire a lock for the backend to be verified.  This takes
   * two arguments, which are the backend ID and a message explaining the
   * problem that occurred.
   */
  public static final int MSGID_VERIFYINDEX_CANNOT_LOCK_BACKEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 336;



  /**
   * The message ID for the message that will be used if an error occurs while
   * attempting to release a lock for the backend to be verified.  This takes
   * two arguments, which are the backend ID and a message explaining the
   * problem that occurred.
   */
  public static final int MSGID_VERIFYINDEX_CANNOT_UNLOCK_BACKEND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_WARNING | 337;

  /**
   * The message ID for the message that will be used as the description of the
   * types only argument for the search results.  This does not take any
   * arguments.
   */
  public static final int MSGID_DESCRIPTION_TYPES_ONLY =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 338;



  /**
   * The message ID for the message that will be used as the description of the
   * skipSchemaValidation argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFIMPORT_DESCRIPTION_SKIP_SCHEMA_VALIDATION =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 339;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the server plugins.  This takes a single argument, which is a
   * message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFEXPORT_CANNOT_INITIALIZE_PLUGINS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 340;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the server plugins.  This takes a single argument, which is a
   * message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_INITIALIZE_PLUGINS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 341;



  /**
   * The message ID for the message that will be used as the description of the
   * assertionFilter option.  It does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_ASSERTION_FILTER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 342;



  /**
   * The message ID for the message that will be used if a request is made to
   * use the LDAP assertion control but the provided filter is invalid.  This
   * takes a single argument, which is a message explaining why the filter is
   * invalid.
   */
  public static final int MSGID_LDAP_ASSERTION_INVALID_FILTER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 343;



  /**
   * The message ID for the message that will be used as the description of the
   * assertionFilter option for the ldapsearch tool.  It does not take any
   * arguments.
   */
  public static final int MSGID_DESCRIPTION_SEARCH_ASSERTION_FILTER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 344;



  /**
   * The message ID for the message that will be used as the description of the
   * assertionFilter option for the ldapcompare tool.  It does not take any
   * arguments.
   */
  public static final int MSGID_DESCRIPTION_COMPARE_ASSERTION_FILTER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 345;



  /**
   * The message ID for the message that will be used as the description of the
   * preReadAttributes option.  It does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_PREREAD_ATTRS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 346;



  /**
   * The message ID for the message that will be used as the description of the
   * postReadAttributes option.  It does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_POSTREAD_ATTRS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 347;



  /**
   * The message ID for the message that will be used if the pre-read response
   * control does not have a value.  This does not take any arguments.
   */
  public static final int MSGID_LDAPMODIFY_PREREAD_NO_VALUE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 348;



  /**
   * The message ID for the message that will be used if the pre-read response
   * control value cannot be decoded.  This takes a single argument, which is a
   * message explaining the problem that occurred.
   */
  public static final int MSGID_LDAPMODIFY_PREREAD_CANNOT_DECODE_VALUE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 349;



  /**
   * The message ID for the message that will be used as the heading for the
   * entry displayed from the pre-read response control.  It does not take any
   * arguments.
   */
  public static final int MSGID_LDAPMODIFY_PREREAD_ENTRY =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 350;



  /**
   * The message ID for the message that will be used if the post-read response
   * control does not have a value.  This does not take any arguments.
   */
  public static final int MSGID_LDAPMODIFY_POSTREAD_NO_VALUE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 351;



  /**
   * The message ID for the message that will be used if the post-read response
   * control value cannot be decoded.  This takes a single argument, which is a
   * message explaining the problem that occurred.
   */
  public static final int MSGID_LDAPMODIFY_POSTREAD_CANNOT_DECODE_VALUE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 352;



  /**
   * The message ID for the message that will be used as the heading for the
   * entry displayed from the post-read response control.  It does not take any
   * arguments.
   */
  public static final int MSGID_LDAPMODIFY_POSTREAD_ENTRY =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 353;



  /**
   * The message ID for the message that will be used as the description for the
   * command-line option that includes the proxied authorization control in the
   * request.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_PROXY_AUTHZID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 354;



  /**
   * The message ID for the message that will be used as the description for the
   * command-line option that includes the persistent search control in the
   * request.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_PSEARCH_INFO =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 355;



  /**
   * The message ID for the message that will be used if a request is made to
   * use the persistent search control but the descriptor string is empty.  This
   * does not take any arguments.
   */
  public static final int MSGID_PSEARCH_MISSING_DESCRIPTOR =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 356;



  /**
   * The message ID for the message that will be used if a request is made to
   * use the persistent search control but the descriptor string does not start
   * with "ps".  This takes a single argument, which is the provided descriptor
   * string.
   */
  public static final int MSGID_PSEARCH_DOESNT_START_WITH_PS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 357;



  /**
   * The message ID for the message that will be used if the persistent search
   * control descriptor contains an invalid change type.  This takes a single
   * argument, which is the invalid change type.
   */
  public static final int MSGID_PSEARCH_INVALID_CHANGE_TYPE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 358;



  /**
   * The message ID for the message that will be used if the persistent search
   * control descriptor contains an invalid changesOnly value.  This takes a
   * single argument, which is the invalid changesOnly value.
   */
  public static final int MSGID_PSEARCH_INVALID_CHANGESONLY =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 359;



  /**
   * The message ID for the message that will be used if the persistent search
   * control descriptor contains an invalid returnECs value.  This takes a
   * single argument, which is the invalid returnECs value.
   */
  public static final int MSGID_PSEARCH_INVALID_RETURN_ECS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 360;



  /**
   * The message ID for the message that will be used as the description for the
   * command-line option that requests that the authzID be included in the bind
   * response.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_REPORT_AUTHZID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 361;



  /**
   * The message ID for the message that will be used to report the
   * authorization ID included in the bind response to the user.  This takes a
   * single argument, which is the authorization ID.
   */
  public static final int MSGID_BIND_AUTHZID_RETURNED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 362;

  /**
   * The message ID for the message that will be used as the description of the
   * filename argument.  This does not take any arguments.
   */
  public static final int MSGID_SEARCH_DESCRIPTION_FILENAME =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 363;



  /**
   * The message ID for the message that will be used as the description of the
   * matchedValuesFilter option for the ldapsearch tool.  It does not take any
   * arguments.
   */
  public static final int MSGID_DESCRIPTION_MATCHED_VALUES_FILTER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 364;



  /**
   * The message ID for the message that will be used if a request is made to
   * use the matched values but the provided filter is invalid.  This takes a
   * single argument, which is a message explaining why the filter is invalid.
   */
  public static final int MSGID_LDAP_MATCHEDVALUES_INVALID_FILTER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 365;



  /**
   * The message ID for the message that will be used if the modify
   * tool cannot open the LDIF file for reading.  This takes two
   * arguments, which are the path to the LDIF file and a string
   * representation of the exception that was caught.
   */
  public static final int MSGID_LDIF_FILE_CANNOT_OPEN_FOR_READ =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_FATAL_ERROR | 366;



  /**
   * The message ID for the message that will be used if an I/O error occurs
   * while attempting to read from the LDIF file.  This takes two
   * arguments, which are the path to the LDIF file and a string
   * representation of the exception that was caught.
   */
  public static final int MSGID_LDIF_FILE_READ_ERROR =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_FATAL_ERROR | 367;



  /**
   * The message ID for the message that will be used if an entry in the
   * LDIF file cannot be parsed as a valid LDIF entry.  This takes
   * three arguments, which are the approximate line number in the LDIF file,
   * the path to the LDIF file, and a string representation of the exception
   * that was caught.
   */
  public static final int MSGID_LDIF_FILE_INVALID_LDIF_ENTRY =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 368;



  /**
   * The message ID for the message that will be used as the description of the
   * authPasswordSyntax argument.  This does not take any arguments.
   */
  public static final int MSGID_ENCPW_DESCRIPTION_AUTHPW =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 369;



  /**
   * The message ID for the message that will be used if no auth password
   * storage schemes have been defined in the Directory Server.  This does not
   * take any arguments.
   */
  public static final int MSGID_ENCPW_NO_AUTH_STORAGE_SCHEMES =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 370;



  /**
   * The message ID for the message that will be used if the requested auth
   * password storage scheme is not configured for use in the Directory Server.
   * This takes a single argument, which is the name of the requested storage
   * scheme.
   */
  public static final int MSGID_ENCPW_NO_SUCH_AUTH_SCHEME =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 371;



  /**
   * The message ID for the message that will be used if the encoded password is
   * not valid according to the auth password syntax.  This takes a single
   * argument, which is a message explaining why it is invalid.
   */
  public static final int MSGID_ENCPW_INVALID_ENCODED_AUTHPW =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 372;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the password policy components.  This takes a single argument,
   * which is a message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFIMPORT_CANNOT_INITIALIZE_PWPOLICY =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 373;



  /**
   * The message ID for the message that will be used as the description of the
   * host argument.  It does not take any arguments.
   */
  public static final int MSGID_STOPDS_DESCRIPTION_HOST =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 374;



  /**
   * The message ID for the message that will be used as the description of the
   * port argument.  It does not take any arguments.
   */
  public static final int MSGID_STOPDS_DESCRIPTION_PORT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 375;



  /**
   * The message ID for the message that will be used as the description of the
   * useSSL argument.  It does not take any arguments.
   */
  public static final int MSGID_STOPDS_DESCRIPTION_USESSL =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 376;



  /**
   * The message ID for the message that will be used as the description of the
   * useStartTLS argument.  It does not take any arguments.
   */
  public static final int MSGID_STOPDS_DESCRIPTION_USESTARTTLS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 377;



  /**
   * The message ID for the message that will be used as the description of the
   * bindDN argument.  It does not take any arguments.
   */
  public static final int MSGID_STOPDS_DESCRIPTION_BINDDN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 378;



  /**
   * The message ID for the message that will be used as the description of the
   * bindPassword argument.  It does not take any arguments.
   */
  public static final int MSGID_STOPDS_DESCRIPTION_BINDPW =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 379;



  /**
   * The message ID for the message that will be used as the description of the
   * bindPasswordFile argument.  It does not take any arguments.
   */
  public static final int MSGID_STOPDS_DESCRIPTION_BINDPWFILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 380;



  /**
   * The message ID for the message that will be used as the description of the
   * saslOption argument.  It does not take any arguments.
   */
  public static final int MSGID_STOPDS_DESCRIPTION_SASLOPTIONS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 381;



  /**
   * The message ID for the message that will be used as the description of the
   * proxyAuthZID argument.  It does not take any arguments.
   */
  public static final int MSGID_STOPDS_DESCRIPTION_PROXYAUTHZID =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 382;



  /**
   * The message ID for the message that will be used as the description of the
   * stopReason argument.  It does not take any arguments.
   */
  public static final int MSGID_STOPDS_DESCRIPTION_STOP_REASON =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 383;



  /**
   * The message ID for the message that will be used as the description of the
   * stopTime argument.  It does not take any arguments.
   */
  public static final int MSGID_STOPDS_DESCRIPTION_STOP_TIME =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 384;



  /**
   * The message ID for the message that will be used as the description of the
   * trustAll argument.  It does not take any arguments.
   */
  public static final int MSGID_STOPDS_DESCRIPTION_TRUST_ALL =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 385;



  /**
   * The message ID for the message that will be used as the description of the
   * keyStoreFile argument.  It does not take any arguments.
   */
  public static final int MSGID_STOPDS_DESCRIPTION_KSFILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 386;



  /**
   * The message ID for the message that will be used as the description of the
   * keyStorePassword argument.  It does not take any arguments.
   */
  public static final int MSGID_STOPDS_DESCRIPTION_KSPW =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 387;



  /**
   * The message ID for the message that will be used as the description of the
   * keyStorePasswordFile argument.  It does not take any arguments.
   */
  public static final int MSGID_STOPDS_DESCRIPTION_KSPWFILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 388;



  /**
   * The message ID for the message that will be used as the description of the
   * trustStoreFile argument.  It does not take any arguments.
   */
  public static final int MSGID_STOPDS_DESCRIPTION_TSFILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 389;



  /**
   * The message ID for the message that will be used as the description of the
   * trustStorePassword argument.  It does not take any arguments.
   */
  public static final int MSGID_STOPDS_DESCRIPTION_TSPW =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 390;



  /**
   * The message ID for the message that will be used as the description of the
   * trustStorePasswordFile argument.  It does not take any arguments.
   */
  public static final int MSGID_STOPDS_DESCRIPTION_TSPWFILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 391;



  /**
   * The message ID for the message that will be used as the description of the
   * help argument.  It does not take any arguments.
   */
  public static final int MSGID_STOPDS_DESCRIPTION_SHOWUSAGE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 392;



  /**
   * The message ID for the message that will be used if an error occurs while
   * attempting to initialize the command-line argument parser.  This takes a
   * single argument, which is a message explaining the problem that occurred.
   */
  public static final int MSGID_STOPDS_CANNOT_INITIALIZE_ARGS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 393;



  /**
   * The message ID for the message that will be used if an error occurs while
   * parsing the provided command-line arguments.  This takes a single argument,
   * which is a message explaining the problem that occurred.
   */
  public static final int MSGID_STOPDS_ERROR_PARSING_ARGS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 394;



  /**
   * The message ID for the message that will be used if two arguments that are
   * mutually exclusive were both provided.  This takes two arguments, which are
   * the long identifiers for the mutually-exclusive command line arguments.
   */
  public static final int MSGID_STOPDS_MUTUALLY_EXCLUSIVE_ARGUMENTS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 395;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to parse the provided stop time.  This does not take any arguments.
   */
  public static final int MSGID_STOPDS_CANNOT_DECODE_STOP_TIME =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 396;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to perform SSL initialization.  This takes a single argument, which
   * is a message explaining the problem that occurred.
   */
  public static final int MSGID_STOPDS_CANNOT_INITIALIZE_SSL =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 397;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to parse a SASL option string.  This takes a single argument, which
   * is the SASL option string.
   */
  public static final int MSGID_STOPDS_CANNOT_PARSE_SASL_OPTION =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 398;



  /**
   * The message ID for the message that will be used if SASL options were used
   * without specifying the SASL mechanism.  This does not take any arguments.
   */
  public static final int MSGID_STOPDS_NO_SASL_MECHANISM =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 399;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to determine the port to use to communicate with the Directory
   * Server.  This takes two arguments, which are the name of the port argument
   * and a message explaining the problem that occurred.
   */
  public static final int MSGID_STOPDS_CANNOT_DETERMINE_PORT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 400;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to connect to the Directory Server.  This takes a single argument,
   * which is a message explaining the problem that occurred.
   */
  public static final int MSGID_STOPDS_CANNOT_CONNECT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 401;



  /**
   * The message ID for the message that will be used if the connection is
   * closed while waiting for the response from the Directory Server.  This does
   * not take any arguments.
   */
  public static final int MSGID_STOPDS_UNEXPECTED_CONNECTION_CLOSURE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 402;



  /**
   * The message ID for the message that will be used if an I/O error occurs
   * while communicating with the Directory Server.  This takes a single
   * argument, which is a message explaining the problem that occurred.
   */
  public static final int MSGID_STOPDS_IO_ERROR =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 403;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to decode the response from the Directory Server.  This takes a
   * single  argument, which is a message explaining the problem that occurred.
   */
  public static final int MSGID_STOPDS_DECODE_ERROR =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 404;



  /**
   * The message ID for the message that will be used if an unexpected response
   * type was received for the add request.  This takes a single argument, which
   * is the name of the response type that was received.
   */
  public static final int MSGID_STOPDS_INVALID_RESPONSE_TYPE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 405;



  /**
   * The message ID for the message that will be used to indicate that the
   * user's password is expired.  This does not take any arguments.
   */
  public static final int MSGID_BIND_PASSWORD_EXPIRED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 406;



  /**
   * The message ID for the message that will be used to indicate that the
   * user's password will expire in the near future.  This takes a single
   * argument, which indicates the length of time until the password is actually
   * expired.
   */
  public static final int MSGID_BIND_PASSWORD_EXPIRING =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 407;



  /**
   * The message ID for the message that will be used to indicate that the
   * user's account has been locked.  This does not take any arguments.
   */
  public static final int MSGID_BIND_ACCOUNT_LOCKED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 408;



  /**
   * The message ID for the message that will be used to indicate that the
   * user's password must be changed before any other operations will be
   * allowed.  This does not take any arguments.
   */
  public static final int MSGID_BIND_MUST_CHANGE_PASSWORD =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 409;



  /**
   * The message ID for the message that will be used to specify the number of
   * grace logins that the user has left.  This takes a single argument, which
   * is the number of grace logins remaining.
   */
  public static final int MSGID_BIND_GRACE_LOGINS_REMAINING =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 410;



  /**
   * The message ID for the message that will be used as the description for the
   * command-line option that requests that the password policy control be used
   * in the bind operation.  This does not take any arguments.
   */
  public static final int MSGID_DESCRIPTION_USE_PWP_CONTROL =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 411;



  /**
   * The message ID for the message that will be used as the description of the
   * restart argument.  It does not take any arguments.
   */
  public static final int MSGID_STOPDS_DESCRIPTION_RESTART =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 412;


  /**
   * The message ID for the message that will be used as the description of the
   * filename argument.  This does not take any arguments.
   */
  public static final int MSGID_COMPARE_DESCRIPTION_FILENAME =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 413;


  /**
   * The message ID for the message that will be used as the description of the
   * ldifFile argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFSEARCH_DESCRIPTION_LDIF_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 414;


  /**
   * The message ID for the message that will be used as the description of the
   * baseDN argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFSEARCH_DESCRIPTION_BASEDN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 415;


  /**
   * The message ID for the message that will be used as the description of the
   * scope argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFSEARCH_DESCRIPTION_SCOPE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 416;


  /**
   * The message ID for the message that will be used as the description of the
   * configFile argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFSEARCH_DESCRIPTION_CONFIG_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 417;


  /**
   * The message ID for the message that will be used as the description of the
   * configClass argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFSEARCH_DESCRIPTION_CONFIG_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 418;


  /**
   * The message ID for the message that will be used as the description of the
   * filterFile argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFSEARCH_DESCRIPTION_FILTER_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 419;


  /**
   * The message ID for the message that will be used as the description of the
   * outputFile argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFSEARCH_DESCRIPTION_OUTPUT_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 420;


  /**
   * The message ID for the message that will be used as the description of the
   * overwriteExisting argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFSEARCH_DESCRIPTION_OVERWRITE_EXISTING =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 421;


  /**
   * The message ID for the message that will be used as the description of the
   * dontWrap argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFSEARCH_DESCRIPTION_DONT_WRAP =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 422;


  /**
   * The message ID for the message that will be used as the description of the
   * sizeLimit argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFSEARCH_DESCRIPTION_SIZE_LIMIT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 423;


  /**
   * The message ID for the message that will be used as the description of the
   * timeLimit argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFSEARCH_DESCRIPTION_TIME_LIMIT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 424;


  /**
   * The message ID for the message that will be used as the description of the
   * help argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFSEARCH_DESCRIPTION_USAGE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 425;



  /**
   * The message ID for the message that will be used if an error occurs while
   * attempting to initialize the command-line argument parser.  This takes a
   * single argument, which is a message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFSEARCH_CANNOT_INITIALIZE_ARGS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 426;



  /**
   * The message ID for the message that will be used if an error occurs while
   * parsing the provided command-line arguments.  This takes a single argument,
   * which is a message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFSEARCH_ERROR_PARSING_ARGS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 427;



  /**
   * The message ID for the message that will be used if no filter file or
   * single filter was provided.  This does not take any arguments.
   */
  public static final int MSGID_LDIFSEARCH_NO_FILTER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 428;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the Directory Server configuration.  This takes two
   * arguments, which are the path to the Directory Server configuration file
   * and a message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFSEARCH_CANNOT_INITIALIZE_CONFIG =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 429;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the Directory Server schema.  This takes two arguments, which
   * are the path to the Directory Server configuration file and a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_LDIFSEARCH_CANNOT_INITIALIZE_SCHEMA =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 430;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to parse a search filter.  This takes two arguments, which are the
   * provided filter string and a message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFSEARCH_CANNOT_PARSE_FILTER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 431;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to parse a base DN.  This takes two arguments, which are the
   * provided base DN string and a message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFSEARCH_CANNOT_PARSE_BASE_DN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 432;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to parse the time limit.  This takes a single argument, which is a
   * message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFSEARCH_CANNOT_PARSE_TIME_LIMIT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 433;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to parse the size limit.  This takes a single argument, which is a
   * message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFSEARCH_CANNOT_PARSE_SIZE_LIMIT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 434;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to create the LDIF reader.  This takes a single argument, which is
   * a message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFSEARCH_CANNOT_CREATE_READER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 435;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to create the LDIF writer.  This takes a single argument, which is
   * a message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFSEARCH_CANNOT_CREATE_WRITER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 436;



  /**
   * The message ID for the message that will be used if the configured time
   * limit has been exceeded.  This does not take any arguments.
   */
  public static final int MSGID_LDIFSEARCH_TIME_LIMIT_EXCEEDED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_WARNING | 437;



  /**
   * The message ID for the message that will be used if the configured size
   * limit has been exceeded.  This does not take any arguments.
   */
  public static final int MSGID_LDIFSEARCH_SIZE_LIMIT_EXCEEDED =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_WARNING | 438;



  /**
   * The message ID for the message that will be used if a recoverable error
   * occurs while attempting to read an entry.  This takes a single argument,
   * which is a message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFSEARCH_CANNOT_READ_ENTRY_RECOVERABLE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 439;



  /**
   * The message ID for the message that will be used if a fatal error occurs
   * while attempting to read an entry.  This takes a single argument, which is
   * a message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFSEARCH_CANNOT_READ_ENTRY_FATAL =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 440;



  /**
   * The message ID for the message that will be used if an error occurs during
   * search processing.  This takes a single argument, which is a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_LDIFSEARCH_ERROR_DURING_PROCESSING =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 441;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the Directory Server JMX subsystem.  This takes two arguments,
   * which are the path to the Directory Server configuration file and a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_LDIFSEARCH_CANNOT_INITIALIZE_JMX =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 442;


  /**
   * The message ID for the message that will be used as the description of the
   * sourceLDIF argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFDIFF_DESCRIPTION_SOURCE_LDIF =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 443;


  /**
   * The message ID for the message that will be used as the description of the
   * targetLDIF argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFDIFF_DESCRIPTION_TARGET_LDIF =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 444;


  /**
   * The message ID for the message that will be used as the description of the
   * outputLDIF argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFDIFF_DESCRIPTION_OUTPUT_LDIF =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 445;


  /**
   * The message ID for the message that will be used as the description of the
   * overwriteExisting argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFDIFF_DESCRIPTION_OVERWRITE_EXISTING =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 446;


  /**
   * The message ID for the message that will be used as the description of the
   * configFile argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFDIFF_DESCRIPTION_CONFIG_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 447;


  /**
   * The message ID for the message that will be used as the description of the
   * configClass argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFDIFF_DESCRIPTION_CONFIG_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 448;


  /**
   * The message ID for the message that will be used as the description of the
   * help argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFDIFF_DESCRIPTION_USAGE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 449;



  /**
   * The message ID for the message that will be used if an error occurs while
   * attempting to initialize the command-line argument parser.  This takes a
   * single argument, which is a message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFDIFF_CANNOT_INITIALIZE_ARGS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 450;



  /**
   * The message ID for the message that will be used if an error occurs while
   * parsing the provided command-line arguments.  This takes a single argument,
   * which is a message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFDIFF_ERROR_PARSING_ARGS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 451;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the Directory Server JMX subsystem.  This takes two arguments,
   * which are the path to the Directory Server configuration file and a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_LDIFDIFF_CANNOT_INITIALIZE_JMX =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 452;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the Directory Server configuration.  This takes two
   * arguments, which are the path to the Directory Server configuration file
   * and a message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFDIFF_CANNOT_INITIALIZE_CONFIG =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 453;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the Directory Server schema.  This takes two arguments, which
   * are the path to the Directory Server configuration file and a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_LDIFDIFF_CANNOT_INITIALIZE_SCHEMA =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 454;



  /**
   * The message ID for the message that will be used if an error occurs while
   * opening the source LDIF file.  This takes two arguments, which are the name
   * of the LDIF file and a message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFDIFF_CANNOT_OPEN_SOURCE_LDIF =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 455;



  /**
   * The message ID for the message that will be used if an error occurs while
   * reading the source LDIF file.  This takes two arguments, which are the name
   * of the LDIF file and a message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFDIFF_ERROR_READING_SOURCE_LDIF =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 456;



  /**
   * The message ID for the message that will be used if an error occurs while
   * opening the target LDIF file.  This takes two arguments, which are the name
   * of the LDIF file and a message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFDIFF_CANNOT_OPEN_TARGET_LDIF =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 457;



  /**
   * The message ID for the message that will be used if an error occurs while
   * reading the target LDIF file.  This takes two arguments, which are the name
   * of the LDIF file and a message explaining the problem that occurred.
   */
  public static final int MSGID_LDIFDIFF_ERROR_READING_TARGET_LDIF =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 458;



  /**
   * The message ID for the message that will be used if an error occurs while
   * opening the LDIF writer.  This takes a single argument, which is a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_LDIFDIFF_CANNOT_OPEN_OUTPUT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 459;



  /**
   * The message ID for the message that will be used if no differences are
   * detected between the source and target LDIF files.  This does not take any
   * arguments.
   */
  public static final int MSGID_LDIFDIFF_NO_DIFFERENCES =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 460;



  /**
   * The message ID for the message that will be used if an error occurs while
   * writing diff information.  This takes single argument, which is a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_LDIFDIFF_ERROR_WRITING_OUTPUT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 461;


  /**
   * The message ID for the message that will be used as the description of the
   * configFile argument.  This does not take any arguments.
   */
  public static final int MSGID_CONFIGDS_DESCRIPTION_CONFIG_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 462;


  /**
   * The message ID for the message that will be used as the description of the
   * configClass argument.  This does not take any arguments.
   */
  public static final int MSGID_CONFIGDS_DESCRIPTION_CONFIG_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 463;


  /**
   * The message ID for the message that will be used as the description of the
   * ldapPort argument.  This does not take any arguments.
   */
  public static final int MSGID_CONFIGDS_DESCRIPTION_LDAP_PORT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 464;


  /**
   * The message ID for the message that will be used as the description of the
   * baseDN argument.  This does not take any arguments.
   */
  public static final int MSGID_CONFIGDS_DESCRIPTION_BASE_DN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 465;


  /**
   * The message ID for the message that will be used as the description of the
   * rootDN argument.  This does not take any arguments.
   */
  public static final int MSGID_CONFIGDS_DESCRIPTION_ROOT_DN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 466;


  /**
   * The message ID for the message that will be used as the description of the
   * rootPassword argument.  This does not take any arguments.
   */
  public static final int MSGID_CONFIGDS_DESCRIPTION_ROOT_PW =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 467;


  /**
   * The message ID for the message that will be used as the description of the
   * rootPasswordFile argument.  This does not take any arguments.
   */
  public static final int MSGID_CONFIGDS_DESCRIPTION_ROOT_PW_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 468;


  /**
   * The message ID for the message that will be used as the description of the
   * help argument.  This does not take any arguments.
   */
  public static final int MSGID_CONFIGDS_DESCRIPTION_USAGE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 469;



  /**
   * The message ID for the message that will be used if an error occurs while
   * attempting to initialize the command-line argument parser.  This takes a
   * single argument, which is a message explaining the problem that occurred.
   */
  public static final int MSGID_CONFIGDS_CANNOT_INITIALIZE_ARGS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 470;



  /**
   * The message ID for the message that will be used if an error occurs while
   * parsing the provided command-line arguments.  This takes a single argument,
   * which is a message explaining the problem that occurred.
   */
  public static final int MSGID_CONFIGDS_ERROR_PARSING_ARGS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 471;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to acquire an exclusive lock for the Directory Server process.  This
   * takes two argments, which are the path to the lock file and a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_CONFIGDS_CANNOT_ACQUIRE_SERVER_LOCK =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 472;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the Directory Server JMX subsystem.  This takes two arguments,
   * which are the path to the Directory Server configuration file and a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_CONFIGDS_CANNOT_INITIALIZE_JMX =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 473;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the Directory Server configuration.  This takes two
   * arguments, which are the path to the Directory Server configuration file
   * and a message explaining the problem that occurred.
   */
  public static final int MSGID_CONFIGDS_CANNOT_INITIALIZE_CONFIG =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 474;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the Directory Server schema.  This takes two arguments, which
   * are the path to the Directory Server configuration file and a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_CONFIGDS_CANNOT_INITIALIZE_SCHEMA =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 475;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to parse a base DN.  This takes two arguments, which are the base DN
   * string and a message explaining the problem that occurred.
   */
  public static final int MSGID_CONFIGDS_CANNOT_PARSE_BASE_DN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 476;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to parse the root DN.  This takes two arguments, which are the root
   * DN string and a message explaining the problem that occurred.
   */
  public static final int MSGID_CONFIGDS_CANNOT_PARSE_ROOT_DN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 477;



  /**
   * The message ID for the message that will be used if a root DN is provided
   * without giving a root password.  This does not take any arguments.
   */
  public static final int MSGID_CONFIGDS_NO_ROOT_PW =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 478;



  /**
   * The message ID for the message that will be used if an error occurs while
   * attempting to update the base DNs.  This takes a single argument, which is
   * a message explaining the problem that occurred.
   */
  public static final int MSGID_CONFIGDS_CANNOT_UPDATE_BASE_DN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 479;



  /**
   * The message ID for the message that will be used if an error occurs while
   * attempting to update the LDAP port.  This takes a single argument, which is
   * a message explaining the problem that occurred.
   */
  public static final int MSGID_CONFIGDS_CANNOT_UPDATE_LDAP_PORT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 480;



  /**
   * The message ID for the message that will be used if an error occurs while
   * attempting to update the root user entry.  This takes a single argument,
   * which is a message explaining the problem that occurred.
   */
  public static final int MSGID_CONFIGDS_CANNOT_UPDATE_ROOT_USER =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 481;



  /**
   * The message ID for the message that will be used if an error occurs while
   * attempting to write the updated configuration.  This takes a single
   * argument, which is a message explaining the problem that occurred.
   */
  public static final int MSGID_CONFIGDS_CANNOT_WRITE_UPDATED_CONFIG =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 482;



  /**
   * The message ID for the message that will be used if no configuration
   * changes were requested.  This does not take any arguments.
   */
  public static final int MSGID_CONFIGDS_NO_CONFIG_CHANGES =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 483;



  /**
   * The message ID for the message that will be used to indicate that the
   * updated configuration has been written.  This does not take any arguments.
   */
  public static final int MSGID_CONFIGDS_WROTE_UPDATED_CONFIG =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 484;



  /**
   * The message ID for the message that will be used as the description for the
   * testOnly argument.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_DESCRIPTION_TESTONLY =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 485;



  /**
   * The message ID for the message that will be used as the description for the
   * programName argument.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_DESCRIPTION_PROGNAME =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 486;



  /**
   * The message ID for the message that will be used as the description for the
   * configFile argument.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_DESCRIPTION_CONFIG_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 487;



  /**
   * The message ID for the message that will be used as the description for the
   * configClass argument.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_DESCRIPTION_CONFIG_CLASS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 488;



  /**
   * The message ID for the message that will be used as the description for the
   * silentInstall argument.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_DESCRIPTION_SILENT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 489;



  /**
   * The message ID for the message that will be used as the description for the
   * baseDN argument.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_DESCRIPTION_BASEDN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 490;



  /**
   * The message ID for the message that will be used as the description for the
   * addBaseEntry argument.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_DESCRIPTION_ADDBASE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 491;



  /**
   * The message ID for the message that will be used as the description for the
   * importLDIF argument.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_DESCRIPTION_IMPORTLDIF =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 492;



  /**
   * The message ID for the message that will be used as the description for the
   * ldapPort argument.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_DESCRIPTION_LDAPPORT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 493;



  /**
   * The message ID for the message that will be used as the description for the
   * skipPortCheck argument.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_DESCRIPTION_SKIPPORT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 494;



  /**
   * The message ID for the message that will be used as the description for the
   * rootDN argument.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_DESCRIPTION_ROOTDN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 495;



  /**
   * The message ID for the message that will be used as the description for the
   * rootPassword argument.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_DESCRIPTION_ROOTPW =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 496;



  /**
   * The message ID for the message that will be used as the description for the
   * rootPasswordFile argument.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_DESCRIPTION_ROOTPWFILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 497;



  /**
   * The message ID for the message that will be used as the description for the
   * help argument.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_DESCRIPTION_HELP =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 498;



  /**
   * The message ID for the message that will be used if the user did not
   * specify the path to the Directory Server configuration file.  This takes a
   * single argument, which is the name of the command-line option that should
   * be used to provide that information.
   */
  public static final int MSGID_INSTALLDS_NO_CONFIG_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 499;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the Directory Server JMX subsystem.  This takes two arguments,
   * which are the path to the Directory Server configuration file and a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_INSTALLDS_CANNOT_INITIALIZE_JMX =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 500;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the Directory Server configuration.  This takes two
   * arguments, which are the path to the Directory Server configuration file
   * and a message explaining the problem that occurred.
   */
  public static final int MSGID_INSTALLDS_CANNOT_INITIALIZE_CONFIG =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 501;



  /**
   * The message ID for the message that will be used if an error occurs while
   * initializing the Directory Server schema.  This takes two arguments, which
   * are the path to the Directory Server configuration file and a message
   * explaining the problem that occurred.
   */
  public static final int MSGID_INSTALLDS_CANNOT_INITIALIZE_SCHEMA =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 502;



  /**
   * The message ID for the message that will be used if an error occurs while
   * trying to parse a string as a DN.  This takes two arguments, which are the
   * DN string and a message explaining the problem that occurred.
   */
  public static final int MSGID_INSTALLDS_CANNOT_PARSE_DN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 503;



  /**
   * The message ID for the message that will be used as the prompt to provide
   * the directory base DN.  It does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_PROMPT_BASEDN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 504;



  /**
   * The message ID for the message that will be used as the prompt to determine
   * whether to import data from LDIF.  It does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_PROMPT_IMPORT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 505;



  /**
   * The message ID for the message that will be used as the prompt to provide
   * the path to the LDIF file to import.  It does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_PROMPT_IMPORT_FILE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 506;



  /**
   * The message ID for the message that will be used if two conflicting
   * arguments were provided to the program.  This takes two arguments, which
   * are the long forms of the conflicting arguments.
   */
  public static final int MSGID_INSTALLDS_TWO_CONFLICTING_ARGUMENTS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 507;



  /**
   * The message ID for the message that will be used as the prompt to determine
   * whether to add the base entry.  It does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_PROMPT_ADDBASE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 508;



  /**
   * The message ID for the message that will be used as the prompt to determine
   * the LDAP port number to use.  It does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_PROMPT_LDAPPORT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 509;



  /**
   * The message ID for the message that will be used if an error occurs while
   * attempting to bind to a privileged port.  This takes two arguments, which
   * are the port number and a message explaining the problem that occurred.
   */
  public static final int MSGID_INSTALLDS_CANNOT_BIND_TO_PRIVILEGED_PORT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 510;



  /**
   * The message ID for the message that will be used if an error occurs while
   * attempting to bind to a non-privileged port.  This takes two arguments,
   * which are the port number and a message explaining the problem that
   * occurred.
   */
  public static final int MSGID_INSTALLDS_CANNOT_BIND_TO_PORT =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 511;



  /**
   * The message ID for the message that will be used as the prompt to determine
   * the initial root DN.  It does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_PROMPT_ROOT_DN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 512;



  /**
   * The message ID for the message that will be used if no root password was
   * provided when performing a silent installation.  This takes two arguments,
   * which are the long forms of the root password and root password file
   * arguments.
   */
  public static final int MSGID_INSTALLDS_NO_ROOT_PASSWORD =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 513;



  /**
   * The message ID for the message that will be used as the prompt to request
   * the initial root password for the first time.  This does not take any
   * arguments.
   */
  public static final int MSGID_INSTALLDS_PROMPT_ROOT_PASSWORD =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 514;



  /**
   * The message ID for the message that will be used as the prompt to confirm
   * the initial root password.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_PROMPT_CONFIRM_ROOT_PASSWORD =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 515;



  /**
   * The message ID for the message that will be used to indicate that the
   * server configuration is being updated.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_STATUS_CONFIGURING_DS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 516;



  /**
   * The message ID for the message that will be used to indicate that the
   * base LDIF file is being created.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_STATUS_CREATING_BASE_LDIF =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 517;



  /**
   * The message ID for the message that will be used if an error occurs while
   * creating the base LDIF file.  This takes a single argument, which is a
   * message explaining the problem that occurred.
   */
  public static final int MSGID_INSTALLDS_CANNOT_CREATE_BASE_ENTRY_LDIF =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_SEVERE_ERROR | 518;



  /**
   * The message ID for the message that will be used to indicate that the
   * LDIF data is being imported.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_STATUS_IMPORTING_LDIF =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 519;



  /**
   * The message ID for the message that will be used to indicate that the setup
   * process was successful.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_STATUS_SUCCESS =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 520;



  /**
   * The message ID for the message that will be used as the prompt value for
   * Boolean "true" or "yes" values.
   */
  public static final int MSGID_INSTALLDS_PROMPT_VALUE_YES =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 521;



  /**
   * The message ID for the message that will be used as the prompt value for
   * Boolean "false" or "no" values.
   */
  public static final int MSGID_INSTALLDS_PROMPT_VALUE_NO =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 522;



  /**
   * The message ID for the message that will be used to indicate that the
   * Boolean value could not be interpreted.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_INVALID_YESNO_RESPONSE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 523;



  /**
   * The message ID for the message that will be used to indicate that the
   * response value could not be interpreted as an integer.  This does not take
   * any arguments.
   */
  public static final int MSGID_INSTALLDS_INVALID_INTEGER_RESPONSE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 524;



  /**
   * The message ID for the message that will be used to indicate that the
   * provided integer value was below the lower bound.  This takes a single
   * argument, which is the lower bound.
   */
  public static final int MSGID_INSTALLDS_INTEGER_BELOW_LOWER_BOUND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 525;



  /**
   * The message ID for the message that will be used to indicate that the
   * provided integer value was above the upper bound.  This takes a single
   * argument, which is the upper bound.
   */
  public static final int MSGID_INSTALLDS_INTEGER_ABOVE_UPPER_BOUND =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 526;



  /**
   * The message ID for the message that will be used to indicate that the
   * response value could not be interpreted as a DN.  This does not take any
   * arguments.
   */
  public static final int MSGID_INSTALLDS_INVALID_DN_RESPONSE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 527;



  /**
   * The message ID for the message that will be used to indicate that the
   * response value was an invalid zero-length string.  This does not take any
   * arguments.
   */
  public static final int MSGID_INSTALLDS_INVALID_STRING_RESPONSE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 528;



  /**
   * The message ID for the message that will be used to indicate that the
   * response value was an invalid zero-length string.  This does not take any
   * arguments.
   */
  public static final int MSGID_INSTALLDS_INVALID_PASSWORD_RESPONSE =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 529;



  /**
   * The message ID for the message that will be used to indicate that the
   * provided password values do not match.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_PASSWORDS_DONT_MATCH =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 530;



  /**
   * The message ID for the message that will be used if an error occurs while
   * reading from standard input.  This takes a single argument, which is a
   * message explaining the problem that occurred.
   */
  public static final int MSGID_INSTALLDS_ERROR_READING_FROM_STDIN =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_MILD_ERROR | 531;



  /**
   * The message ID for the message that will be used as the description of the
   * quiet argument.  This does not take any arguments.
   */
  public static final int MSGID_LDIFIMPORT_DESCRIPTION_QUIET =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 532;



  /**
   * The message ID for the message that will be used to indicate that the
   * LDIF import was successful.  This does not take any arguments.
   */
  public static final int MSGID_INSTALLDS_IMPORT_SUCCESSFUL =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 533;



  /**
   * The message ID for the message that will be used if the user did not
   * specify the path to the Directory Server configuration file.  This takes a
   * single argument, which is the name of the command-line option that should
   * be used to provide that information.
   */
  public static final int MSGID_INSTALLDS_INITIALIZING =
       CATEGORY_MASK_TOOLS | SEVERITY_MASK_INFORMATIONAL | 534;



  /**
   * Associates a set of generic messages with the message IDs defined in this
   * class.
   */
  public static void registerMessages()
  {
    registerMessage(MSGID_TOOLS_CANNOT_CREATE_SSL_CONNECTION,
                    "Unable to create an SSL connection to the server: %s");
    registerMessage(MSGID_TOOLS_SSL_CONNECTION_NOT_INITIALIZED,
                    "Unable to create an SSL connection to the server because" +
                    " the connection factory has not been initialized.");
    registerMessage(MSGID_TOOLS_CANNOT_LOAD_KEYSTORE_FILE,
                    "Cannot load the key store file: %s.");
    registerMessage(MSGID_TOOLS_CANNOT_INIT_KEYMANAGER,
                    "Cannot initialize the key manager for the key store:" +
                    "%s.");
    registerMessage(MSGID_TOOLS_CANNOT_LOAD_TRUSTSTORE_FILE,
                    "Cannot load the key store file: %s.");
    registerMessage(MSGID_TOOLS_CANNOT_INIT_TRUSTMANAGER,
                    "Cannot initialize the key manager for the key store:" +
                    "%s.");


    registerMessage(MSGID_ENCPW_DESCRIPTION_LISTSCHEMES,
                    "Lists the available password storage schemes configured " +
                    "in the Directory Server.");
    registerMessage(MSGID_ENCPW_DESCRIPTION_CLEAR_PW,
                    "Specifies the clear-text password to be encoded and/or " +
                    "compared against an encoded password.");
    registerMessage(MSGID_ENCPW_DESCRIPTION_CLEAR_PW_FILE,
                    "Specifies the path to a file containing the clear-text " +
                    "password to be encoded and/or compared against an " +
                    "encoded password.");
    registerMessage(MSGID_ENCPW_DESCRIPTION_ENCODED_PW,
                    "Specifies the encoded password to be compared against " +
                    "the provided clear-text password.");
    registerMessage(MSGID_ENCPW_DESCRIPTION_ENCODED_PW_FILE,
                    "Specifies the path to a file containing the encoded " +
                    "password to be compared against the provided clear-text " +
                    "password.");
    registerMessage(MSGID_ENCPW_DESCRIPTION_CONFIG_CLASS,
                    "Specifies the fully-qualified name of the Java class " +
                    "that serves as the configuration handler for the " +
                    "Directory Server.");
    registerMessage(MSGID_ENCPW_DESCRIPTION_CONFIG_FILE,
                    "Specifies the path to the Directory Server " +
                    "configuration file.");
    registerMessage(MSGID_ENCPW_DESCRIPTION_SCHEME,
                    "Specifies the name of the password storage scheme that " +
                    "should be used to encode the provided clear-text " +
                    "password.");
    registerMessage(MSGID_ENCPW_DESCRIPTION_AUTHPW,
                    "Indicates that the authentication password syntax " +
                    "should be used to encode the password, rather than the " +
                    "default user password syntax.");
    registerMessage(MSGID_ENCPW_DESCRIPTION_USAGE,
                    "Displays this usage information.");
    registerMessage(MSGID_ENCPW_CANNOT_INITIALIZE_ARGS,
                    "An unexpected error occurred while attempting to " +
                    "initialize the command-line arguments:  %s.");
    registerMessage(MSGID_ENCPW_ERROR_PARSING_ARGS,
                    "An error occurred while parsing the command-line " +
                    "arguments:  %s.");
    registerMessage(MSGID_ENCPW_NO_CLEAR_PW,
                    "No clear-text password was specified.  Use --%s or --%s " +
                    "to specify the password to encode.");
    registerMessage(MSGID_ENCPW_NO_SCHEME,
                    "No password storage scheme was specified.  Use the --%s " +
                    "argument to specify the storage scheme.");
    registerMessage(MSGID_ENCPW_SERVER_BOOTSTRAP_ERROR,
                    "An unexpected error occurred while attempting to " +
                    "bootstrap the Directory Server client-side code:  %s.");
    registerMessage(MSGID_ENCPW_CANNOT_LOAD_CONFIG,
                    "An error occurred while trying to load the Directory " +
                    "Server configuration:  %s.");
    registerMessage(MSGID_ENCPW_CANNOT_LOAD_SCHEMA,
                    "An error occurred while trying to load the Directory " +
                    "Server schema:  %s.");
    registerMessage(MSGID_ENCPW_CANNOT_INITIALIZE_CORE_CONFIG,
                    "An error occurred while trying to initialize the core " +
                    "Directory Server configuration:  %s.");
    registerMessage(MSGID_ENCPW_CANNOT_INITIALIZE_STORAGE_SCHEMES,
                    "An error occurred while trying to initialize the " +
                    "Directory Server password storage schemes:  %s.");
    registerMessage(MSGID_ENCPW_NO_AUTH_STORAGE_SCHEMES,
                    "No authentication password storage schemes have been " +
                    "configured for use in the Directory Server.");
    registerMessage(MSGID_ENCPW_NO_STORAGE_SCHEMES,
                    "No password storage schemes have been configured for " +
                    "use in the Directory Server.");
    registerMessage(MSGID_ENCPW_NO_SUCH_AUTH_SCHEME,
                    "Authentication password storage scheme \"%s\" is not "+
                    "configured for use in the Directory Server.");
    registerMessage(MSGID_ENCPW_NO_SUCH_SCHEME,
                    "Password storage scheme \"%s\" is not configured for " +
                    "use in the Directory Server.");
    registerMessage(MSGID_ENCPW_INVALID_ENCODED_AUTHPW,
                    "The provided password is not a valid encoded " +
                    "authentication password value:  %s.");
    registerMessage(MSGID_ENCPW_PASSWORDS_MATCH,
                    "The provided clear-text and encoded passwords match.");
    registerMessage(MSGID_ENCPW_PASSWORDS_DO_NOT_MATCH,
                    "The provided clear-text and encoded passwords do not " +
                    "match.");
    registerMessage(MSGID_ENCPW_ENCODED_PASSWORD,
                    "Encoded Password:  \"%s\".");
    registerMessage(MSGID_ENCPW_CANNOT_ENCODE,
                    "An error occurred while attempting to encode the " +
                    "clear-text password:  %s.");


    registerMessage(MSGID_LDIFEXPORT_DESCRIPTION_CONFIG_CLASS,
                    "Specifies the fully-qualified name of the Java class " +
                    "that serves as the configuration handler for the " +
                    "Directory Server.");
    registerMessage(MSGID_LDIFEXPORT_DESCRIPTION_CONFIG_FILE,
                    "Specifies the path to the Directory Server " +
                    "configuration file.");
    registerMessage(MSGID_LDIFEXPORT_DESCRIPTION_LDIF_FILE,
                    "Specifies the path to the file to which the LDIF data " +
                    "should be written.");
    registerMessage(MSGID_LDIFEXPORT_DESCRIPTION_APPEND_TO_LDIF,
                    "Indicates that the export process should append to an " +
                    "existing LDIF file rather than overwrite it.");
    registerMessage(MSGID_LDIFEXPORT_DESCRIPTION_BACKEND_ID,
                    "Specifies the backend ID for the backend from which the " +
                    "data should be exported.");
    registerMessage(MSGID_LDIFEXPORT_DESCRIPTION_INCLUDE_BRANCH,
                    "Specifies the base DN of a branch that should be " +
                    "included in the LDIF export.  This argument may be " +
                    "provided more than once to specify multiple include " +
                    "branches.");
    registerMessage(MSGID_LDIFEXPORT_DESCRIPTION_EXCLUDE_BRANCH,
                    "Specifies the base DN of a branch that should be " +
                    "excluded from the LDIF export.  This argument may be " +
                    "provided more than once to specify multiple exclude " +
                    "branches.");
    registerMessage(MSGID_LDIFEXPORT_DESCRIPTION_INCLUDE_ATTRIBUTE,
                    "Specifies an attribute that should be included in the " +
                    "LDIF export.  This argument may be provided more than " +
                    "once to specify multiple include attributes.  If this " +
                    "is used, then only the listed include attributes will " +
                    "be included in the LDIF export.");
    registerMessage(MSGID_LDIFEXPORT_DESCRIPTION_EXCLUDE_ATTRIBUTE,
                    "Specifies an attribute that should be excluded from the " +
                    "LDIF export.  This argument may be provided more than " +
                    "once to specify multiple exclude attributes.");
    registerMessage(MSGID_LDIFEXPORT_DESCRIPTION_INCLUDE_FILTER,
                    "Specifies a search filter that may be used to control " +
                    "which entries are included in the export.  Only entries " +
                    "matching the specified filter will be included.  This " +
                    "argument may be provided more than once to specify " +
                    "multiple include filters.");
    registerMessage(MSGID_LDIFEXPORT_DESCRIPTION_EXCLUDE_FILTER,
                    "Specifies a search filter that may be used to control " +
                    "which entries are excluded from the export.  Any entry " +
                    "matching the specified filter will be excluded.  This " +
                    "argument may be provided more than once to specify " +
                    "multiple exclude filters.");
    registerMessage(MSGID_LDIFEXPORT_DESCRIPTION_WRAP_COLUMN,
                    "Specifies the column at which long lines should be " +
                    "wrapped.  A value of zero indicates that long lines " +
                    "should not be wrapped.");
    registerMessage(MSGID_LDIFEXPORT_DESCRIPTION_COMPRESS_LDIF,
                    "Indicates that the LDIF data should be compressed as it " +
                    "is exported.");
    registerMessage(MSGID_LDIFEXPORT_DESCRIPTION_ENCRYPT_LDIF,
                    "Indicates that the LDIF data should be encrypted as it " +
                    "is exported.");
    registerMessage(MSGID_LDIFEXPORT_DESCRIPTION_SIGN_HASH,
                    "Indicates that a signed hash of the export data " +
                    "should be appended to the LDIF file.");
    registerMessage(MSGID_LDIFEXPORT_DESCRIPTION_USAGE,
                    "Displays this usage information.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_INITIALIZE_ARGS,
                    "An unexpected error occurred while attempting to " +
                    "initialize the command-line arguments:  %s.");
    registerMessage(MSGID_LDIFEXPORT_ERROR_PARSING_ARGS,
                    "An error occurred while parsing the command-line " +
                    "arguments:  %s.");
    registerMessage(MSGID_LDIFEXPORT_SERVER_BOOTSTRAP_ERROR,
                    "An unexpected error occurred while attempting to " +
                    "bootstrap the Directory Server client-side code:  %s.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_LOAD_CONFIG,
                    "An error occurred while trying to load the Directory " +
                    "Server configuration:  %s.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_LOAD_SCHEMA,
                    "An error occurred while trying to load the Directory " +
                    "Server schema:  %s.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_INITIALIZE_CORE_CONFIG,
                    "An error occurred while trying to initialize the core " +
                    "Directory Server configuration:  %s.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_INITIALIZE_CRYPTO_MANAGER,
                    "An error occurred while attempting to initialize the " +
                    "crypto manager:  %s.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_INITIALIZE_PLUGINS,
                    "An error occurred while attempting to initialize the " +
                    "LDIF export plugins:  %s.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_PARSE_EXCLUDE_FILTER,
                    "Unable to decode exclude filter string \"%s\" as a " +
                    "valid search filter:  %s.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_PARSE_INCLUDE_FILTER,
                    "Unable to decode include filter string \"%s\" as a " +
                    "valid search filter:  %s.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_DECODE_BASE_DN,
                    "Unable to decode base DN string \"%s\" as a valid " +
                    "distinguished name:  %s.");
    registerMessage(MSGID_LDIFEXPORT_MULTIPLE_BACKENDS_FOR_ID,
                    "Multiple Directory Server backends are configured with " +
                    "the requested backend ID \"%s\".");
    registerMessage(MSGID_LDIFEXPORT_NO_BACKENDS_FOR_ID,
                    "None of the Directory Server backends are configured " +
                    "with the requested backend ID \"%s\".");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_EXPORT_BACKEND,
                    "The Directory Server backend with backend ID \"%s\" " +
                    "does not provide a mechanism for performing LDIF " +
                    "exports.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_DECODE_EXCLUDE_BASE,
                    "Unable to decode exclude branch string \"%s\" as a " +
                    "valid distinguished name:  %s.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_DECODE_WRAP_COLUMN_AS_INTEGER,
                    "Unable to decode wrap column value \"%s\" as an " +
                    "integer.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_LOCK_BACKEND,
                    "An error occurred while attempting to acquire a shared " +
                    "lock for backend %s:  %s.  This generally means that " +
                    "some other process has an exclusive lock on this " +
                    "backend (e.g., an LDIF import or a restore).  The LDIF " +
                    "export cannot continue.");
    registerMessage(MSGID_LDIFEXPORT_ERROR_DURING_EXPORT,
                    "An error occurred while attempting to process the LDIF " +
                    "export:  %s.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_UNLOCK_BACKEND,
                    "An error occurred while attempting to release the " +
                    "shared lock for backend %s:  %s.  This lock should " +
                    "automatically be cleared when the export process exits, " +
                    "so no further action should be required.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_DECODE_BACKEND_BASE_DN,
                    "Unable to decode the backend configuration base DN " +
                    "string \"%s\" as a valid DN:  %s.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_RETRIEVE_BACKEND_BASE_ENTRY,
                    "Unable to retrieve the backend configuration base entry " +
                    "\"%s\" from the server configuration:  %s.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_DETERMINE_BACKEND_CLASS,
                    "Cannot determine the name of the Java class providing " +
                    "the logic for the backend defined in configuration " +
                    "entry %s:  %s.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_LOAD_BACKEND_CLASS,
                    "Unable to load class %s referenced in configuration " +
                    "entry %s for use as a Directory Server backend:  %s.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_INSTANTIATE_BACKEND_CLASS,
                    "Unable to create an instance of class %s referenced in " +
                    "configuration entry %s as a Directory Server backend:  " +
                    "%s.");
    registerMessage(MSGID_LDIFEXPORT_NO_BASES_FOR_BACKEND,
                    "No base DNs have been defined in backend configuration " +
                    "entry %s.  This backend will not be evaluated in the " +
                    "LDIF export process.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_DETERMINE_BASES_FOR_BACKEND,
                    "Unable to determine the set of base DNs defined in " +
                    "backend configuration entry %s:  %s.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_DETERMINE_BACKEND_ID,
                    "Cannot determine the backend ID for the backend defined " +
                    "in configuration entry %s:  %s.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_DECODE_INCLUDE_BASE,
                    "Unable to decode include branch string \"%s\" as a " +
                    "valid distinguished name:  %s.");
    registerMessage(MSGID_LDIFEXPORT_INVALID_INCLUDE_BASE,
                    "Provided include base DN \"%s\" is not handled by the " +
                    "backend with backend ID %s.");


    registerMessage(MSGID_LDIFIMPORT_DESCRIPTION_CONFIG_CLASS,
                    "Specifies the fully-qualified name of the Java class " +
                    "that serves as the configuration handler for the " +
                    "Directory Server.");
    registerMessage(MSGID_LDIFIMPORT_DESCRIPTION_CONFIG_FILE,
                    "Specifies the path to the Directory Server " +
                    "configuration file.");
    registerMessage(MSGID_LDIFIMPORT_DESCRIPTION_LDIF_FILE,
                    "Specifies the path to the file containing the LDIF data " +
                    "to import.  This argument may be provided more than " +
                    "once to import from multiple LDIF files (the files " +
                    "will be processed in the order they are provided in " +
                    "the arguments).");
    registerMessage(MSGID_LDIFIMPORT_DESCRIPTION_APPEND,
                    "Indicates that the import process should append to the " +
                    "existing database rather than overwriting it.");
    registerMessage(MSGID_LDIFIMPORT_DESCRIPTION_REPLACE_EXISTING,
                    "Indicates whether an existing entry should be replaced " +
                    "when appending to an existing database.");
    registerMessage(MSGID_LDIFIMPORT_DESCRIPTION_BACKEND_ID,
                    "Specifies the backend ID for the backend into which the " +
                    "data should be imported.");
    registerMessage(MSGID_LDIFIMPORT_DESCRIPTION_INCLUDE_BRANCH,
                    "Specifies the base DN of a branch that should be " +
                    "included in the LDIF import.  This argument may be " +
                    "provided more than once to specify multiple include " +
                    "branches.");
    registerMessage(MSGID_LDIFIMPORT_DESCRIPTION_EXCLUDE_BRANCH,
                    "Specifies the base DN of a branch that should be " +
                    "excluded from the LDIF import.  This argument may be " +
                    "provided more than once to specify multiple exclude " +
                    "branches.");
    registerMessage(MSGID_LDIFIMPORT_DESCRIPTION_INCLUDE_ATTRIBUTE,
                    "Specifies an attribute that should be included in the " +
                    "LDIF import.  This argument may be provided more than " +
                    "once to specify multiple include attributes.  If this " +
                    "is used, then only the listed include attributes will " +
                    "be included in the LDIF import.");
    registerMessage(MSGID_LDIFIMPORT_DESCRIPTION_EXCLUDE_ATTRIBUTE,
                    "Specifies an attribute that should be excluded from the " +
                    "LDIF import.  This argument may be provided more than " +
                    "once to specify multiple exclude attributes.");
    registerMessage(MSGID_LDIFIMPORT_DESCRIPTION_INCLUDE_FILTER,
                    "Specifies a search filter that may be used to control " +
                    "which entries are included in the import.  Only entries " +
                    "matching the specified filter will be included.  This " +
                    "argument may be provided more than once to specify " +
                    "multiple include filters.");
    registerMessage(MSGID_LDIFIMPORT_DESCRIPTION_EXCLUDE_FILTER,
                    "Specifies a search filter that may be used to control " +
                    "which entries are excluded from the import.  Any entry " +
                    "matching the specified filter will be excluded.  This " +
                    "argument may be provided more than once to specify " +
                    "multiple exclude filters.");
    registerMessage(MSGID_LDIFIMPORT_DESCRIPTION_REJECT_FILE,
                    "Specifies the path to a file into which rejected " +
                    "entries may be written if they are not accepted during " +
                    "the import process.");
    registerMessage(MSGID_LDIFIMPORT_DESCRIPTION_OVERWRITE_REJECTS,
                    "Indicates whether to overwrite an existing rejects " +
                    "file when performing an LDIF import rather than " +
                    "appending to it.");
    registerMessage(MSGID_LDIFIMPORT_DESCRIPTION_SKIP_SCHEMA_VALIDATION,
                    "Indicates whether to skip schema validation during the " +
                    "import.");
    registerMessage(MSGID_LDIFIMPORT_DESCRIPTION_IS_COMPRESSED,
                    "Indicates whether the LDIF file containing the data to " +
                    "import is compressed.");
    registerMessage(MSGID_LDIFIMPORT_DESCRIPTION_IS_ENCRYPTED,
                    "Indicates whether the LDIF file containing the data to " +
                    "import is encrypted.");
    registerMessage(MSGID_LDIFIMPORT_DESCRIPTION_QUIET,
                    "Use quiet mode (no output).");
    registerMessage(MSGID_LDIFIMPORT_DESCRIPTION_USAGE,
                    "Displays this usage information.");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_INITIALIZE_ARGS,
                    "An unexpected error occurred while attempting to " +
                    "initialize the command-line arguments:  %s.");
    registerMessage(MSGID_LDIFIMPORT_ERROR_PARSING_ARGS,
                    "An error occurred while parsing the command-line " +
                    "arguments:  %s.");
    registerMessage(MSGID_LDIFIMPORT_SERVER_BOOTSTRAP_ERROR,
                    "An unexpected error occurred while attempting to " +
                    "bootstrap the Directory Server client-side code:  %s.");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_LOAD_CONFIG,
                    "An error occurred while trying to load the Directory " +
                    "Server configuration:  %s.");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_LOAD_SCHEMA,
                    "An error occurred while trying to load the Directory " +
                    "Server schema:  %s.");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_INITIALIZE_CORE_CONFIG,
                    "An error occurred while trying to initialize the core " +
                    "Directory Server configuration:  %s.");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_INITIALIZE_CRYPTO_MANAGER,
                    "An error occurred while attempting to initialize the " +
                    "crypto manager:  %s.");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_INITIALIZE_PWPOLICY,
                    "An error occurred while attempting to initialize the " +
                    "password policy components:  %s.");
    registerMessage(MSGID_LDIFEXPORT_CANNOT_INITIALIZE_PLUGINS,
                    "An error occurred while attempting to initialize the " +
                    "LDIF import plugins:  %s.");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_PARSE_EXCLUDE_FILTER,
                    "Unable to decode exclude filter string \"%s\" as a " +
                    "valid search filter:  %s.");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_PARSE_INCLUDE_FILTER,
                    "Unable to decode include filter string \"%s\" as a " +
                    "valid search filter:  %s.");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_DECODE_BASE_DN,
                    "Unable to decode base DN string \"%s\" as a valid " +
                    "distinguished name:  %s.");
    registerMessage(MSGID_LDIFIMPORT_MULTIPLE_BACKENDS_FOR_ID,
                    "Multiple Directory Server backends are configured with " +
                    "backend ID \"%s\".");
    registerMessage(MSGID_LDIFIMPORT_NO_BACKENDS_FOR_ID,
                    "None of the Directory Server backends are configured " +
                    "with the requested backend ID \"%s\".");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_IMPORT,
                    "The Directory Server backend with backend ID %s does " +
                    "not provide a mechanism for performing LDIF imports.");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_DECODE_EXCLUDE_BASE,
                    "Unable to decode exclude branch string \"%s\" as a " +
                    "valid distinguished name:  %s.");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_DECODE_INCLUDE_BASE,
                    "Unable to decode include branch string \"%s\" as a " +
                    "valid distinguished name:  %s.");
    registerMessage(MSGID_LDIFIMPORT_INVALID_INCLUDE_BASE,
                    "Provided include base DN \"%s\" is not handled by the " +
                    "backend with backend ID %s.");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_OPEN_REJECTS_FILE,
                    "An error occurred while trying to open the rejects " +
                    "file %s for writing:  %s.");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_LOCK_BACKEND,
                    "An error occurred while attempting to acquire an " +
                    "exclusive lock for backend %s:  %s.  This generally " +
                    "means some other process is still using this backend " +
                    "(e.g., it is in use by the Directory Server or a " +
                    "backup or LDIF export is in progress.  The LDIF import " +
                    "cannot continue.");
    registerMessage(MSGID_LDIFIMPORT_ERROR_DURING_IMPORT,
                    "An error occurred while attempting to process the LDIF " +
                    "import:  %s.");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_UNLOCK_BACKEND,
                    "An error occurred while attempting to release the " +
                    "exclusive lock for backend %s:  %s.  This lock should " +
                    "automatically be cleared when the import process exits, " +
                    "so no further action should be required.");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_DECODE_BACKEND_BASE_DN,
                    "Unable to decode the backend configuration base DN " +
                    "string \"%s\" as a valid DN:  %s.");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_RETRIEVE_BACKEND_BASE_ENTRY,
                    "Unable to retrieve the backend configuration base entry " +
                    "\"%s\" from the server configuration:  %s.");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_DETERMINE_BACKEND_ID,
                    "Cannot determine the backend ID for the backend defined " +
                    "in configuration entry %s:  %s.");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_DETERMINE_BACKEND_CLASS,
                    "Cannot determine the name of the Java class providing " +
                    "the logic for the backend defined in configuration " +
                    "entry %s:  %s.");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_LOAD_BACKEND_CLASS,
                    "Unable to load class %s referenced in configuration " +
                    "entry %s for use as a Directory Server backend:  %s.");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_INSTANTIATE_BACKEND_CLASS,
                    "Unable to create an instance of class %s referenced in " +
                    "configuration entry %s as a Directory Server backend:  " +
                    "%s.");
    registerMessage(MSGID_LDIFIMPORT_NO_BASES_FOR_BACKEND,
                    "No base DNs have been defined in backend configuration " +
                    "entry %s.  This backend will not be evaluated in the " +
                    "LDIF import process.");
    registerMessage(MSGID_LDIFIMPORT_CANNOT_DETERMINE_BASES_FOR_BACKEND,
                    "Unable to determine the set of base DNs defined in " +
                    "backend configuration entry %s:  %s.");


    registerMessage(MSGID_PROCESSING_OPERATION,
                    "Processing %s request for %s.");
    registerMessage(MSGID_OPERATION_FAILED,
                    "%s operation failed for DN %s. %s.");
    registerMessage(MSGID_OPERATION_SUCCESSFUL,
                    "%s operation successful for DN %s.");
    registerMessage(MSGID_PROCESSING_COMPARE_OPERATION,
                    "Comparing type %s with value %s in entry %s.");
    registerMessage(MSGID_COMPARE_OPERATION_RESULT_FALSE,
                    "Compare operation returned false for entry %s.");
    registerMessage(MSGID_COMPARE_OPERATION_RESULT_TRUE,
                    "Compare operation returned true for entry %s.");
    registerMessage(MSGID_SEARCH_OPERATION_INVALID_PROTOCOL,
                    "Invalid operation type returned in search result %s.");
    registerMessage(MSGID_DESCRIPTION_TRUSTALL,
                    "Blindly trust the server SSL certificate.");
    registerMessage(MSGID_DESCRIPTION_BINDDN,
                    "Bind DN.");
    registerMessage(MSGID_DESCRIPTION_BINDPASSWORD,
                    "Bind password (used for simple authentication).");
    registerMessage(MSGID_DESCRIPTION_BINDPASSWORDFILE,
                    "Read bind passwd (for simple authentication) from file");
    registerMessage(MSGID_DESCRIPTION_PROXY_AUTHZID,
                    "Specifies that the proxied authorization control should " +
                    "be used with the given authorization ID.");
    registerMessage(MSGID_DESCRIPTION_PSEARCH_INFO,
                    "Use the persistent search control to be notified of " +
                    "changes to data matching the search criteria.");
    registerMessage(MSGID_DESCRIPTION_REPORT_AUTHZID,
                    "Use the authorization identity control to request that " +
                    "the server provide the authorization ID in the bind " +
                    "response.");
    registerMessage(MSGID_DESCRIPTION_USE_PWP_CONTROL,
                    "Use the password policy control in the bind request.");
    registerMessage(MSGID_BIND_AUTHZID_RETURNED,
                    "# Bound with authorization ID %s.");
    registerMessage(MSGID_BIND_PASSWORD_EXPIRED,
                    "# Your password has expired.");
    registerMessage(MSGID_BIND_PASSWORD_EXPIRING,
                    "# Your password will expire in %s.");
    registerMessage(MSGID_BIND_MUST_CHANGE_PASSWORD,
                    "# You must change your password before any other " +
                    "operations will be allowed.");
    registerMessage(MSGID_BIND_GRACE_LOGINS_REMAINING,
                    "# You have %d grace logins remaining.");
    registerMessage(MSGID_DESCRIPTION_VERBOSE,
                    "Run in verbose mode.");
    registerMessage(MSGID_DESCRIPTION_KEYSTOREPATH,
                    "Path to key database to use for SSL client " +
        "authentication.");
    registerMessage(MSGID_DESCRIPTION_TRUSTSTOREPATH,
                    "Path to the SSL certificate database.");
    registerMessage(MSGID_DESCRIPTION_KEYSTOREPASSWORD,
                    "SSL key password.");
    registerMessage(MSGID_DESCRIPTION_HOST,
                    "LDAP server name or IP address (default: localhost).");
    registerMessage(MSGID_DESCRIPTION_PORT,
                    "LDAP server TCP port number(default: 389).");
    registerMessage(MSGID_DESCRIPTION_VERSION,
                    "LDAP version number(default: 3).");
    registerMessage(MSGID_DESCRIPTION_SHOWUSAGE,
                    "Display usage information.");
    registerMessage(MSGID_DESCRIPTION_CONTROLS,
                    "The OID, criticality and value of the control to apply.");
    registerMessage(MSGID_DESCRIPTION_CONTINUE_ON_ERROR,
                    "Continue processing even if there are errors.");
    registerMessage(MSGID_DESCRIPTION_USE_SSL,
                    "Make an SSL connection to the server.");
    registerMessage(MSGID_DESCRIPTION_START_TLS,
                    "Use a TLS connection to the server.");
    registerMessage(MSGID_DESCRIPTION_USE_SASL_EXTERNAL,
                    "Use the SASL EXTERNAL authentication mechanism.");
    registerMessage(MSGID_DESCRIPTION_ENCODING,
                    "Character set for command line input " +
                    "(default taken from locale)");
    registerMessage(MSGID_DELETE_DESCRIPTION_FILENAME,
                    "The name of the file that contains a list of the DNs of " +
                    "the entries to delete.");
    registerMessage(MSGID_SEARCH_DESCRIPTION_FILENAME,
                    "The name of the file that contains a list of filter " +
                    "strings.");
    registerMessage(MSGID_COMPARE_DESCRIPTION_FILENAME,
                    "The name of the file that contains a list of the DNs of " +
                    "the entries to compare.");
    registerMessage(MSGID_DELETE_DESCRIPTION_DELETE_SUBTREE,
                    "Delete the specified entry and all entries below it.");
    registerMessage(MSGID_MODIFY_DESCRIPTION_DEFAULT_ADD,
                    "Add entries as the default action.");
    registerMessage(MSGID_DESCRIPTION_ASSERTION_FILTER,
                    "Use the LDAP assertion control so that the operation is " +
                    "only processed if the target entry matches the provided " +
                    "assertion filter.");
    registerMessage(MSGID_DESCRIPTION_PREREAD_ATTRS,
                    "Use the LDAP ReadEntry pre-read control to retrieve " +
                    "a copy of the entry immediately before a delete, " +
                    "modify, or modify DN operation.");
    registerMessage(MSGID_DESCRIPTION_POSTREAD_ATTRS,
                    "Use the LDAP ReadEntry post-read control to retrieve " +
                    "a copy of the entry immediately after an add, modify, " +
                    "or modify DN operation.");
    registerMessage(MSGID_DESCRIPTION_SEARCH_ASSERTION_FILTER,
                    "Use the LDAP assertion control so that the search is " +
                    "only processed if the base entry matches the provided " +
                    "assertion filter.");
    registerMessage(MSGID_DESCRIPTION_MATCHED_VALUES_FILTER,
                    "Use the matched values control to only return " +
                    "attribute values matching the specified filter.  " +
                    "This option may be provided multiple times to specify " +
                    "multiple filters.");
    registerMessage(MSGID_DESCRIPTION_COMPARE_ASSERTION_FILTER,
                    "Use the LDAP assertion control so that the compare is " +
                    "only processed if the target entry matches the provided " +
                    "assertion filter.");
    registerMessage(MSGID_SEARCH_DESCRIPTION_BASEDN,
                    "The base DN for the search.");
    registerMessage(MSGID_SEARCH_DESCRIPTION_SIZE_LIMIT,
                    "The size limit (in entries) for search " +
                    "(default is no limit)");
    registerMessage(MSGID_SEARCH_DESCRIPTION_TIME_LIMIT,
                    "The time limit (in seconds) for search " +
                    "(default is no limit)");
    registerMessage(MSGID_SEARCH_DESCRIPTION_SEARCH_SCOPE,
                    "The scope for the search which is one of " +
                    "base, one, sub, or subordinate. (default is sub)");
    registerMessage(MSGID_SEARCH_DESCRIPTION_DEREFERENCE_POLICY,
                    "Alias dereferencing policy. The value is one of never, " +
                    "always, search, or find (default is never).");


    registerMessage(MSGID_LDAPAUTH_CANNOT_SEND_SIMPLE_BIND,
                    "Cannot send the simple bind request:  %s.");
    registerMessage(MSGID_LDAPAUTH_CANNOT_READ_BIND_RESPONSE,
                    "Cannot read the bind response from the server:  " +
                    "%s.");
    registerMessage(MSGID_LDAPAUTH_SERVER_DISCONNECT,
                    "The Directory Server indicated that it was closing the " +
                    "connection to the client (result code %d, message " +
                    "\"%s\".");
    registerMessage(MSGID_LDAPAUTH_UNEXPECTED_EXTENDED_RESPONSE,
                    "The Directory Server sent an unexpected extended " +
                    "response message to the client:  %s.");
    registerMessage(MSGID_LDAPAUTH_UNEXPECTED_RESPONSE,
                    "The Directory Server sent an unexpected response " +
                    "message to the client:  %s.");
    registerMessage(MSGID_LDAPAUTH_SIMPLE_BIND_FAILED,
                    "The simple bind attempt failed:  result code %d (%s), " +
                    "error message \"%s\".");
    registerMessage(MSGID_LDAPAUTH_NO_SASL_MECHANISM,
                    "A SASL bind was requested but no SASL mechanism was " +
                    "specified.");
    registerMessage(MSGID_LDAPAUTH_UNSUPPORTED_SASL_MECHANISM,
                    "The requested SASL mechanism \"%s\" is not supported " +
                    "by this client.");
    registerMessage(MSGID_LDAPAUTH_TRACE_SINGLE_VALUED,
                    "The " + SASL_PROPERTY_TRACE + " SASL property may only " +
                    "be given a single value.");
    registerMessage(MSGID_LDAPAUTH_INVALID_SASL_PROPERTY,
                    "Property \"%s\" is not allowed for the %s SASL " +
                    "mechanism.");
    registerMessage(MSGID_LDAPAUTH_CANNOT_SEND_SASL_BIND,
                    "Cannot send the SASL %S bind request:  %s.");
    registerMessage(MSGID_LDAPAUTH_SASL_BIND_FAILED,
                    "The SASL %s bind attempt failed:  result code %d (%s), " +
                    "error message \"%s\".");
    registerMessage(MSGID_LDAPAUTH_NO_SASL_PROPERTIES,
                    "No SASL properties were provided for use with the %s " +
                    "mechanism.");
    registerMessage(MSGID_LDAPAUTH_AUTHID_SINGLE_VALUED,
                    "The \"" + SASL_PROPERTY_AUTHID + "\" SASL property only " +
                    "accepts a single value.");
    registerMessage(MSGID_LDAPAUTH_SASL_AUTHID_REQUIRED,
                    "The \"" + SASL_PROPERTY_AUTHID + "\" SASL property is " +
                    "required for use with the %s mechanism.");
    registerMessage(MSGID_LDAPAUTH_CANNOT_SEND_INITIAL_SASL_BIND,
                    "Cannot send the initial bind request in the multi-stage " +
                    "%s bind to the server:  %s.");
    registerMessage(MSGID_LDAPAUTH_CANNOT_READ_INITIAL_BIND_RESPONSE,
                    "Cannot read the initial %s bind response from the " +
                    "server:  %s.");
    registerMessage(MSGID_LDAPAUTH_UNEXPECTED_INITIAL_BIND_RESPONSE,
                    "The client received an unexpected intermediate bind " +
                    "response.  The \"SASL bind in progress\" result was " +
                    "expected for the first response in the multi-stage %s " +
                    "bind process, but the bind response had a result code " +
                    "of %d (%s) and an error message of \"%s\".");
    registerMessage(MSGID_LDAPAUTH_NO_CRAMMD5_SERVER_CREDENTIALS,
                    "The initial bind response from the server did not " +
                    "include any server SASL credentials containing the " +
                    "challenge information needed to complete the CRAM-MD5 " +
                    "authentication.");
    registerMessage(MSGID_LDAPAUTH_CANNOT_INITIALIZE_MD5_DIGEST,
                    "An unexpected error occurred while trying to initialize " +
                    "the MD5 digest generator:  %s.");
    registerMessage(MSGID_LDAPAUTH_CANNOT_SEND_SECOND_SASL_BIND,
                    "Cannot send the second bind request in the multi-stage " +
                    "%s bind to the server:  %s.");
    registerMessage(MSGID_LDAPAUTH_CANNOT_READ_SECOND_BIND_RESPONSE,
                    "Cannot read the second %s bind response from the " +
                    "server:  %s.");
    registerMessage(MSGID_LDAPAUTH_NO_ALLOWED_SASL_PROPERTIES,
                    "One or more SASL properties were provided, but the %s " +
                    "mechanism does not take any SASL properties.");
    registerMessage(MSGID_LDAPAUTH_AUTHZID_SINGLE_VALUED,
                    "The \"" + SASL_PROPERTY_AUTHZID + "\" SASL property " +
                    "only accepts a single value.");
    registerMessage(MSGID_LDAPAUTH_REALM_SINGLE_VALUED,
                    "The \"" + SASL_PROPERTY_REALM + "\" SASL property only " +
                    "accepts a single value.");
    registerMessage(MSGID_LDAPAUTH_QOP_SINGLE_VALUED,
                    "The \"" + SASL_PROPERTY_QOP + "\" SASL property only " +
                    "accepts a single value.");
    registerMessage(MSGID_LDAPAUTH_DIGESTMD5_QOP_NOT_SUPPORTED,
                    "The \"%s\" QoP mode is not supported by this client.  " +
                    "Only the \"auth\" mode is currently available for use.");
    // FIXME -- Update this message when auth-int and auth-conf are supported.
    registerMessage(MSGID_LDAPAUTH_DIGESTMD5_INVALID_QOP,
                    "The specified DIGEST-MD5 quality of protection mode " +
                    "\"%s\" is not valid.  The only QoP mode currently " +
                    "supported is \"auth\".");
    registerMessage(MSGID_LDAPAUTH_DIGEST_URI_SINGLE_VALUED,
                    "The \"" + SASL_PROPERTY_DIGEST_URI + "\" SASL property " +
                    "only accepts a single value.");
    registerMessage(MSGID_LDAPAUTH_NO_DIGESTMD5_SERVER_CREDENTIALS,
                    "The initial bind response from the server did not " +
                    "include any server SASL credentials containing the " +
                    "challenge information needed to complete the " +
                    "DIGEST-MD5 authentication.");
    registerMessage(MSGID_LDAPAUTH_DIGESTMD5_INVALID_TOKEN_IN_CREDENTIALS,
                    "The DIGEST-MD5 credentials provided by the server " +
                    "contained an invalid token of \"%s\" starting at " +
                    "position %d.");
    registerMessage(MSGID_LDAPAUTH_DIGESTMD5_INVALID_CHARSET,
                    "The DIGEST-MD5 credentials provided by the server " +
                    "specified the use of the \"%s\" character set.  The " +
                    "character set that may be specified in the DIGEST-MD5 " +
                    "credentials is \"utf-8\".");
    registerMessage(MSGID_LDAPAUTH_REQUESTED_QOP_NOT_SUPPORTED_BY_SERVER,
                    "The requested QoP mode of \"%s\" is not listed as " +
                    "supported by the Directory Server.  The Directory " +
                    "Server's list of supported QoP modes is:  \"%s\".");
    registerMessage(MSGID_LDAPAUTH_DIGESTMD5_NO_NONCE,
                    "The server SASL credentials provided in response to the " +
                    "initial DIGEST-MD5 bind request did not include the " +
                    "nonce to use to generate the authentication digests.");
    registerMessage(MSGID_LDAPAUTH_DIGESTMD5_CANNOT_CREATE_RESPONSE_DIGEST,
                    "An error occurred while attempting to generate the " +
                    "response digest for the DIGEST-MD5 bind request:  %s.");
    registerMessage(MSGID_LDAPAUTH_DIGESTMD5_NO_RSPAUTH_CREDS,
                    "The DIGEST-MD5 bind response from the server did not " +
                    "include the \"rspauth\" element to provide a digest of " +
                    "the response authentication information.");
    registerMessage(MSGID_LDAPAUTH_DIGESTMD5_COULD_NOT_DECODE_RSPAUTH,
                    "An error occurred while trying to decode the rspauth " +
                    "element of the DIGEST-MD5 bind response from the server " +
                    "as a hexadecimal string:  %s.");
    registerMessage(MSGID_LDAPAUTH_DIGESTMD5_COULD_NOT_CALCULATE_RSPAUTH,
                    "An error occurred while trying to calculate the " +
                    "expected rspauth element to compare against the value " +
                    "included in the DIGEST-MD5 response from the server:  " +
                    "%s.");
    registerMessage(MSGID_LDAPAUTH_DIGESTMD5_RSPAUTH_MISMATCH,
                    "The rpsauth element included in the DIGEST-MD5 bind " +
                    "response from the Directory Server was different from " +
                    "the expected value calculated by the client.");
    registerMessage(MSGID_LDAPAUTH_DIGESTMD5_INVALID_CLOSING_QUOTE_POS,
                    "The DIGEST-MD5 response challenge could not be parsed " +
                    "because it had an invalid quotation mark at position %d.");
    registerMessage(MSGID_LDAPAUTH_PROPERTY_DESCRIPTION_TRACE,
                    "Specifies a text string that may be written to the " +
                    "Directory Server error log as trace information for " +
                    "the bind.");
    registerMessage(MSGID_LDAPAUTH_PROPERTY_DESCRIPTION_AUTHID,
                    "Specifies the authentication ID for the bind.");
    registerMessage(MSGID_LDAPAUTH_PROPERTY_DESCRIPTION_REALM,
                    "Specifies the realm into which the authentication is to " +
                    "be performed.");
    registerMessage(MSGID_LDAPAUTH_PROPERTY_DESCRIPTION_QOP,
                    "Specifies the quality of protection to use for the bind.");
    registerMessage(MSGID_LDAPAUTH_PROPERTY_DESCRIPTION_DIGEST_URI,
                    "Specifies the digest URI to use for the bind.");
    registerMessage(MSGID_LDAPAUTH_PROPERTY_DESCRIPTION_AUTHZID,
                    "Specifies the authorization ID to use for the bind.");
    registerMessage(MSGID_DESCRIPTION_SASL_PROPERTIES,
                    "Specifies the SASL properties to use for the bind.");
    registerMessage(MSGID_DESCRIPTION_DONT_WRAP,
                    "Indicates that long lines should not be wrapped.");
    registerMessage(MSGID_LDAPAUTH_PROPERTY_DESCRIPTION_KDC,
                    "Specifies the KDC to use for the Kerberos " +
                    "authentication.");
    registerMessage(MSGID_LDAPAUTH_KDC_SINGLE_VALUED,
                    "The \"" + SASL_PROPERTY_KDC + "\" SASL property only " +
                    "accepts a single value.");
    // FIXME -- Update this message when auth-int and auth-conf are supported.
    registerMessage(MSGID_LDAPAUTH_GSSAPI_INVALID_QOP,
                    "The specified GSSAPI quality of protection mode \"%s\" " +
                    "is not valid.  The only QoP mode currently supported is " +
                    "\"auth\".");
    registerMessage(MSGID_LDAPAUTH_GSSAPI_CANNOT_CREATE_JAAS_CONFIG,
                    "An error occurred while trying to create the " +
                    "temporary JAAS configuration for GSSAPI " +
                    "authentication:  %s.");
    registerMessage(MSGID_LDAPAUTH_GSSAPI_LOCAL_AUTHENTICATION_FAILED,
                    "An error occurred while attempting to perform local " +
                    "authentication to the Kerberos realm:  %s.");
    registerMessage(MSGID_LDAPAUTH_GSSAPI_REMOTE_AUTHENTICATION_FAILED,
                    "An error occurred while attempting to perform GSSAPI " +
                    "authentication to the Directory Server:  %s.");
    registerMessage(MSGID_LDAPAUTH_NONSASL_RUN_INVOCATION,
                    "The LDAPAuthenticationHandler.run() method was called " +
                    "for a non-SASL bind.  The backtrace for this call is %s.");
    registerMessage(MSGID_LDAPAUTH_UNEXPECTED_RUN_INVOCATION,
                    "The LDAPAuthenticationHandler.run() method was called " +
                    "for a SASL bind with an unexpected mechanism of " +
                    "\"%s\".  The backtrace for this call is %s.");
    registerMessage(MSGID_LDAPAUTH_GSSAPI_CANNOT_CREATE_SASL_CLIENT,
                    "An error occurred while attempting to create a SASL " +
                    "client to process the GSSAPI authentication:  %s.");
    registerMessage(MSGID_LDAPAUTH_GSSAPI_CANNOT_CREATE_INITIAL_CHALLENGE,
                    "An error occurred while attempting to create the " +
                    "initial challenge for GSSAPI authentication:  %s.");
    registerMessage(MSGID_LDAPAUTH_GSSAPI_CANNOT_VALIDATE_SERVER_CREDS,
                    "An error occurred while trying to validate the SASL " +
                    "credentials provided by the Directory Server in the " +
                    "GSSAPI bind response:  %s.");
    registerMessage(MSGID_LDAPAUTH_GSSAPI_UNEXPECTED_SUCCESS_RESPONSE,
                    "The Directory Server unexpectedly returned a success " +
                    "response to the client even though the client does not " +
                    "believe that the GSSAPI negotiation is complete.");
    registerMessage(MSGID_LDAPAUTH_GSSAPI_BIND_FAILED,
                    "The GSSAPI bind attempt failed.  The result code was %d " +
                    "(%s), and the error message was \"%s\".");
    registerMessage(MSGID_LDAPAUTH_NONSASL_CALLBACK_INVOCATION,
                    "The LDAPAuthenticationHandler.handle() method was " +
                    "called for a non-SASL bind.  The backtrace for this " +
                    "call is %s.");
    registerMessage(MSGID_LDAPAUTH_UNEXPECTED_GSSAPI_CALLBACK,
                    "The LDAPAuthenticationHandler.handle() method was " +
                    "called during a GSSAPI bind attempt with an unexpected " +
                    "callback type of %s.");
    registerMessage(MSGID_LDAPAUTH_UNEXPECTED_CALLBACK_INVOCATION,
                    "The LDAPAuthenticationHandler.handle() method was " +
                    "called for an unexpected SASL mechanism of %s.  The " +
                    "backtrace for this call is %s.");
    registerMessage(MSGID_LDAPAUTH_PASSWORD_PROMPT,
                    "Password for user '%s':  ");
    registerMessage(MSGID_LDAPAUTH_CANNOT_SEND_WHOAMI_REQUEST,
                    "Cannot send the 'Who Am I?' request to the Directory " +
                    "Server:  %s.");
    registerMessage(MSGID_LDAPAUTH_CANNOT_READ_WHOAMI_RESPONSE,
                    "Cannot read the 'Who Am I?' response from the Directory " +
                    "Server:  %s.");
    registerMessage(MSGID_LDAPAUTH_WHOAMI_FAILED,
                    "The 'Who Am I?' request was rejected by the Directory " +
                    "Server with a result code of %d (%s) and and error " +
                    "message of \"%s\".");


    registerMessage(MSGID_DESCRIPTION_INVALID_VERSION,
                    "Invalid LDAP version number '%s'. Allowed values are " +
                    "2 and 3.");
    registerMessage(MSGID_SEARCH_INVALID_SEARCH_SCOPE,
                    "Invalid scope %s specified for the search request.");
    registerMessage(MSGID_SEARCH_NO_FILTERS,
                    "No filters specified for the search request.");
    registerMessage(MSGID_PSEARCH_MISSING_DESCRIPTOR,
                    "The request to use the persistent search control did " +
                    "not include a descriptor that indicates the options to " +
                    "use with that control.");
    registerMessage(MSGID_PSEARCH_DOESNT_START_WITH_PS,
                    "The persistent search descriptor %s did not start with " +
                    "the required 'ps' string.");
    registerMessage(MSGID_PSEARCH_INVALID_CHANGE_TYPE,
                    "The provided change type value %s is invalid.  The " +
                    "recognized change types are add, delete, modify, " +
                    "modifydn, and any.");
    registerMessage(MSGID_PSEARCH_INVALID_CHANGESONLY,
                    "The provided changesOnly value %s is invalid.  Allowed " +
                    "values are 1 to only return matching entries that have " +
                    "changed since the beginning of the search, or 0 to also " +
                    "include existing entries that match the search criteria.");
    registerMessage(MSGID_PSEARCH_INVALID_RETURN_ECS,
                    "The provided returnECs value %s is invalid.  Allowed " +
                    "values are 1 to request that the entry change " +
                    "notification control be included in updated entries, or " +
                    "0 to exclude the control from matching entries.");
    registerMessage(MSGID_LDAP_ASSERTION_INVALID_FILTER,
                    "The search filter provided for the LDAP assertion " +
                    "control was invalid:  %s.");
    registerMessage(MSGID_LDAP_MATCHEDVALUES_INVALID_FILTER,
                    "The provided matched values filter was invalid:  %s.");
    registerMessage(MSGID_LDAPMODIFY_PREREAD_NO_VALUE,
                    "The pre-read response control did not include a value.");
    registerMessage(MSGID_LDAPMODIFY_PREREAD_CANNOT_DECODE_VALUE,
                    "An error occurred while trying to decode the entry " +
                    "contained in the value of the pre-read response " +
                    "control:  %s.");
    registerMessage(MSGID_LDAPMODIFY_PREREAD_ENTRY,
                    "Target entry before the operation:");
    registerMessage(MSGID_LDAPMODIFY_POSTREAD_NO_VALUE,
                    "The post-read response control did not include a value.");
    registerMessage(MSGID_LDAPMODIFY_POSTREAD_CANNOT_DECODE_VALUE,
                    "An error occurred while trying to decode the entry " +
                    "contained in the value of the post-read response " +
                    "control:  %s.");
    registerMessage(MSGID_LDAPMODIFY_POSTREAD_ENTRY,
                    "Target entry after the operation:");



    registerMessage(MSGID_VERIFYINDEX_DESCRIPTION_BASE_DN,
                    "Specifies the base DN of a backend supporting indexing. " +
                    "Verification is performed on indexes within the scope " +
                    "of the given base DN.");
    registerMessage(MSGID_VERIFYINDEX_DESCRIPTION_INDEX_NAME,
                    "Specifies the name of an index to be verified. For an " +
                    "attribute index this is simply an attribute name.  " +
                    "Multiple indexes may be verified for completeness, or " +
                    "all indexes if no indexes are specified.  An index is " +
                    "complete if each index value references all entries " +
                    "containing that value.");
    registerMessage(MSGID_VERIFYINDEX_DESCRIPTION_VERIFY_CLEAN,
                    "Specifies that a single index should be verified to " +
                    "ensure it is clean.  An index is clean if each index " +
                    "value references only entries containing that value.  " +
                    "Only one index at a time may be verified in this way.");
    registerMessage(MSGID_VERIFYINDEX_ERROR_DURING_VERIFY,
                    "An error occurred while attempting to perform index " +
                    "verification:  %s.");
    registerMessage(MSGID_VERIFYINDEX_VERIFY_CLEAN_REQUIRES_SINGLE_INDEX,
                    "Only one index at a time may be verified for " +
                    "cleanliness.");
    registerMessage(MSGID_VERIFYINDEX_WRONG_BACKEND_TYPE,
                    "The backend does not support indexing.");
    registerMessage(MSGID_VERIFYINDEX_DESCRIPTION_CONFIG_CLASS,
                    "Specifies the fully-qualified name of the Java class " +
                    "that serves as the configuration handler for the " +
                    "Directory Server.");
    registerMessage(MSGID_VERIFYINDEX_DESCRIPTION_CONFIG_FILE,
                    "Specifies the path to the Directory Server " +
                    "configuration file.");
    registerMessage(MSGID_VERIFYINDEX_DESCRIPTION_USAGE,
                    "Displays this usage information.");
    registerMessage(MSGID_VERIFYINDEX_CANNOT_INITIALIZE_ARGS,
                    "An unexpected error occurred while attempting to " +
                    "initialize the command-line arguments:  %s.");
    registerMessage(MSGID_VERIFYINDEX_ERROR_PARSING_ARGS,
                    "An error occurred while parsing the command-line " +
                    "arguments:  %s.");
    registerMessage(MSGID_VERIFYINDEX_SERVER_BOOTSTRAP_ERROR,
                    "An unexpected error occurred while attempting to " +
                    "bootstrap the Directory Server client-side code:  %s.");
    registerMessage(MSGID_VERIFYINDEX_CANNOT_LOAD_CONFIG,
                    "An error occurred while trying to load the Directory " +
                    "Server configuration:  %s.");
    registerMessage(MSGID_VERIFYINDEX_CANNOT_LOAD_SCHEMA,
                    "An error occurred while trying to load the Directory " +
                    "Server schema:  %s.");
    registerMessage(MSGID_VERIFYINDEX_CANNOT_INITIALIZE_CORE_CONFIG,
                    "An error occurred while trying to initialize the core " +
                    "Directory Server configuration:  %s.");
    registerMessage(MSGID_VERIFYINDEX_CANNOT_LOCK_BACKEND,
                    "An error occurred while attempting to acquire a shared " +
                    "lock for backend %s:  %s.  This generally means that " +
                    "some other process has an exclusive lock on this " +
                    "backend (e.g., an LDIF import or a restore).  The " +
                    "index verification cannot continue.");
    registerMessage(MSGID_VERIFYINDEX_CANNOT_UNLOCK_BACKEND,
                    "An error occurred while attempting to release the " +
                    "shared lock for backend %s:  %s.  This lock should " +
                    "automatically be cleared when the verification process " +
                    "exits, so no further action should be required.");
    registerMessage(MSGID_VERIFYINDEX_CANNOT_DECODE_BASE_DN,
                    "Unable to decode base DN string \"%s\" as a valid " +
                    "distinguished name:  %s.");
    registerMessage(MSGID_VERIFYINDEX_MULTIPLE_BACKENDS_FOR_BASE,
                    "Multiple Directory Server backends are configured to " +
                    "support base DN \"%s\".");
    registerMessage(MSGID_VERIFYINDEX_NO_BACKENDS_FOR_BASE,
                    "None of the Directory Server backends are configured " +
                    "to support the requested base DN \"%s\".");
    registerMessage(MSGID_VERIFYINDEX_CANNOT_DECODE_BACKEND_BASE_DN,
                    "Unable to decode the backend configuration base DN " +
                    "string \"%s\" as a valid DN:  %s.");
    registerMessage(MSGID_VERIFYINDEX_CANNOT_RETRIEVE_BACKEND_BASE_ENTRY,
                    "Unable to retrieve the backend configuration base entry " +
                    "\"%s\" from the server configuration:  %s.");
    registerMessage(MSGID_VERIFYINDEX_CANNOT_DETERMINE_BACKEND_CLASS,
                    "Cannot determine the name of the Java class providing " +
                    "the logic for the backend defined in configuration " +
                    "entry %s:  %s.");
    registerMessage(MSGID_VERIFYINDEX_CANNOT_LOAD_BACKEND_CLASS,
                    "Unable to load class %s referenced in configuration " +
                    "entry %s for use as a Directory Server backend:  %s.");
    registerMessage(MSGID_VERIFYINDEX_CANNOT_INSTANTIATE_BACKEND_CLASS,
                    "Unable to create an instance of class %s referenced in " +
                    "configuration entry %s as a Directory Server backend:  " +
                    "%s.");
    registerMessage(MSGID_VERIFYINDEX_NO_BASES_FOR_BACKEND,
                    "No base DNs have been defined in backend configuration " +
                    "entry %s.  This backend will not be evaluated in the " +
                    "data verification process.");
    registerMessage(MSGID_VERIFYINDEX_CANNOT_DETERMINE_BASES_FOR_BACKEND,
                    "Unable to determine the set of base DNs defined in " +
                    "backend configuration entry %s:  %s.");


    registerMessage(MSGID_BACKUPDB_DESCRIPTION_CONFIG_CLASS,
                    "Specifies the fully-qualified name of the Java class " +
                    "that serves as the configuration handler for the " +
                    "Directory Server.");
    registerMessage(MSGID_BACKUPDB_DESCRIPTION_CONFIG_FILE,
                    "Specifies the path to the Directory Server " +
                    "configuration file.");
    registerMessage(MSGID_BACKUPDB_DESCRIPTION_BACKEND_ID,
                    "Specifies the backend ID for the backend that should " +
                    "be archived.  Multiple backends may be archived by " +
                    "providing this argument multiple times.");
    registerMessage(MSGID_BACKUPDB_DESCRIPTION_BACKUP_ALL,
                    "Indicates that all backends defined in the server " +
                    "should be backed up.");
    registerMessage(MSGID_BACKUPDB_DESCRIPTION_BACKUP_ID,
                    "Specifies the backup ID that will be used to identify " +
                    "the backup that is created.");
    registerMessage(MSGID_BACKUPDB_DESCRIPTION_BACKUP_DIR,
                    "Specifies the path to the directory in which the " +
                    "backup file(s) should be placed.");
    registerMessage(MSGID_BACKUPDB_DESCRIPTION_INCREMENTAL,
                    "Indicates whether to generate an incremental backup " +
                    "or a full backup.");
    registerMessage(MSGID_BACKUPDB_DESCRIPTION_INCREMENTAL_BASE_ID,
                    "Specifies the backup ID of the backup against which an " +
                    "incremental backup should be taken.  If none is " +
                    "provided, then the backend will automatically choose an " +
                    "appropriate backup on which to base the incremental " +
                    "backup.");
    registerMessage(MSGID_BACKUPDB_DESCRIPTION_COMPRESS,
                    "Indicates whether the backup file(s) should be " +
                    "compressed.");
    registerMessage(MSGID_BACKUPDB_DESCRIPTION_ENCRYPT,
                    "Indicates whether the backup file(s) should be " +
                    "encrypted.");
    registerMessage(MSGID_BACKUPDB_DESCRIPTION_HASH,
                    "Indicates whether to generate a hash of the backup " +
                    "file(s) so that their integrity can be verified on " +
                    "restore.");
    registerMessage(MSGID_BACKUPDB_DESCRIPTION_SIGN_HASH,
                    "Indicates whether the hash of the archive file(s) " +
                    "should be digitally signed to provide tamper detection.");
    registerMessage(MSGID_BACKUPDB_DESCRIPTION_USAGE,
                    "Displays this usage information.");
    registerMessage(MSGID_BACKUPDB_CANNOT_INITIALIZE_ARGS,
                    "An unexpected error occurred while attempting to " +
                    "initialize the command-line arguments:  %s.");
    registerMessage(MSGID_BACKUPDB_ERROR_PARSING_ARGS,
                    "An error occurred while parsing the command-line " +
                    "arguments:  %s.");
    registerMessage(MSGID_BACKUPDB_SERVER_BOOTSTRAP_ERROR,
                    "An unexpected error occurred while attempting to " +
                    "bootstrap the Directory Server client-side code:  %s.");
    registerMessage(MSGID_BACKUPDB_CANNOT_LOAD_CONFIG,
                    "An error occurred while trying to load the Directory " +
                    "Server configuration:  %s.");
    registerMessage(MSGID_BACKUPDB_CANNOT_LOAD_SCHEMA,
                    "An error occurred while trying to load the Directory " +
                    "Server schema:  %s.");
    registerMessage(MSGID_BACKUPDB_CANNOT_INITIALIZE_CORE_CONFIG,
                    "An error occurred while trying to initialize the core " +
                    "Directory Server configuration:  %s.");
    registerMessage(MSGID_BACKUPDB_CANNOT_INITIALIZE_CRYPTO_MANAGER,
                    "An error occurred while attempting to initialize the " +
                    "crypto manager:  %s.");
    registerMessage(MSGID_BACKUPDB_MULTIPLE_BACKENDS_FOR_ID,
                    "Multiple Directory Server backends are configured with " +
                    "the requested backend ID \"%s\".");
    registerMessage(MSGID_BACKUPDB_NO_BACKENDS_FOR_ID,
                    "None of the Directory Server backends are configured " +
                    "with the requested backend ID \"%s\".");
    registerMessage(MSGID_BACKUPDB_CONFIG_ENTRY_MISMATCH,
                    "The configuration for the backend with backend ID %s is " +
                    "held in entry \"%s\", but other backups in the target " +
                    "backup directory %s were generated from a backend whose " +
                    "configuration was held in configuration entry \"%s\".");
    registerMessage(MSGID_BACKUPDB_INVALID_BACKUP_DIR,
                    "An error occurred while attempting to use the specified " +
                    "path \"%s\" as the target directory for the backup:  %s.");
    registerMessage(MSGID_BACKUPDB_CANNOT_BACKUP,
                    "The target backend %s cannot be backed up using the " +
                    "requested configuration:  %s.");
    registerMessage(MSGID_BACKUPDB_ERROR_DURING_BACKUP,
                    "An error occurred while attempting to back up backend " +
                    "%s with the requested configuration:  %s.");
    registerMessage(MSGID_BACKUPDB_CANNOT_DECODE_BACKEND_BASE_DN,
                    "Unable to decode the backend configuration base DN " +
                    "string \"%s\" as a valid DN:  %s.");
    registerMessage(MSGID_BACKUPDB_CANNOT_RETRIEVE_BACKEND_BASE_ENTRY,
                    "Unable to retrieve the backend configuration base entry " +
                    "\"%s\" from the server configuration:  %s.");
    registerMessage(MSGID_BACKUPDB_CANNOT_DETERMINE_BACKEND_CLASS,
                    "Cannot determine the name of the Java class providing " +
                    "the logic for the backend defined in configuration " +
                    "entry %s:  %s.");
    registerMessage(MSGID_BACKUPDB_CANNOT_LOAD_BACKEND_CLASS,
                    "Unable to load class %s referenced in configuration " +
                    "entry %s for use as a Directory Server backend:  %s.");
    registerMessage(MSGID_BACKUPDB_CANNOT_INSTANTIATE_BACKEND_CLASS,
                    "Unable to create an instance of class %s referenced in " +
                    "configuration entry %s as a Directory Server backend:  " +
                    "%s.");
    registerMessage(MSGID_BACKUPDB_NO_BASES_FOR_BACKEND,
                    "No base DNs have been defined in backend configuration " +
                    "entry %s.  This backend will not be evaluated in the " +
                    "LDIF export process.");
    registerMessage(MSGID_BACKUPDB_CANNOT_DETERMINE_BASES_FOR_BACKEND,
                    "Unable to determine the set of base DNs defined in " +
                    "backend configuration entry %s:  %s.");
    registerMessage(MSGID_BACKUPDB_CANNOT_DETERMINE_BACKEND_ID,
                    "Cannot determine the backend ID for the backend defined " +
                    "in configuration entry %s:  %s.");
    registerMessage(MSGID_BACKUPDB_CANNOT_MIX_BACKUP_ALL_AND_BACKEND_ID,
                    "The %s and %s arguments may not be used together.  " +
                    "Exactly one of them must be provided.");
    registerMessage(MSGID_BACKUPDB_NEED_BACKUP_ALL_OR_BACKEND_ID,
                    "Neither the %s argument nor the %s argument was " +
                    "provided.  Exactly one of them is required.");
    registerMessage(MSGID_BACKUPDB_CANNOT_CREATE_BACKUP_DIR,
                    "An error occurred while attempting to create the backup " +
                    "directory %s:  %s.");
    registerMessage(MSGID_BACKUPDB_BACKUP_NOT_SUPPORTED,
                    "Backend ID %s was included in the set of backends to " +
                    "archive, but this backend does not provide support for " +
                    "a backup mechanism.  It will be skipped.");
    registerMessage(MSGID_BACKUPDB_NO_BACKENDS_TO_ARCHIVE,
                    "None of the target backends provide a backup " +
                    "mechanism.  The backup operation has been aborted.");
    registerMessage(MSGID_BACKUPDB_CANNOT_LOCK_BACKEND,
                    "An error occurred while attempting to acquire a shared " +
                    "lock for backend %s:  %s.  This generally means that " +
                    "some other process has exclusive access to this " +
                    "backend (e.g., a restore or an LDIF import).  This " +
                    "backend will not be archived.");
    registerMessage(MSGID_BACKUPDB_STARTING_BACKUP,
                    "Starting backup for backend %s.");
    registerMessage(MSGID_BACKUPDB_CANNOT_PARSE_BACKUP_DESCRIPTOR,
                    "An error occurred while attempting to parse the backup " +
                    "descriptor file %s:  %s.");
    registerMessage(MSGID_BACKUPDB_CANNOT_UNLOCK_BACKEND,
                    "An error occurred while attempting to release the " +
                    "shared lock for backend %s:  %s.  This lock should " +
                    "automatically be cleared when the backup process exits, " +
                    "so no further action should be required.");
    registerMessage(MSGID_BACKUPDB_COMPLETED_WITH_ERRORS,
                    "The backup process completed with one or more errors.");
    registerMessage(MSGID_BACKUPDB_COMPLETED_SUCCESSFULLY,
                    "The backup process completed successfully.");
    registerMessage(MSGID_BACKUPDB_INCREMENTAL_BASE_REQUIRES_INCREMENTAL,
                    "The use of the %s argument requires that the %s " +
                    "argument is also provided.");
    registerMessage(MSGID_BACKUPDB_SIGN_REQUIRES_HASH,
                    "The use of the %s argument requires that the %s " +
                    "argument is also provided.");


    registerMessage(MSGID_RESTOREDB_DESCRIPTION_CONFIG_CLASS,
                    "Specifies the fully-qualified name of the Java class " +
                    "that serves as the configuration handler for the " +
                    "Directory Server.");
    registerMessage(MSGID_RESTOREDB_DESCRIPTION_CONFIG_FILE,
                    "Specifies the path to the Directory Server " +
                    "configuration file.");
    registerMessage(MSGID_RESTOREDB_DESCRIPTION_BACKEND_ID,
                    "Specifies the backend ID for the backend that should " +
                    "be restored.");
    registerMessage(MSGID_RESTOREDB_DESCRIPTION_BACKUP_ID,
                    "Specifies the backup ID that will be used to identify " +
                    "which backup should be restored.  If this is not " +
                    "provided, then the latest backup in the directory will " +
                    "be restored.");
    registerMessage(MSGID_RESTOREDB_DESCRIPTION_BACKUP_DIR,
                    "Specifies the path to the directory in which the " +
                    "backup file(s) are located.");
    registerMessage(MSGID_RESTOREDB_DESCRIPTION_LIST_BACKUPS,
                    "Indicates that this utility should display a list of " +
                    "the available backups and exit.");
    registerMessage(MSGID_RESTOREDB_DESCRIPTION_VERIFY_ONLY,
                    "Indicates that the contents of the specified backup " +
                    "should be verified to the best of the backend's ability " +
                    "but should not be restored.");
    registerMessage(MSGID_RESTOREDB_DESCRIPTION_USAGE,
                    "Displays this usage information.");
    registerMessage(MSGID_RESTOREDB_CANNOT_INITIALIZE_ARGS,
                    "An unexpected error occurred while attempting to " +
                    "initialize the command-line arguments:  %s.");
    registerMessage(MSGID_RESTOREDB_ERROR_PARSING_ARGS,
                    "An error occurred while parsing the command-line " +
                    "arguments:  %s.");
    registerMessage(MSGID_RESTOREDB_SERVER_BOOTSTRAP_ERROR,
                    "An unexpected error occurred while attempting to " +
                    "bootstrap the Directory Server client-side code:  %s.");
    registerMessage(MSGID_RESTOREDB_CANNOT_LOAD_CONFIG,
                    "An error occurred while trying to load the Directory " +
                    "Server configuration:  %s.");
    registerMessage(MSGID_RESTOREDB_CANNOT_LOAD_SCHEMA,
                    "An error occurred while trying to load the Directory " +
                    "Server schema:  %s.");
    registerMessage(MSGID_RESTOREDB_CANNOT_INITIALIZE_CORE_CONFIG,
                    "An error occurred while trying to initialize the core " +
                    "Directory Server configuration:  %s.");
    registerMessage(MSGID_RESTOREDB_CANNOT_INITIALIZE_CRYPTO_MANAGER,
                    "An error occurred while attempting to initialize the " +
                    "crypto manager:  %s.");
    registerMessage(MSGID_RESTOREDB_CANNOT_READ_BACKUP_DIRECTORY,
                    "An error occurred while attempting to examine the " +
                    "set of backups contained in backup directory %s:  %s.");
    registerMessage(MSGID_RESTOREDB_LIST_BACKUP_ID,
                    "Backup ID:          %s");
    registerMessage(MSGID_RESTOREDB_LIST_BACKUP_DATE,
                    "Backup Date:        %s");
    registerMessage(MSGID_RESTOREDB_LIST_INCREMENTAL,
                    "Is Incremental:     %s");
    registerMessage(MSGID_RESTOREDB_LIST_COMPRESSED,
                    "Is Compressed:      %s");
    registerMessage(MSGID_RESTOREDB_LIST_ENCRYPTED,
                    "Is Encrypted:       %s");
    registerMessage(MSGID_RESTOREDB_LIST_HASHED,
                    "Has Unsigned Hash:  %s");
    registerMessage(MSGID_RESTOREDB_LIST_SIGNED,
                    "Has Signed Hash:    %s");
    registerMessage(MSGID_RESTOREDB_LIST_DEPENDENCIES,
                    "Dependent Upon:     %s");
    registerMessage(MSGID_RESTOREDB_INVALID_BACKUP_ID,
                    "The requested backup ID %s does not exist in %s.");
    registerMessage(MSGID_RESTOREDB_NO_BACKUPS_IN_DIRECTORY,
                    "There are no Directory Server backups contained in " +
                    "%s.");
    registerMessage(MSGID_RESTOREDB_NO_BACKENDS_FOR_DN,
                    "The backups contained in directory %s were taken from " +
                    "a Directory Server backend defined in configuration " +
                    "entry %s but no such backend is available.");
    registerMessage(MSGID_RESTOREDB_CANNOT_RESTORE,
                    "The Directory Server backend configured with backend ID " +
                    "%s does not provide a mechanism for restoring " +
                    "backups.");
    registerMessage(MSGID_RESTOREDB_CANNOT_LOCK_BACKEND,
                    "An error occurred while attempting to acquire an " +
                    "exclusive lock for backend %s:  %s.  This generally " +
                    "means some other process is still using this backend " +
                    "(e.g., it is in use by the Directory Server or a " +
                    "backup or LDIF export is in progress.  The restore " +
                    "cannot continue.");
    registerMessage(MSGID_RESTOREDB_ERROR_DURING_BACKUP,
                    "An unexpected error occurred while attempting to " +
                    "restore backup %s from %s:  %s.");
    registerMessage(MSGID_RESTOREDB_CANNOT_UNLOCK_BACKEND,
                    "An error occurred while attempting to release the " +
                    "exclusive lock for backend %s:  %s.  This lock should " +
                    "automatically be cleared when the restore process " +
                    "exits, so no further action should be required.");
    registerMessage(MSGID_RESTOREDB_CANNOT_DECODE_BACKEND_BASE_DN,
                    "Unable to decode the backend configuration base DN " +
                    "string \"%s\" as a valid DN:  %s.");
    registerMessage(MSGID_RESTOREDB_CANNOT_RETRIEVE_BACKEND_BASE_ENTRY,
                    "Unable to retrieve the backend configuration base entry " +
                    "\"%s\" from the server configuration:  %s.");
    registerMessage(MSGID_RESTOREDB_CANNOT_DETERMINE_BACKEND_CLASS,
                    "Cannot determine the name of the Java class providing " +
                    "the logic for the backend defined in configuration " +
                    "entry %s:  %s.");
    registerMessage(MSGID_RESTOREDB_CANNOT_LOAD_BACKEND_CLASS,
                    "Unable to load class %s referenced in configuration " +
                    "entry %s for use as a Directory Server backend:  %s.");
    registerMessage(MSGID_RESTOREDB_CANNOT_INSTANTIATE_BACKEND_CLASS,
                    "Unable to create an instance of class %s referenced in " +
                    "configuration entry %s as a Directory Server backend:  " +
                    "%s.");
    registerMessage(MSGID_RESTOREDB_NO_BASES_FOR_BACKEND,
                    "No base DNs have been defined in backend configuration " +
                    "entry %s.  This backend will not be evaluated in the " +
                    "LDIF export process.");
    registerMessage(MSGID_RESTOREDB_CANNOT_DETERMINE_BASES_FOR_BACKEND,
                    "Unable to determine the set of base DNs defined in " +
                    "backend configuration entry %s:  %s.");
    registerMessage(MSGID_RESTOREDB_CANNOT_DETERMINE_BACKEND_ID,
                    "Cannot determine the backend ID for the backend defined " +
                    "in configuration entry %s:  %s.");
    registerMessage(MSGID_DESCRIPTION_NOOP,
                    "No-op mode used to show what the tool would do with the " +
                    "given input but not perform any operations.");
    registerMessage(MSGID_DESCRIPTION_TYPES_ONLY,
                    "Specify that the search retrieve only attribute names, " +
                    "not the attribute values.");
    registerMessage(MSGID_LDIF_FILE_CANNOT_OPEN_FOR_READ,
                    "An error occurred while attempting to open the " +
                    "LDIF file %s for reading:  %s.");
    registerMessage(MSGID_LDIF_FILE_READ_ERROR,
                    "An error occurred while attempting to read the contents " +
                    "of LDIF file %s:  %s.");
    registerMessage(MSGID_LDIF_FILE_INVALID_LDIF_ENTRY,
                    "Error at or near line %d in LDIF file %s:  %s.");


    registerMessage(MSGID_STOPDS_DESCRIPTION_HOST,
                    "The address of the Directory Server to shut down.");
    registerMessage(MSGID_STOPDS_DESCRIPTION_PORT,
                    "The port of the Directory Server to shut down.");
    registerMessage(MSGID_STOPDS_DESCRIPTION_USESSL,
                    "Use SSL to communicate with the Directory Server.");
    registerMessage(MSGID_STOPDS_DESCRIPTION_USESTARTTLS,
                    "Use StartTLS to communicate with the Directory Server.");
    registerMessage(MSGID_STOPDS_DESCRIPTION_BINDDN,
                    "The DN to use when performing a simple bind to the " +
                    "Directory Server.");
    registerMessage(MSGID_STOPDS_DESCRIPTION_BINDPW,
                    "The password to use to bind to the Directory Server.");
    registerMessage(MSGID_STOPDS_DESCRIPTION_BINDPWFILE,
                    "The path to a file containing the password to use to " +
                    "bind to the Directory Server.");
    registerMessage(MSGID_STOPDS_DESCRIPTION_SASLOPTIONS,
                    "A SASL option to use for authentication in the form " +
                    "name=value.  At least one SASL option must be given " +
                    "with a name of mech to specify which SASL mechanism to " +
                    "use.  Multiple SASL options may be provided by using " +
                    "multiple instances of this argument.");
    registerMessage(MSGID_STOPDS_DESCRIPTION_PROXYAUTHZID,
                    "Use the proxied authorization control with the provided " +
                    "authorization ID.");
    registerMessage(MSGID_STOPDS_DESCRIPTION_STOP_REASON,
                    "A human-readable reason explaining why the Directory " +
                    "Server is being stopped.");
    registerMessage(MSGID_STOPDS_DESCRIPTION_RESTART,
                    "Attempt to automatically restart the server once it has " +
                    "stopped.");
    registerMessage(MSGID_STOPDS_DESCRIPTION_STOP_TIME,
                    "The time that the Directory Server should be stopped if " +
                    "it should be some time in the future.  The value should " +
                    "be in the form YYYYMMDDhhmmssZ for UTC time or " +
                    "YYYYMMDDhhmmss for local time.");
    registerMessage(MSGID_STOPDS_DESCRIPTION_TRUST_ALL,
                    "Blindly trust all server certificates.");
    registerMessage(MSGID_STOPDS_DESCRIPTION_KSFILE,
                    "The path to the SSL key store file.");
    registerMessage(MSGID_STOPDS_DESCRIPTION_KSPW,
                    "The password needed to access the key store content.");
    registerMessage(MSGID_STOPDS_DESCRIPTION_KSPWFILE,
                    "The path to the file containing the password needed to " +
                    "access the key store content.");
    registerMessage(MSGID_STOPDS_DESCRIPTION_TSFILE,
                    "The path to the SSL trust store file.");
    registerMessage(MSGID_STOPDS_DESCRIPTION_TSPW,
                    "The password needed to access the trust store content.");
    registerMessage(MSGID_STOPDS_DESCRIPTION_TSPWFILE,
                    "The path to the file containing the password needed to " +
                    "access the trust store content.");
    registerMessage(MSGID_STOPDS_DESCRIPTION_SHOWUSAGE,
                    "Display this usage information.");
    registerMessage(MSGID_STOPDS_CANNOT_INITIALIZE_ARGS,
                    "An unexpected error occurred while attempting to " +
                    "initialize the command-line arguments:  %s.");
    registerMessage(MSGID_STOPDS_ERROR_PARSING_ARGS,
                    "An error occurred while parsing the command-line " +
                    "arguments:  %s.");
    registerMessage(MSGID_STOPDS_MUTUALLY_EXCLUSIVE_ARGUMENTS,
                    "ERROR:  You may not provide both the %s and the %s " +
                    "arguments.");
    registerMessage(MSGID_STOPDS_CANNOT_DECODE_STOP_TIME,
                    "ERROR:  Unable to decode the provided stop time.  It " +
                    "should be in the form YYYYMMDDhhmmssZ for UTC time or " +
                    "YYYYMMDDhhmmss for local time.");
    registerMessage(MSGID_STOPDS_CANNOT_INITIALIZE_SSL,
                    "ERROR:  Unable to perform SSL initialization:  %s.");
    registerMessage(MSGID_STOPDS_CANNOT_PARSE_SASL_OPTION,
                    "ERROR:  The provided SASL option string \"%s\" could " +
                    "not be parsed in the form \"name=value\".");
    registerMessage(MSGID_STOPDS_NO_SASL_MECHANISM,
                    "ERROR:  One or more SASL options were provided, but " +
                    "none of them were the \"mech\" option to specify which " +
                    "SASL mechanism should be used.");
    registerMessage(MSGID_STOPDS_CANNOT_DETERMINE_PORT,
                    "ERROR:  Cannot parse the value of the %s argument as " +
                    "an integer value between 1 and 65535:  %s.");
    registerMessage(MSGID_STOPDS_CANNOT_CONNECT,
                    "ERROR:  Cannot establish a connection to the " +
                    "Directory Server:  %s.");
    registerMessage(MSGID_STOPDS_UNEXPECTED_CONNECTION_CLOSURE,
                    "ERROR:  The connection to the Directory Server was " +
                    "unexpectedly closed while waiting for a response to the " +
                    "shutdown request.");
    registerMessage(MSGID_STOPDS_IO_ERROR,
                    "ERROR:  An I/O error occurred while attempting to " +
                    "communicate with the Directory Server:  %s.");
    registerMessage(MSGID_STOPDS_DECODE_ERROR,
                    "ERROR:  An error occurred while trying to decode the " +
                    "response from the server:  %s.");
    registerMessage(MSGID_STOPDS_INVALID_RESPONSE_TYPE,
                    "ERROR:  Expected an add response message but got a %s " +
                    "message instead.");


    registerMessage(MSGID_LDIFSEARCH_DESCRIPTION_LDIF_FILE,
                    "Specifies the LDIF file containing the data to search.  " +
                    "Multiple files may be specified by providing the option " +
                    "multiple times.  If no files are provided, the data " +
                    "will be read from standard input.");
    registerMessage(MSGID_LDIFSEARCH_DESCRIPTION_BASEDN,
                    "The base DN for the search.  Multiple base DNs may be " +
                    "specified by providing the option multiple times.  If " +
                    "no base DN is provided, then the root DSE will be used.");
    registerMessage(MSGID_LDIFSEARCH_DESCRIPTION_SCOPE,
                    "The scope for the search.  It must be one of 'base', " +
                    "'one', 'sub', or 'subordinate'.  If it is not provided, " +
                    "then 'sub' will be used.");
    registerMessage(MSGID_LDIFSEARCH_DESCRIPTION_CONFIG_FILE,
                    "The path to the Directory Server configuration file, " +
                    "which will enable the use of the schema definitions " +
                    "when processing the searches.  If it is not provided, " +
                    "then schema processing will not be available.");
    registerMessage(MSGID_LDIFSEARCH_DESCRIPTION_CONFIG_CLASS,
                    "The fully-qualified name of the Java class to use as " +
                    "the Directory Server configuration handler.  If this is " +
                    "not provided, then a default of " +
                    ConfigFileHandler.class.getName() + " will be used.");
    registerMessage(MSGID_LDIFSEARCH_DESCRIPTION_FILTER_FILE,
                    "The path to the file containing the search filter(s) " +
                    "to use.  If this is not provided, then the filter must " +
                    "be provided on the command line after all configuration " +
                    "options.");
    registerMessage(MSGID_LDIFSEARCH_DESCRIPTION_OUTPUT_FILE,
                    "The path to the output file to which the matching " +
                    "entries should be written.  If this is not provided, " +
                    "then the data will be written to standard output.");
    registerMessage(MSGID_LDIFSEARCH_DESCRIPTION_OVERWRITE_EXISTING,
                    "Indicates that any existing output file should be " +
                    "overwritten rather than appending to it.");
    registerMessage(MSGID_LDIFSEARCH_DESCRIPTION_DONT_WRAP,
                    "Indicates that long lines should not be wrapped.");
    registerMessage(MSGID_LDIFSEARCH_DESCRIPTION_SIZE_LIMIT,
                    "Specifies the maximum number of matching entries to " +
                    "return.");
    registerMessage(MSGID_LDIFSEARCH_DESCRIPTION_TIME_LIMIT,
                    "Specifies the maximum length of time (in seconds) to " +
                    "spend processing.");
    registerMessage(MSGID_LDIFSEARCH_DESCRIPTION_USAGE,
                    "Displays usage information for this program.");
    registerMessage(MSGID_LDIFSEARCH_CANNOT_INITIALIZE_ARGS,
                    "An unexpected error occurred while attempting to " +
                    "initialize the command-line arguments:  %s.");
    registerMessage(MSGID_LDIFSEARCH_ERROR_PARSING_ARGS,
                    "An error occurred while parsing the command-line " +
                    "arguments:  %s.");
    registerMessage(MSGID_LDIFSEARCH_NO_FILTER,
                    "No search filter was specified.  Either a filter file " +
                    "or an individual search filter must be provided.");
    registerMessage(MSGID_LDIFSEARCH_CANNOT_INITIALIZE_JMX,
                    "An error occurred while attempting to initialize the " +
                    "Directory Server JMX subsystem based on the information " +
                    "in configuration file %s:  %s.");
    registerMessage(MSGID_LDIFSEARCH_CANNOT_INITIALIZE_CONFIG,
                    "An error occurred while attempting to process the " +
                    "Directory Server configuration file %s:  %s.");
    registerMessage(MSGID_LDIFSEARCH_CANNOT_INITIALIZE_SCHEMA,
                    "An error occurred while attempting to initialize the " +
                    "Directory Server schema based on the information in " +
                    "configuration file %s:  %s.");
    registerMessage(MSGID_LDIFSEARCH_CANNOT_PARSE_FILTER,
                    "An error occurred while attempting to parse search " +
                    "filter '%s':  %s.");
    registerMessage(MSGID_LDIFSEARCH_CANNOT_PARSE_BASE_DN,
                    "An error occurred while attempting to parse base DN " +
                    "'%s':  %s.");
    registerMessage(MSGID_LDIFSEARCH_CANNOT_PARSE_TIME_LIMIT,
                    "An error occurred while attempting to parse the " +
                    "time limit as an integer:  %s.");
    registerMessage(MSGID_LDIFSEARCH_CANNOT_PARSE_SIZE_LIMIT,
                    "An error occurred while attempting to parse the " +
                    "size limit as an integer:  %s.");
    registerMessage(MSGID_LDIFSEARCH_CANNOT_CREATE_READER,
                    "An error occurred while attempting to create the LDIF " +
                    "reader:  %s.");
    registerMessage(MSGID_LDIFSEARCH_CANNOT_CREATE_WRITER,
                    "An error occurred while attempting to create the LDIF " +
                    "writer used to return matching entries:  %s.");
    registerMessage(MSGID_LDIFSEARCH_TIME_LIMIT_EXCEEDED,
                    "The specified time limit has been exceeded during " +
                    "search processing.");
    registerMessage(MSGID_LDIFSEARCH_SIZE_LIMIT_EXCEEDED,
                    "The specified size limit has been exceeded during " +
                    "search processing.");
    registerMessage(MSGID_LDIFSEARCH_CANNOT_READ_ENTRY_RECOVERABLE,
                    "An error occurred while attempting to read an entry " +
                    "from the LDIF content:  %s.  Skipping this entry and " +
                    "continuing processing.");
    registerMessage(MSGID_LDIFSEARCH_CANNOT_READ_ENTRY_FATAL,
                    "An error occurred while attempting to read an entry " +
                    "from the LDIF content:  %s.  Unable to continue " +
                    "processing.");
    registerMessage(MSGID_LDIFSEARCH_ERROR_DURING_PROCESSING,
                    "An unexpected error occurred during search processing:  " +
                    "%s.");


    registerMessage(MSGID_LDIFDIFF_DESCRIPTION_SOURCE_LDIF,
                    "Specifies the LDIF file to use as the source data.");
    registerMessage(MSGID_LDIFDIFF_DESCRIPTION_TARGET_LDIF,
                    "Specifies the LDIF file to use as the target data.");
    registerMessage(MSGID_LDIFDIFF_DESCRIPTION_OUTPUT_LDIF,
                    "Specifies the file to which the output should be " +
                    "written.");
    registerMessage(MSGID_LDIFDIFF_DESCRIPTION_OVERWRITE_EXISTING,
                    "Indicates that any existing output file should be " +
                    "overwritten rather than appending to it.");
    registerMessage(MSGID_LDIFDIFF_DESCRIPTION_CONFIG_FILE,
                    "The path to the Directory Server configuration file, " +
                    "which will enable the use of the schema definitions " +
                    "when processing the LDIF data.  If it is not provided, " +
                    "then schema processing will not be available.");
    registerMessage(MSGID_LDIFDIFF_DESCRIPTION_CONFIG_CLASS,
                    "The fully-qualified name of the Java class to use as " +
                    "the Directory Server configuration handler.  If this is " +
                    "not provided, then a default of " +
                    ConfigFileHandler.class.getName() + " will be used.");
    registerMessage(MSGID_LDIFDIFF_DESCRIPTION_USAGE,
                    "Displays usage information for this program.");
    registerMessage(MSGID_LDIFDIFF_CANNOT_INITIALIZE_ARGS,
                    "An unexpected error occurred while attempting to " +
                    "initialize the command-line arguments:  %s.");
    registerMessage(MSGID_LDIFDIFF_ERROR_PARSING_ARGS,
                    "An error occurred while parsing the command-line " +
                    "arguments:  %s.");
    registerMessage(MSGID_LDIFDIFF_CANNOT_INITIALIZE_JMX,
                    "An error occurred while attempting to initialize the " +
                    "Directory Server JMX subsystem based on the information " +
                    "in configuration file %s:  %s.");
    registerMessage(MSGID_LDIFDIFF_CANNOT_INITIALIZE_CONFIG,
                    "An error occurred while attempting to process the " +
                    "Directory Server configuration file %s:  %s.");
    registerMessage(MSGID_LDIFDIFF_CANNOT_INITIALIZE_SCHEMA,
                    "An error occurred while attempting to initialize the " +
                    "Directory Server schema based on the information in " +
                    "configuration file %s:  %s.");
    registerMessage(MSGID_LDIFDIFF_CANNOT_OPEN_SOURCE_LDIF,
                    "An error occurred while attempting to open source LDIF " +
                    "%s:  %s.");
    registerMessage(MSGID_LDIFDIFF_ERROR_READING_SOURCE_LDIF,
                    "An error occurred while reading the contents of source " +
                    "LDIF %s:  %s.");
    registerMessage(MSGID_LDIFDIFF_CANNOT_OPEN_TARGET_LDIF,
                    "An error occurred while attempting to open target LDIF " +
                    "%s:  %s.");
    registerMessage(MSGID_LDIFDIFF_ERROR_READING_TARGET_LDIF,
                    "An error occurred while reading the contents of target " +
                    "LDIF %s:  %s.");
    registerMessage(MSGID_LDIFDIFF_CANNOT_OPEN_OUTPUT,
                    "An error occurred while attempting to open the LDIF " +
                    "writer for the diff output:  %s.");
    registerMessage(MSGID_LDIFDIFF_NO_DIFFERENCES,
                    "No differences were detected between the source and " +
                    "target LDIF files.");
    registerMessage(MSGID_LDIFDIFF_ERROR_WRITING_OUTPUT,
                    "An error occurred while attempting to write the diff " +
                    "output:  %s.");


    registerMessage(MSGID_CONFIGDS_DESCRIPTION_CONFIG_FILE,
                    "The path to the Directory Server configuration file.");
    registerMessage(MSGID_CONFIGDS_DESCRIPTION_CONFIG_CLASS,
                    "The fully-qualified name of the Java class to use as " +
                    "the Directory Server configuration handler.  If this is " +
                    "not provided, then a default of " +
                    ConfigFileHandler.class.getName() + " will be used.");
    registerMessage(MSGID_CONFIGDS_DESCRIPTION_LDAP_PORT,
                    "Specifies the port on which the Directory Server should " +
                    "listen for LDAP communication.");
    registerMessage(MSGID_CONFIGDS_DESCRIPTION_BASE_DN,
                    "Specifies the base DN for user information in the " +
                    "Directory Server.  Multiple base DNs may be provided " +
                    "by using this option multiple times.");
    registerMessage(MSGID_CONFIGDS_DESCRIPTION_ROOT_DN,
                    "Specifies the DN for the initial root user for the " +
                    "Directory Server.");
    registerMessage(MSGID_CONFIGDS_DESCRIPTION_ROOT_PW,
                    "Specifies the password for the initial root user for " +
                    "the Directory Server.");
    registerMessage(MSGID_CONFIGDS_DESCRIPTION_ROOT_PW_FILE,
                    "Specifies the path to a file containing the password " +
                    "for the initial root user for the Directory Server.");
    registerMessage(MSGID_CONFIGDS_DESCRIPTION_USAGE,
                    "Displays usage information for this program.");
    registerMessage(MSGID_CONFIGDS_CANNOT_INITIALIZE_ARGS,
                    "An unexpected error occurred while attempting to " +
                    "initialize the command-line arguments:  %s.");
    registerMessage(MSGID_CONFIGDS_ERROR_PARSING_ARGS,
                    "An error occurred while parsing the command-line " +
                    "arguments:  %s.");
    registerMessage(MSGID_CONFIGDS_CANNOT_ACQUIRE_SERVER_LOCK,
                    "An error occurred while attempting to acquire the " +
                    "server-wide lock file %s:  %s.  This generally means " +
                    "that the Directory Server is running, or another tool " +
                    "that requires exclusive access to the server is in use.");
    registerMessage(MSGID_CONFIGDS_CANNOT_INITIALIZE_JMX,
                    "An error occurred while attempting to initialize the " +
                    "Directory Server JMX subsystem based on the information " +
                    "in configuration file %s:  %s.");
    registerMessage(MSGID_CONFIGDS_CANNOT_INITIALIZE_CONFIG,
                    "An error occurred while attempting to process the " +
                    "Directory Server configuration file %s:  %s.");
    registerMessage(MSGID_CONFIGDS_CANNOT_INITIALIZE_SCHEMA,
                    "An error occurred while attempting to initialize the " +
                    "Directory Server schema based on the information in " +
                    "configuration file %s:  %s.");
    registerMessage(MSGID_CONFIGDS_CANNOT_PARSE_BASE_DN,
                    "An error occurred while attempting to parse base DN " +
                    "value \"%s\" as a DN:  %s.");
    registerMessage(MSGID_CONFIGDS_CANNOT_PARSE_ROOT_DN,
                    "An error occurred while attempting to parse root DN " +
                    "value \"%s\" as a DN:  %s.");
    registerMessage(MSGID_CONFIGDS_NO_ROOT_PW,
                    "The DN for the initial root user was provided, but no " +
                    "corresponding password was given.  If the root DN is " +
                    "specified then the password must also be provided.");
    registerMessage(MSGID_CONFIGDS_CANNOT_UPDATE_BASE_DN,
                    "An error occurred while attempting to update the base " +
                    "DN(s) for user data in the Directory Server:  %s.");
    registerMessage(MSGID_CONFIGDS_CANNOT_UPDATE_LDAP_PORT,
                    "An error occurred while attempting to update the port " +
                    "on which to listen for LDAP communication:  %s.");
    registerMessage(MSGID_CONFIGDS_CANNOT_UPDATE_ROOT_USER,
                    "An error occurred while attempting to update the entry " +
                    "for the initial Directory Server root user:  %s.");
    registerMessage(MSGID_CONFIGDS_CANNOT_WRITE_UPDATED_CONFIG,
                    "An error occurred while writing the updated Directory " +
                    "Server configuration:  %s.");
    registerMessage(MSGID_CONFIGDS_NO_CONFIG_CHANGES,
                    "ERROR:  No configuration changes were specified.");
    registerMessage(MSGID_CONFIGDS_WROTE_UPDATED_CONFIG,
                    "Successfully wrote the updated Directory Server " +
                    "configuration.");


    registerMessage(MSGID_INSTALLDS_DESCRIPTION_TESTONLY,
                    "Just verify that the JVM can be started properly.");
    registerMessage(MSGID_INSTALLDS_DESCRIPTION_PROGNAME,
                    "The setup command used to invoke this program.");
    registerMessage(MSGID_INSTALLDS_DESCRIPTION_CONFIG_FILE,
                    "The path to the Directory Server configuration file.");
    registerMessage(MSGID_INSTALLDS_DESCRIPTION_CONFIG_CLASS,
                    "The fully-qualified name of the Java class to use as " +
                    "the Directory Server configuration handler.  If this is " +
                    "not provided, then a default of " +
                    ConfigFileHandler.class.getName() + " will be used.");
    registerMessage(MSGID_INSTALLDS_DESCRIPTION_SILENT,
                    "Perform a silent installation.");
    registerMessage(MSGID_INSTALLDS_DESCRIPTION_BASEDN,
                    "Specifies the base DN for user information in the " +
                    "Directory Server.  Multiple base DNs may be provided " +
                    "by using this option multiple times.");
    registerMessage(MSGID_INSTALLDS_DESCRIPTION_ADDBASE,
                    "Indicates whether to create the base entry in the " +
                    "Directory Server database.");
    registerMessage(MSGID_INSTALLDS_DESCRIPTION_IMPORTLDIF,
                    "Specifies the path to an LDIF file containing data that " +
                    "should be added to the Directory Server database.  " +
                    "Multiple LDIF files may be provided by using this " +
                    "option multiple times.");
    registerMessage(MSGID_INSTALLDS_DESCRIPTION_LDAPPORT,
                    "Specifies the port on which the Directory Server should " +
                    "listen for LDAP communication.");
    registerMessage(MSGID_INSTALLDS_DESCRIPTION_SKIPPORT,
                    "Skip the check to determine whether the specified LDAP " +
                    "port is usable.");
    registerMessage(MSGID_INSTALLDS_DESCRIPTION_ROOTDN,
                    "Specifies the DN for the initial root user for the " +
                    "Directory Server.");
    registerMessage(MSGID_INSTALLDS_DESCRIPTION_ROOTPW,
                    "Specifies the password for the initial root user for " +
                    "the Directory Server.");
    registerMessage(MSGID_INSTALLDS_DESCRIPTION_ROOTPWFILE,
                    "Specifies the path to a file containing the password " +
                    "for the initial root user for the Directory Server.");
    registerMessage(MSGID_INSTALLDS_DESCRIPTION_HELP,
                    "Displays usage information for this program.");
    registerMessage(MSGID_INSTALLDS_NO_CONFIG_FILE,
                    "ERROR:  No configuration file path was provided (use " +
                    "the %s argument).");
    registerMessage(MSGID_INSTALLDS_INITIALIZING,
                    "Please wait while the setup program initializes....");
    registerMessage(MSGID_INSTALLDS_CANNOT_INITIALIZE_JMX,
                    "An error occurred while attempting to initialize the " +
                    "Directory Server JMX subsystem based on the information " +
                    "in configuration file %s:  %s.");
    registerMessage(MSGID_INSTALLDS_CANNOT_INITIALIZE_CONFIG,
                    "An error occurred while attempting to process the " +
                    "Directory Server configuration file %s:  %s.");
    registerMessage(MSGID_INSTALLDS_CANNOT_INITIALIZE_SCHEMA,
                    "An error occurred while attempting to initialize the " +
                    "Directory Server schema based on the information in " +
                    "configuration file %s:  %s.");
    registerMessage(MSGID_INSTALLDS_CANNOT_PARSE_DN,
                    "An error occurred while attempting to parse the string " +
                    "\"%s\" as a valid DN:  %s.");
    registerMessage(MSGID_INSTALLDS_PROMPT_BASEDN,
                    "What do you wish to use as the base DN for the " +
                    "directory data?");
    registerMessage(MSGID_INSTALLDS_PROMPT_IMPORT,
                    "Do you wish to populate the directory database with " +
                    "information from an existing LDIF file?");
    registerMessage(MSGID_INSTALLDS_PROMPT_IMPORT_FILE,
                    "Please specify the path to the LDIF file containing " +
                    "the data to import.");
    registerMessage(MSGID_INSTALLDS_TWO_CONFLICTING_ARGUMENTS,
                    "ERROR:  You may not provide both the %s and the %s " +
                    "arguments at the same time.");
    registerMessage(MSGID_INSTALLDS_PROMPT_ADDBASE,
                    "Would you like to have the base %s entry automatically " +
                    "created in the directory database?");
    registerMessage(MSGID_INSTALLDS_PROMPT_LDAPPORT,
                    "On which port would you like the Directory Server to " +
                    "accept connections from LDAP clients?");
    registerMessage(MSGID_INSTALLDS_CANNOT_BIND_TO_PRIVILEGED_PORT,
                    "ERROR:  Unable to bind to port %d:  %s.  This port may " +
                    "already be in use, or if you are a nonroot user then " +
                    "you may not be allowed to use port numbers 1024 or " +
                    "below.");
    registerMessage(MSGID_INSTALLDS_CANNOT_BIND_TO_PORT,
                    "ERROR:  Unable to bind to port %d:  %s.  This port may " +
                    "already be in use, or you may not have permission to " +
                    "bind to it.");
    registerMessage(MSGID_INSTALLDS_PROMPT_ROOT_DN,
                    "What would you like to use as the initial root user DN " +
                    "for the Directory Server?");
    registerMessage(MSGID_INSTALLDS_NO_ROOT_PASSWORD,
                    "ERROR:  No password was provided for the initial root "+
                    "user.  When performing a silent installation, this must " +
                    "be provided using either the %s or the %s argument.");
    registerMessage(MSGID_INSTALLDS_PROMPT_ROOT_PASSWORD,
                    "Please provide the password to use for the initial root " +
                    "user");
    registerMessage(MSGID_INSTALLDS_PROMPT_CONFIRM_ROOT_PASSWORD,
                    "Please re-enter the password for confirmation");
    registerMessage(MSGID_INSTALLDS_STATUS_CONFIGURING_DS,
                    "Applying the requested configuration to the " +
                    "Directory Server....");
    registerMessage(MSGID_INSTALLDS_STATUS_CREATING_BASE_LDIF,
                    "Creating a temporary LDIF file with the initial base " +
                    "entry contents....");
    registerMessage(MSGID_INSTALLDS_CANNOT_CREATE_BASE_ENTRY_LDIF,
                    "An error occurred while attempting to create the " +
                    "base LDIF file:  %s.");
    registerMessage(MSGID_INSTALLDS_STATUS_IMPORTING_LDIF,
                    "Importing the LDIF data into the Directory Server " +
                    "database....");
    registerMessage(MSGID_INSTALLDS_IMPORT_SUCCESSFUL,
                    "Import complete.");
    registerMessage(MSGID_INSTALLDS_STATUS_SUCCESS,
                    "The " + SHORT_NAME + " setup process has completed " +
                    "successfully.");
    registerMessage(MSGID_INSTALLDS_PROMPT_VALUE_YES, "yes");
    registerMessage(MSGID_INSTALLDS_PROMPT_VALUE_NO, "no");
    registerMessage(MSGID_INSTALLDS_INVALID_YESNO_RESPONSE,
                    "ERROR:  The provided value could not be interpreted as " +
                    "a yes or no response.  Please enter a response of " +
                    "either \"yes\" or \"no\".");
    registerMessage(MSGID_INSTALLDS_INVALID_INTEGER_RESPONSE,
                    "ERROR:  The provided response could not be interpreted " +
                    "as an integer.  Please provide the repsonse as an " +
                    "integer value.");
    registerMessage(MSGID_INSTALLDS_INTEGER_BELOW_LOWER_BOUND,
                    "ERROR:  The provided value is less than the lowest " +
                    "allowed value of %d.");
    registerMessage(MSGID_INSTALLDS_INTEGER_ABOVE_UPPER_BOUND,
                    "ERROR:  The provided value is greater than the largest " +
                    "allowed value of %d.");
    registerMessage(MSGID_INSTALLDS_INVALID_DN_RESPONSE,
                    "ERROR:  The provided response could not be interpreted " +
                    "as an LDAP DN.");
    registerMessage(MSGID_INSTALLDS_INVALID_STRING_RESPONSE,
                    "ERROR:  The response value may not be an empty string.");
    registerMessage(MSGID_INSTALLDS_INVALID_PASSWORD_RESPONSE,
                    "ERROR:  The password value may not be an empty string.");
    registerMessage(MSGID_INSTALLDS_PASSWORDS_DONT_MATCH,
                    "ERROR:  The provided password values do not match.");
    registerMessage(MSGID_INSTALLDS_ERROR_READING_FROM_STDIN,
                    "ERROR:  Unexpected failure while reading from standard " +
                    "input:  %s.");
  }
}

