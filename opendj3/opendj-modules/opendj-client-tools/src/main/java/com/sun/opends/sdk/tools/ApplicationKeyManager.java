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
 *      Copyright 2008-2010 Sun Microsystems, Inc.
 *      Portions Copyright 2009 Parametric Technology Corporation (PTC)
 */

package com.sun.opends.sdk.tools;



import java.net.Socket;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;



/**
 * This class is in charge of checking whether the certificates that are
 * presented are trusted or not. This implementation tries to check also that
 * the subject DN of the certificate corresponds to the host passed using the
 * setHostName method.
 *<p>
 * The constructor tries to use a default TrustManager from the system and if it
 * cannot be retrieved this class will only accept the certificates explicitly
 * accepted by the user (and specified by calling acceptCertificate).
 *<p>
 * NOTE: this class is not aimed to be used when we have connections in
 * parallel.
 */
final class ApplicationKeyManager implements X509KeyManager
{
  static private final Logger LOG = Logger
      .getLogger(ApplicationKeyManager.class.getName());

  /**
   * The default keyManager.
   */
  private X509KeyManager keyManager = null;



  /**
   * The default constructor.
   *
   * @param keystore
   *          The keystore to use for this keymanager.
   * @param password
   *          The keystore password to use for this keymanager.
   */
  ApplicationKeyManager(final KeyStore keystore, final char[] password)
  {
    KeyManagerFactory kmf = null;
    String userSpecifiedAlgo = System
        .getProperty("org.opends.admin.keymanageralgo");
    String userSpecifiedProvider = System
        .getProperty("org.opends.admin.keymanagerprovider");

    // Handle IBM specific cases if the user did not specify a algorithm
    // and/or provider.
    final String vendor = System.getProperty("java.vendor");
    if (vendor.startsWith("IBM"))
    {
      if (userSpecifiedAlgo == null)
      {
        userSpecifiedAlgo = "IbmX509";
      }
      if (userSpecifiedProvider == null)
      {
        userSpecifiedProvider = "IBMJSSE2";
      }
    }

    // Have some fallbacks to choose the provider and algorith of the
    // key manager. First see if the user wanted to use something
    // specific, then try with the SunJSSE provider and SunX509
    // algorithm. Finally, fallback to the default algorithm of the JVM.
    final String[] preferredProvider = { userSpecifiedProvider, "SunJSSE",
        null, null };
    final String[] preferredAlgo = { userSpecifiedAlgo, "SunX509", "SunX509",
        TrustManagerFactory.getDefaultAlgorithm() };
    for (int i = 0; i < preferredProvider.length && keyManager == null; i++)
    {
      final String provider = preferredProvider[i];
      final String algo = preferredAlgo[i];
      if (algo == null)
      {
        continue;
      }
      try
      {
        if (provider != null)
        {
          kmf = KeyManagerFactory.getInstance(algo, provider);
        }
        else
        {
          kmf = KeyManagerFactory.getInstance(algo);
        }
        kmf.init(keystore, password);
        final KeyManager kms[] = kmf.getKeyManagers();
        /*
         * Iterate over the returned keymanagers, look for an instance of
         * X509KeyManager. If found, use that as our "default" key manager.
         */
        for (final KeyManager km : kms)
        {
          if (kms[i] instanceof X509KeyManager)
          {
            keyManager = (X509KeyManager) km;
            break;
          }
        }
      }
      catch (final NoSuchAlgorithmException e)
      {
        // Nothing to do. Maybe we should avoid this and be strict, but
        // we are in a best effor mode.
        LOG.log(Level.WARNING, "Error with the algorithm", e);
      }
      catch (final KeyStoreException e)
      {
        // Nothing to do. Maybe we should avoid this and be strict, but
        // we are in a best effor mode..
        LOG.log(Level.WARNING, "Error with the keystore", e);
      }
      catch (final UnrecoverableKeyException e)
      {
        // Nothing to do. Maybe we should avoid this and be strict, but
        // we are in a best effor mode.
        LOG.log(Level.WARNING, "Error with the key", e);
      }
      catch (final NoSuchProviderException e)
      {
        // Nothing to do. Maybe we should avoid this and be strict, but
        // we are in a best effor mode.
        LOG.log(Level.WARNING, "Error with the provider", e);
      }
    }
  }



  /**
   * Choose an alias to authenticate the client side of a secure socket given
   * the public key type and the list of certificate issuer authorities
   * recognized by the peer (if any).
   *
   * @param keyType
   *          the key algorithm type name(s), ordered with the most-preferred
   *          key type first.
   * @param issuers
   *          the list of acceptable CA issuer subject names or null if it does
   *          not matter which issuers are used.
   * @param socket
   *          the socket to be used for this connection. This parameter can be
   *          null, in which case this method will return the most generic alias
   *          to use.
   * @return the alias name for the desired key, or null if there are no
   *         matches.
   */
  public String chooseClientAlias(final String[] keyType,
      final Principal[] issuers, final Socket socket)
  {
    if (keyManager != null)
    {
      return keyManager.chooseClientAlias(keyType, issuers, socket);
    }
    else
    {
      return null;
    }
  }



  /**
   * Choose an alias to authenticate the client side of a secure socket given
   * the public key type and the list of certificate issuer authorities
   * recognized by the peer (if any).
   *
   * @param keyType
   *          the key algorithm type name(s), ordered with the most-preferred
   *          key type first.
   * @param issuers
   *          the list of acceptable CA issuer subject names or null if it does
   *          not matter which issuers are used.
   * @param socket
   *          the socket to be used for this connection. This parameter can be
   *          null, in which case this method will return the most generic alias
   *          to use.
   * @return the alias name for the desired key, or null if there are no
   *         matches.
   */
  public String chooseServerAlias(final String keyType,
      final Principal[] issuers, final Socket socket)
  {
    if (keyManager != null)
    {
      return keyManager.chooseServerAlias(keyType, issuers, socket);
    }
    else
    {
      return null;
    }
  }



  /**
   * Returns the certificate chain associated with the given alias.
   *
   * @param alias
   *          the alias name
   * @return the certificate chain (ordered with the user's certificate first
   *         and the root certificate authority last), or null if the alias
   *         can't be found.
   */
  public X509Certificate[] getCertificateChain(final String alias)
  {
    if (keyManager != null)
    {
      return keyManager.getCertificateChain(alias);
    }
    else
    {
      return null;
    }
  }



  /**
   * Get the matching aliases for authenticating the server side of a secure
   * socket given the public key type and the list of certificate issuer
   * authorities recognized by the peer (if any).
   *
   * @param keyType
   *          the key algorithm type name
   * @param issuers
   *          the list of acceptable CA issuer subject names or null if it does
   *          not matter which issuers are used.
   * @return an array of the matching alias names, or null if there were no
   *         matches.
   */
  public String[] getClientAliases(final String keyType,
      final Principal[] issuers)
  {
    if (keyManager != null)
    {
      return keyManager.getClientAliases(keyType, issuers);
    }
    else
    {
      return null;
    }
  }



  /**
   * Returns the key associated with the given alias.
   *
   * @param alias
   *          the alias name
   * @return the requested key, or null if the alias can't be found.
   */
  public PrivateKey getPrivateKey(final String alias)
  {
    if (keyManager != null)
    {
      return keyManager.getPrivateKey(alias);
    }
    else
    {
      return null;
    }
  }



  /**
   * Get the matching aliases for authenticating the server side of a secure
   * socket given the public key type and the list of certificate issuer
   * authorities recognized by the peer (if any).
   *
   * @param keyType
   *          the key algorithm type name
   * @param issuers
   *          the list of acceptable CA issuer subject names or null if it does
   *          not matter which issuers are used.
   * @return an array of the matching alias names, or null if there were no
   *         matches.
   */
  public String[] getServerAliases(final String keyType,
      final Principal[] issuers)
  {
    if (keyManager != null)
    {
      return keyManager.getServerAliases(keyType, issuers);
    }
    else
    {
      return null;
    }
  }
}
