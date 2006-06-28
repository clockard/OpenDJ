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
package org.opends.server.extensions;



import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import java.util.concurrent.locks.Lock;

import org.opends.server.api.CertificateMapper;
import org.opends.server.config.ConfigEntry;
import org.opends.server.config.ConfigException;
import org.opends.server.core.DirectoryException;
import org.opends.server.core.DirectoryServer;
import org.opends.server.core.InitializationException;
import org.opends.server.core.LockManager;
import org.opends.server.types.DN;
import org.opends.server.types.Entry;
import org.opends.server.types.ResultCode;

import static org.opends.server.loggers.Debug.*;
import static org.opends.server.messages.ExtensionsMessages.*;
import static org.opends.server.messages.MessageHandler.*;
import static org.opends.server.util.StaticUtils.*;



/**
 * This class implements a very simple Directory Server certificate mapper that
 * will map a certificate to a user only if the subject of the peer certificate
 * exactly matches the DN of a user in the Directory Server.
 */
public class SubjectEqualsDNCertificateMapper
       extends CertificateMapper
{
  /**
   * The fully-qualified name of this class for debugging purposes.
   */
  private static final String CLASS_NAME =
       "org.opends.server.extensions.SubjectEqualsDNCertificateMapper";



  /**
   * Creates a new instance of this certificate mapper.  Note that all actual
   * initialization should be done in the
   * <CODE>initializeCertificateMapper</CODE> method.
   */
  public SubjectEqualsDNCertificateMapper()
  {
    super();

    assert debugConstructor(CLASS_NAME);
  }



  /**
   * Initializes this certificate mapper based on the information in the
   * provided configuration entry.
   *
   * @param  configEntry  The configuration entry that contains the information
   *                      to use to initialize this certificate mapper.
   *
   * @throws  ConfigException  If the provided entry does not contain a valid
   *                           certificate mapper configuration.
   *
   * @throws  InitializationException  If a problem occurs during initialization
   *                                   that is not related to the server
   *                                   configuration.
   */
  public void initializeCertificateMapper(ConfigEntry configEntry)
         throws ConfigException, InitializationException
  {
    assert debugEnter(CLASS_NAME, "initializeCertificateMapper",
                      String.valueOf(configEntry));

    // No initialization is required.
  }



  /**
   * Establishes a mapping between the information in the provided certificate
   * chain to the DN of a single user in the Directory Server.
   *
   * @param  certificateChain  The certificate chain presented by the client
   *                           during SSL negotiation.  The peer certificate
   *                           will be listed first, followed by the ordered
   *                           issuer chain as appropriate.
   *
   * @return  The DN of the one user to whom the mapping was established, or
   *          <CODE>null</CODE> if no mapping was established and no special
   *         message is required to send back to the client.
   *
   * @throws  DirectoryException  If a problem occurred while attempting to
   *                              establish the mapping.  This may include
   *                              internal failures, a mapping which matches
   *                              multiple users, or any other case in which an
   *                              error message should be returned to the
   *                              client.
   */
  public Entry mapCertificateToUser(Certificate[] certificateChain)
         throws DirectoryException
  {
    assert debugEnter(CLASS_NAME, "mapCertificateToUser",
                      String.valueOf(certificateChain));


    // Make sure that a peer certificate was provided.
    if ((certificateChain == null) || (certificateChain.length == 0))
    {
      int    msgID   = MSGID_SEDCM_NO_PEER_CERTIFICATE;
      String message = getMessage(msgID);
      throw new DirectoryException(ResultCode.INVALID_CREDENTIALS, message,
                                   msgID);
    }


    // Get the first certificate in the chain.  It must be an X.509 certificate.
    X509Certificate peerCertificate;
    try
    {
      peerCertificate = (X509Certificate) certificateChain[0];
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "mapCertificateToUser", e);

      int    msgID   = MSGID_SEDCM_PEER_CERT_NOT_X509;
      String message =
           getMessage(msgID, String.valueOf(certificateChain[0].getType()));
      throw new DirectoryException(ResultCode.INVALID_CREDENTIALS, message,
                                   msgID);
    }


    // Get the subject from the peer certificate and decode it as a DN.
    X500Principal peerPrincipal = peerCertificate.getSubjectX500Principal();
    DN subjectDN;
    try
    {
      subjectDN = DN.decode(peerPrincipal.getName(X500Principal.RFC2253));
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "mapCertificateToUser", e);

      int    msgID   = MSGID_SEDCM_CANNOT_DECODE_SUBJECT_AS_DN;
      String message = getMessage(msgID, String.valueOf(peerPrincipal),
                                  stackTraceToSingleLineString(e));
      throw new DirectoryException(ResultCode.INVALID_CREDENTIALS, message,
                                   msgID);
    }


    // Acquire a read lock on the user entry.  If this fails, then so will the
    // certificate mapping.
    Lock readLock = null;
    for (int i=0; i < 3; i++)
    {
      readLock = LockManager.lockRead(subjectDN);
      if (readLock != null)
      {
        break;
      }
    }

    if (readLock == null)
    {
      int    msgID   = MSGID_SEDCM_CANNOT_LOCK_ENTRY;
      String message = getMessage(msgID, String.valueOf(subjectDN));
      throw new DirectoryException(ResultCode.INVALID_CREDENTIALS, message,
                                   msgID);
    }


    // Retrieve the entry with the specified DN from the directory.
    Entry userEntry;
    try
    {
      userEntry = DirectoryServer.getEntry(subjectDN);
    }
    catch (DirectoryException de)
    {
      assert debugException(CLASS_NAME, "mapCertificateToUser", de);

      int    msgID   = MSGID_SEDCM_CANNOT_GET_ENTRY;
      String message = getMessage(msgID, String.valueOf(subjectDN),
                                  de.getErrorMessage());
      throw new DirectoryException(ResultCode.INVALID_CREDENTIALS, message,
                                   msgID, de);
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "mapCertificateToUser", e);

      int    msgID   = MSGID_SEDCM_CANNOT_GET_ENTRY;
      String message = getMessage(msgID, String.valueOf(subjectDN),
                                  stackTraceToSingleLineString(e));
      throw new DirectoryException(ResultCode.INVALID_CREDENTIALS, message,
                                   msgID, e);
    }
    finally
    {
      LockManager.unlock(subjectDN, readLock);
    }


    if (userEntry == null)
    {
      int    msgID   = MSGID_SEDCM_NO_USER_FOR_DN;
      String message = getMessage(msgID, String.valueOf(subjectDN));
      throw new DirectoryException(ResultCode.INVALID_CREDENTIALS, message,
                                   msgID);
    }
    else
    {
      return userEntry;
    }
  }
}

