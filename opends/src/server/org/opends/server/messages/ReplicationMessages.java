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
 *      Portions Copyright 2006-2007 Sun Microsystems, Inc.
 */
package org.opends.server.messages;

import static org.opends.server.messages.MessageHandler.*;

/**
 * This class defines the set of message IDs and default format strings for
 * messages associated with the Replication.
 */
public class ReplicationMessages {

  /**
   * Name used to store attachment of historical information in the
   * operation.
   */
  public static final String HISTORICAL = "ds-synch-historical";

  /**
   * Invalid DN.
   */
  public static final int MSGID_SYNC_INVALID_DN =
       CATEGORY_MASK_SYNC | SEVERITY_MASK_MILD_ERROR | 1;

  /**
   * Invalid Changelog Server.
   */
  public static final int MSGID_INVALID_CHANGELOG_SERVER =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_MILD_ERROR | 4;

  /**
   * Unknown hostname.
   */
  public static final int MSGID_UNKNOWN_HOSTNAME =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_MILD_ERROR | 5;

  /**
   * Could not connect to any changelog server.
   */
  public static final int MSGID_COULD_NOT_BIND_CHANGELOG =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_MILD_ERROR | 6;

  /**
   * Unknown Operation type.
   */
  public static final int MSGID_UNKNOWN_TYPE =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_MILD_ERROR | 7;

  /**
   * Error while replaying an operation.
   */
  public static final int MSGID_ERROR_REPLAYING_OPERATION =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_MILD_ERROR | 8;

  /**
   * Operation was not found in Pending List during Post-Operation processing.
   */
  public static final int MSGID_OPERATION_NOT_FOUND_IN_PENDING =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_MILD_ERROR | 9;

  /**
   * Unable to open changelog database.
   */
  public static final int MSGID_COULD_NOT_INITIALIZE_DB =
   CATEGORY_MASK_SYNC | SEVERITY_MASK_MILD_ERROR | 10;

  /**
   * Unable to read changelog database.
   */
  public static final int MSGID_COULD_NOT_READ_DB =
   CATEGORY_MASK_SYNC | SEVERITY_MASK_MILD_ERROR | 11;

  /**
   * Exception while replaying an operation.
   */
  public static final int MSGID_EXCEPTION_REPLAYING_OPERATION =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_MILD_ERROR | 12;

  /**
   * Need to have a Changelog port.
   */
  public static final int MSGID_NEED_CHANGELOG_PORT =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_MILD_ERROR | 13;

  /**
   * Error while updating the ruv.
   */
  public static final int MSGID_ERROR_UPDATING_RUV =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_MILD_ERROR | 14;

  /**
   * Error while searching the ruv.
   */
  public static final int MSGID_ERROR_SEARCHING_RUV =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_MILD_ERROR | 15;

  /**
   * A server disconnected from the changelog server.
   * (this is an informational message)
   */
  public static final int MSGID_SERVER_DISCONNECT =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_NOTICE | 16;

  /**
   * There is no server listening on this host:port.
   * (this is an informational message)
   */
  public static final int MSGID_NO_CHANGELOG_SERVER_LISTENING =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_NOTICE | 17;

  /**
   * Tried to connect to a changelog server that does not have
   * all the changes that we have.
   * Try another one.
   */
  public static final int MSGID_CHANGELOG_MISSING_CHANGES =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_NOTICE | 18;

  /**
   * Only one changelog server is configured.
   * If this server fails the LDAP server will not be able to
   * process updates anymore.
   */
  public static final int MSGID_NEED_MORE_THAN_ONE_CHANGELOG_SERVER =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_NOTICE | 19;

  /**
   * An Exception happened during connection to a changelog server.
   */
  public static final int MSGID_EXCEPTION_STARTING_SESSION =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_NOTICE | 20;

  /**
   * The internal search operation used to find old changes
   * caused an error.
   */
  public static final int MSGID_CANNOT_RECOVER_CHANGES =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_NOTICE | 21;

  /**
   * When trying to find a changelog sever, it was detected that
   * none of the Changelog server has seen all the operations
   * that this server has already processed.
   */
  public static final int MSGID_COULD_NOT_FIND_CHANGELOG_WITH_MY_CHANGES =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_NOTICE | 22;

  /**
   * Could not find any working changelog server.
   */
  public static final int MSGID_COULD_NOT_FIND_CHANGELOG =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_NOTICE | 23;

  /**
   * Exception closing changelog database.
   */
  public static final int MSGID_EXCEPTION_CLOSING_DATABASE =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_NOTICE | 24;

  /**
   * Error Decoding message during operation replay.
   */
  public static final int MSGID_EXCEPTION_DECODING_OPERATION =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 25;

  /**
   * Database Exception in the Chanlog service causing the
   * changelog to shutdown.
   */
  public static final int MSGID_CHANGELOG_SHUTDOWN_DATABASE_ERROR =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_FATAL_ERROR | 26;

  /**
   * Database Exception in the Chanlog service causing the
   * changelog to shutdown.
   */
  public static final int MSGID_IGNORE_BAD_DN_IN_DATABASE_IDENTIFIER =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 27;

  /**
   * Database Exception closing the Changelog Environement.
   */
  public static final int MSGID_ERROR_CLOSING_CHANGELOG_ENV =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 28;

  /**
   * Exception during the database trimming or flush.
   */
  public static final int MSGID_EXCEPTION_CHANGELOG_TRIM_FLUSH =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 29;

  /**
   * Error processing changelog message.
   */
  public static final int MSGID_CHANGELOG_CONNECTION_ERROR =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 30;

  /**
   * Remote server has sent an unknown message.
   */
  public static final int MSGID_UNKNOWN_MESSAGE =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 31;

  /**
   * Remote server has sent an unknown message.
   */
  public static final int MSGID_WRITER_UNEXPECTED_EXCEPTION =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 32;

  /**
   * Remote server has sent an unknown message.
   */
  public static final int MSGID_CHANGELOG_ERROR_SENDING_ACK =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 33;

  /**
   * Exception while receiving a message.
   */
  public static final int MSGID_EXCEPTION_RECEIVING_REPLICATION_MESSAGE =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 34;

  /**
   * Loop detected while replaying an operation.  This message takes one
   * string argument containing details of the operation that could not be
   * replayed.
   */
  public static final int MSGID_LOOP_REPLAYING_OPERATION =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_MILD_ERROR | 35;

  /**
   * Failure when test existence or try to create directory
   * for the changelog database.  This message takes one
   * string argument containing details of the exception
   * and path of the directory.
   */
  public static final int MSGID_FILE_CHECK_CREATE_FAILED =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_MILD_ERROR | 36;

  /**
   * The message ID for the description of the attribute used to specify the
   * list of other Changelog Servers in the Changelog Server
   * Configuration.
   */
  public static final int MSGID_CHANGELOG_SERVER_ATTR =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_INFORMATIONAL | 37;

  /**
   * The message ID for the description of the attribute used to specify
   * the identifier of the Changelog Server.
   */
  public static final int MSGID_SERVER_ID_ATTR =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_INFORMATIONAL | 38;

  /**
   * The message id for the description of the attribute used to specify
   * the port number of the Changelog Server.
   */
  public static final int MSGID_CHANGELOG_PORT_ATTR =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_INFORMATIONAL | 39;

  /**
   * The message id for the description of the attribute used to specify
   * the receive Window Size used by a Changelog Server.
   */
  public static final int MSGID_WINDOW_SIZE_ATTR =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_INFORMATIONAL | 40;

  /**
   * The message id for thedescription of the  attribute used to specify
   * the maximum queue size used by a Changelog Server.
   */
  public static final int MSGID_QUEUE_SIZE_ATTR =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_INFORMATIONAL | 41;

  /**
   * The message id for the Attribute used to specify the directory where the
   * persistent storage of the Changelog server will be saved.
   */
  public static final int MSGID_CHANGELOG_DIR_PATH_ATTR =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_INFORMATIONAL | 42;

  /**
   * The message id for the description of the attribute used to configure
   * the purge delay of the Changelog Servers.
   */
  public static final int MSGID_PURGE_DELAY_ATTR =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_INFORMATIONAL | 43;

  /**
   * The message id for the error raised when export/import
   * is rejected due to an export/import already in progress.
   */
  public static final int MSGID_SIMULTANEOUS_IMPORT_EXPORT_REJECTED =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 44;

  /**
   * The message id for the error raised when import
   * is rejected due to an invalid source of data imported.
   */
  public static final int MSGID_INVALID_IMPORT_SOURCE =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 45;

  /**
   * The message id for the error raised when export
   * is rejected due to an invalid target to export datas.
   */
  public static final int MSGID_INVALID_EXPORT_TARGET =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 46;

  /**
   * The message id for the error raised when import/export message
   * cannot be routed to an up-and-running target in the domain.
   */
  public static final int MSGID_NO_REACHABLE_PEER_IN_THE_DOMAIN =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 47;

  /**
   * The message ID for the message that will be used when no domain
   * can be found matching the provided domain base DN.
   */
  public static final int  MSGID_NO_MATCHING_DOMAIN =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 48;

  /**
   * The message ID for the message that will be used when no domain
   * can be found matching the provided domain base DN.
   */
  public static final int  MSGID_MULTIPLE_MATCHING_DOMAIN
       = CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 49;


  /**
   * The message ID for the message that will be used when the domain
   * belongs to a provider class that does not allow the export.
   */
  public static final int  MSGID_INVALID_PROVIDER =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 50;

  /**
   * The message ID for the message that will be used when
   * a replication server hostname cannot be resolved as an IP address.
   */
  public static final int  MSGID_COULD_NOT_SOLVE_HOSTNAME =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 51;

  /**
   * A replication server received a null messsage from
   * another server.
   */
  public static final int MSGID_READER_NULL_MSG =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_NOTICE | 52;

  /**
   * A server disconnected from the replication server.
   * (this is an informational message)
   */
  public static final int MSGID_READER_EXCEPTION =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_NOTICE | 53;

  /**
   * A replication server received a null messsage from
   * another server.
   */
  public static final int MSGID_DUPLICATE_SERVER_ID =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 54;

  /**
   * A server disconnected from the replication server.
   * (this is an informational message)
   */
  public static final int MSGID_DUPLICATE_REPLICATION_SERVER_ID =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 55;

  /**
   * Some bad historical information was found in an entry.
   */
  public static final int MSGID_BAD_HISTORICAL =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 56;

  /**
   * Could not add the conflict attribute to an entry after a conflict was
   * deteceted.
   */
  public static final int MSGID_CANNOT_ADD_CONFLICT_ATTRIBUTE =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 57;

  /**
   * Could not rename a conflicting entry.
   */
  public static final int MSGID_CANNOT_RENAME_CONFLICT_ENTRY =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 58;

  /**
   * Exception during rename of a conflicting entry.
   */
  public static final int MSGID_EXCEPTION_RENAME_CONFLICT_ENTRY =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 59;

  /**
   * The JVM does not support UTF8. This is required to serialize
   * the changes and store them in the database.
   */
  public static final int MSGID_CHANGELOG_UNSUPPORTED_UTF8_ENCODING =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 60;

  /**
   * An update operation is aborted on error because the replication
   * is defined but the replicationDomain could not contact any
   * of the ReplicationServer.
   */
  public static final int MSGID_REPLICATION_COULD_NOT_CONNECT =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 61;

  /**
   * After a failure to connect to any replication server the
   * replication was finally able to connect.
   */
  public static final int MSGID_NOW_FOUND_CHANGELOG =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_NOTICE | 62;


  /**
   * The connection to the curent Replication Server has failed.
   */
  public static final int MSGID_DISCONNECTED_FROM_CHANGELOG =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_NOTICE | 63;

  /**
   * An error happened to send a ReplServerInfoMessage to another
   * replication server.
   */
  public static final int MSGID_CHANGELOG_ERROR_SENDING_INFO =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 64;

  /**
   * An error happened to send an ErrorMessage to another
   * replication server.
   */
  public static final int MSGID_CHANGELOG_ERROR_SENDING_ERROR =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 65;

  /**
   * An error happened to send a Message (probably a RoutableMessage)
   * to another replication server.
   */
  public static final int MSGID_CHANGELOG_ERROR_SENDING_MSG =
    CATEGORY_MASK_SYNC | SEVERITY_MASK_SEVERE_ERROR | 66;

  /**
   * Register the messages from this class in the core server.
   *
   */
  public static void registerMessages()
  {
    registerMessage(MSGID_SYNC_INVALID_DN,
       "The configured DN is already used by another domain");
    registerMessage(MSGID_INVALID_CHANGELOG_SERVER,
        "Invalid replication server configuration");
    registerMessage(MSGID_UNKNOWN_HOSTNAME,
        "Changelog failed to start because the hostname is unknown");
    registerMessage(MSGID_COULD_NOT_BIND_CHANGELOG,
        "Changelog failed to start :" +
        " could not bind to the changelog listen port : %d. Error : %s");
    registerMessage(MSGID_UNKNOWN_TYPE,
        "Unknown operation type : %s");
    registerMessage(MSGID_ERROR_REPLAYING_OPERATION,
        "Error %s when replaying operation with changenumber %s %s : %s");
    registerMessage(MSGID_OPERATION_NOT_FOUND_IN_PENDING,
        "Internal Error : Operation %s change number %s" +
        " was not found in pending list");
    registerMessage(MSGID_COULD_NOT_INITIALIZE_DB,
        "Changelog failed to start " +
        "because the database %s could not be opened");
    registerMessage(MSGID_COULD_NOT_READ_DB,
        "Changelog failed to start " +
        "because the database %s could not be read");
    registerMessage(MSGID_EXCEPTION_REPLAYING_OPERATION,
         "An Exception was caught while replaying operation %s : %s");
    registerMessage(MSGID_NEED_CHANGELOG_PORT,
         "The replication server port must be defined");
    registerMessage(MSGID_ERROR_UPDATING_RUV,
         "Error %s when updating server state %s : %s base dn : %s");
    registerMessage(MSGID_ERROR_SEARCHING_RUV,
         "Error %s when searching for server state %s : %s base dn : %s");
    registerMessage(MSGID_SERVER_DISCONNECT,
         "%s has disconnected from this replication server");
    registerMessage(MSGID_NO_CHANGELOG_SERVER_LISTENING,
         "There is no replication server listening on %s");
    registerMessage(MSGID_CHANGELOG_MISSING_CHANGES,
        "The replication server %s is missing some changes that this server" +
        " has already processed");
    registerMessage(MSGID_NEED_MORE_THAN_ONE_CHANGELOG_SERVER,
        "More than one replication server should be configured");
    registerMessage(MSGID_EXCEPTION_STARTING_SESSION,
        "Caught Exception during initial communication with " +
        "replication server : ");
    registerMessage(MSGID_CANNOT_RECOVER_CHANGES,
        "Error when searching old changes from the database. ");
    registerMessage(
        MSGID_COULD_NOT_FIND_CHANGELOG_WITH_MY_CHANGES,
        "Could not find a replication server that has seen all the local" +
        " changes. Going to replay changes");
    registerMessage(MSGID_COULD_NOT_FIND_CHANGELOG,
        "Could not connect to any replication server on suffix %s, "
        +"retrying...");
    registerMessage(MSGID_EXCEPTION_CLOSING_DATABASE,
        "Error closing changelog database %s : ");
    registerMessage(MSGID_EXCEPTION_DECODING_OPERATION,
        "Error trying to replay %s, operation could not be decoded : ");
    registerMessage(MSGID_CHANGELOG_SHUTDOWN_DATABASE_ERROR,
        "Error Trying to use the underlying database. " +
        "The Changelog Service is going to shut down. ");
    registerMessage(MSGID_IGNORE_BAD_DN_IN_DATABASE_IDENTIFIER,
        "A badly formatted DN was found in the list of database known " +
        "By this changelog service :%s. This Identifier will be ignored. ");
    registerMessage(MSGID_ERROR_CLOSING_CHANGELOG_ENV,
        "Error closing the changelog database : ");
    registerMessage(MSGID_EXCEPTION_CHANGELOG_TRIM_FLUSH,
        "Error during the changelog database trimming or flush process." +
        " The Changelog service is going to shutdown. ");
    registerMessage(MSGID_CHANGELOG_CONNECTION_ERROR,
        "Error during Changelog service message processing ." +
        " Connection from %s is rejected. ");
    registerMessage(MSGID_UNKNOWN_MESSAGE,
        "%s has sent an unknown message. Closing the connection. ");
    registerMessage(MSGID_WRITER_UNEXPECTED_EXCEPTION,
        "An unexpected error happened handling connection with %s." +
        "This connection is going to be closed. ");
    registerMessage(MSGID_CHANGELOG_ERROR_SENDING_ACK,
        "An unexpected error occurred  while sending an ack to %s." +
        "This connection is going to be closed and reopened. ");
    registerMessage(
        MSGID_EXCEPTION_RECEIVING_REPLICATION_MESSAGE,
        "An Exception was caught while receiving replication message : %s");
    registerMessage(MSGID_LOOP_REPLAYING_OPERATION,
        "A loop was detected while replaying operation: %s");
    registerMessage(MSGID_FILE_CHECK_CREATE_FAILED,
        "An Exception was caught while testing existence or trying " +
        " to create the directory for the changelog database : %s");
    registerMessage(MSGID_CHANGELOG_SERVER_ATTR,
        "Specifies the list of replication servers to which this" +
        " replication server should connect. Each value of this attribute" +
        " should contain a values build with the hostname and the port" +
        " number of the remote server separated with a \":\"");
    registerMessage(MSGID_SERVER_ID_ATTR,
        "Specifies the server ID. Each replication server in the topology" +
        " Must be assigned a unique server ID in the topology");
    registerMessage(MSGID_CHANGELOG_PORT_ATTR,
        "Specifies the port number that the replication server will use to" +
        " listen for connections from LDAP servers");
    registerMessage(MSGID_WINDOW_SIZE_ATTR,
        "Specifies the receive window size of the replication server");
    registerMessage(MSGID_QUEUE_SIZE_ATTR,
        "Specifies the receive queue size of the replication server." +
        " The replication servers will queue up to this number of messages" +
        " in its memory queue and save the older messages to persistent" +
        " storage. Using a larger size may improve performances when" +
        " The replication delay is larger than this size but at the cost" +
        " of using more memory");
    registerMessage(MSGID_CHANGELOG_DIR_PATH_ATTR,
        "Specifies the replication server directory. The replication server" +
        " will create all persistent storage below this path");
    registerMessage(MSGID_PURGE_DELAY_ATTR,
        "Specifies the Changelog Purge Delay, The replication servers will" +
        " keep all changes up to this amount of time before deleting them." +
        " This values defines the maximum age of a backup that can be" +
        " restored because replication servers would not be able to refresh" +
        " LDAP servers with older versions of the data. A zero value" +
        " can be used to specify an infinite delay (or never purge)");
    registerMessage(MSGID_SIMULTANEOUS_IMPORT_EXPORT_REJECTED,
        "The current request is rejected due to an import or an export" +
        " already in progress for the same data");
    registerMessage(MSGID_INVALID_IMPORT_SOURCE,
        "Invalid source for the import");
    registerMessage(MSGID_INVALID_EXPORT_TARGET,
        "Invalid target for the export");
    registerMessage(MSGID_NO_REACHABLE_PEER_IN_THE_DOMAIN,
        "No reachable peer in the domain");
    registerMessage(MSGID_NO_MATCHING_DOMAIN,
        "No domain matches the base DN provided");
    registerMessage(MSGID_MULTIPLE_MATCHING_DOMAIN,
        "Multiple domains match the base DN provided");
    registerMessage(MSGID_INVALID_PROVIDER,
        "The provider class does not allow the operation requested");
    registerMessage(MSGID_COULD_NOT_SOLVE_HOSTNAME,
        "The hostname %s could not be resolved as an IP address");
    registerMessage(MSGID_DUPLICATE_SERVER_ID,
        "Servers %s and %s have the same ServerId : %d");
    registerMessage(MSGID_DUPLICATE_REPLICATION_SERVER_ID,
        "Replication Servers %s and %s have the same ServerId : %d");
    registerMessage(MSGID_READER_NULL_MSG,
        "Received a Null Msg from %s");
    registerMessage(MSGID_READER_EXCEPTION,
        "Exception when reading messages from %s");
    registerMessage(MSGID_BAD_HISTORICAL,
        "Entry %s was containing some unknown historical information,"
        + " This may cause some inconsistency for this entry");
    registerMessage(MSGID_CANNOT_ADD_CONFLICT_ATTRIBUTE,
        "A conflict was detected but the conflict information could not be" +
        "added. Operation : ");
    registerMessage(MSGID_CANNOT_RENAME_CONFLICT_ENTRY,
        "An error happened trying the rename a conflicting entry : ");
    registerMessage(MSGID_EXCEPTION_RENAME_CONFLICT_ENTRY,
        "An Exception happened when trying the rename a conflicting entry : ");
    registerMessage(MSGID_CHANGELOG_UNSUPPORTED_UTF8_ENCODING,
        "The JVM does not support UTF-8. This is required to be able to "
        + "encode the changes in the database. "
        + "This replication server will now shutdown");
    registerMessage(MSGID_REPLICATION_COULD_NOT_CONNECT,
        "The Replication is configured for suffix  %s "
        + "but was not able to connect to any Replication Server");
    registerMessage(MSGID_NOW_FOUND_CHANGELOG,
        "Replication Server %s now used for Replication Domain %s");
    registerMessage(MSGID_DISCONNECTED_FROM_CHANGELOG,
        "The connection to Replication Server %s has been dropped by the "
        + "Replication Server");
    registerMessage(MSGID_CHANGELOG_ERROR_SENDING_INFO,
        "An unexpected error occurred  while sending a Server " +
        " Info message to %s. " +
        "This connection is going to be closed and reopened");
    registerMessage(MSGID_CHANGELOG_ERROR_SENDING_ERROR,
        "An unexpected error occurred  while sending an Error Message to %s. "+
        "This connection is going to be closed and reopened");
    registerMessage(MSGID_CHANGELOG_ERROR_SENDING_MSG,
        "An unexpected error occurred  while sending a Message to %s. "+
        "This connection is going to be closed and reopened");
  }
}
