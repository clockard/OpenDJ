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
 *      Copyright 2008 Sun Microsystems, Inc.
 */
package org.opends.server.core.networkgroups;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;
import
 org.opends.server.admin.std.meta.NetworkGroupCriteriaCfgDefn.AllowedAuthMethod;
import org.opends.server.api.ClientConnection;
import org.opends.server.types.AuthenticationType;
import org.opends.server.types.DN;

/**
 * This class defines the authentication method criteria.
 * A connection matches the criteria if the authentication
 * method used on this connection is one of the allowed
 * authentication methods specified in the criteria.
 */

public class AuthMethodCriteria implements NetworkGroupCriterion {
  private Collection<AllowedAuthMethod> authMethods;

  /**
   * Constructor.
   */
  public AuthMethodCriteria() {
    authMethods = new TreeSet<AllowedAuthMethod>();
  }

  /**
   * Adds a new allowed authentication method to the list of allowed
   * authentication methods.
   * @param method The authentication method
   */
  public void addAuthMethod(AllowedAuthMethod method) {
    authMethods.add(method);
  }

  /**
   * {@inheritDoc}
   */
  public boolean match(ClientConnection connection) {
    Collection<AuthenticationType> authTypes =
         new ArrayList<AuthenticationType>();

    for (AllowedAuthMethod method:authMethods) {
      if (method == AllowedAuthMethod.ANONYMOUS) {
        if (connection.getAuthenticationInfo().isAuthenticated() == false) {
          return (true);
        }
      } else if (method == AllowedAuthMethod.SASL) {
        authTypes.add(AuthenticationType.SASL);
      } else if (method == AllowedAuthMethod.SIMPLE) {
        authTypes.add(AuthenticationType.SIMPLE);
      }
    }
    return (connection.getAuthenticationInfo().hasAnyAuthenticationType(
         authTypes));
  }

  /**
   * {@inheritDoc}
   */
  public boolean matchAfterBind(ClientConnection connection, DN bindDN,
            AuthenticationType authType, boolean isSecure) {
    for (AllowedAuthMethod method:authMethods) {
      if (method == AllowedAuthMethod.ANONYMOUS
          && bindDN.toNormalizedString().equals("")) {
        return true;
      }
      if (method == AllowedAuthMethod.SASL
          && authType == AuthenticationType.SASL) {
        return true;
      }
      if (method == AllowedAuthMethod.SIMPLE
          && authType == AuthenticationType.SIMPLE
          && !bindDN.toNormalizedString().equals("")) {
        return true;
      }
    }
    return false;
  }
}
