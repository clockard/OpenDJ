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
package org.opends.server.admin.server;



import java.util.LinkedList;
import java.util.List;

import org.opends.server.admin.Configuration;
import org.opends.server.admin.DecodingException;
import org.opends.server.admin.InstantiableRelationDefinition;
import org.opends.server.admin.ManagedObjectPath;
import org.opends.server.admin.OptionalRelationDefinition;
import org.opends.server.admin.RelationDefinition;
import org.opends.server.api.ConfigDeleteListener;
import org.opends.server.config.ConfigEntry;
import org.opends.server.types.AttributeValue;
import org.opends.server.types.ConfigChangeResult;
import org.opends.server.types.DN;
import org.opends.server.types.ResultCode;



/**
 * An adaptor class which converts {@link ConfigDeleteListener}
 * callbacks to strongly typed {@link ConfigurationDeleteListener}
 * callbacks.
 *
 * @param <S>
 *          The type of server configuration handled by the delete
 *          listener.
 */
final class ConfigDeleteListenerAdaptor<S extends Configuration>
    extends AbstractConfigListenerAdaptor implements
    ConfigDeleteListener {

  // The managed object path of the parent.
  private final ManagedObjectPath<?, ?> path;

  // The instantiable relation.
  private final InstantiableRelationDefinition<?, S> instantiableRelation;

  // The optional relation.
  private final OptionalRelationDefinition<?, S> optionalRelation;

  // The underlying delete listener.
  private final ConfigurationDeleteListener<S> listener;

  // Cached configuration object between accept/apply callbacks.
  private S cachedConfiguration;



  /**
   * Create a new configuration delete listener adaptor for an
   * instantiable relation.
   *
   * @param path
   *          The managed object path of the parent.
   * @param relation
   *          The instantiable relation.
   * @param listener
   *          The underlying delete listener.
   */
  public ConfigDeleteListenerAdaptor(ManagedObjectPath<?, ?> path,
      InstantiableRelationDefinition<?, S> relation,
      ConfigurationDeleteListener<S> listener) {
    this.path = path;
    this.optionalRelation = null;
    this.instantiableRelation = relation;
    this.listener = listener;
    this.cachedConfiguration = null;
  }



  /**
   * Create a new configuration delete listener adaptor for an
   * optional relation.
   *
   * @param path
   *          The managed object path of the parent.
   * @param relation
   *          The optional relation.
   * @param listener
   *          The underlying delete listener.
   */
  public ConfigDeleteListenerAdaptor(ManagedObjectPath<?, ?> path,
      OptionalRelationDefinition<?, S> relation,
      ConfigurationDeleteListener<S> listener) {
    this.path = path;
    this.optionalRelation = relation;
    this.instantiableRelation = null;
    this.listener = listener;
    this.cachedConfiguration = null;
  }



  /**
   * {@inheritDoc}
   */
  public ConfigChangeResult applyConfigurationDelete(
      ConfigEntry configEntry) {
    if (optionalRelation != null) {
      // Optional managed objects are located directly beneath the
      // parent and have a well-defined name. We need to make sure
      // that we are handling the correct entry.
      ManagedObjectPath<?, ?> childPath = path.child(optionalRelation);
      DN expectedDN = DNBuilder.create(childPath);
      if (!configEntry.getDN().equals(expectedDN)) {
        // Doesn't apply to us.
        return new ConfigChangeResult(ResultCode.SUCCESS, false);
      }
    }

    // Cached objects are guaranteed to be from previous acceptable
    // callback.
    return listener.applyConfigurationDelete(cachedConfiguration);
  }



  /**
   * {@inheritDoc}
   */
  public boolean configDeleteIsAcceptable(ConfigEntry configEntry,
      StringBuilder unacceptableReason) {
    DN dn = configEntry.getDN();
    AttributeValue av = dn.getRDN().getAttributeValue(0);
    String name = av.getStringValue().trim();

    ManagedObjectPath<?, ?> childPath;
    RelationDefinition<?, S> r;
    if (instantiableRelation != null) {
      childPath = path.child(instantiableRelation, name);
      r = instantiableRelation;
    } else {
      // Optional managed objects are located directly beneath the
      // parent and have a well-defined name. We need to make sure
      // that we are handling the correct entry.
      childPath = path.child(optionalRelation);
      DN expectedDN = DNBuilder.create(childPath);
      if (!dn.equals(expectedDN)) {
        // Doesn't apply to us.
        return true;
      }

      r = optionalRelation;
    }

    ServerManagedObject<? extends S> mo;
    try {
      mo = ServerManagedObject.decode(childPath, r
          .getChildDefinition(), configEntry);
    } catch (DecodingException e) {
      generateUnacceptableReason(e, unacceptableReason);
      return false;
    }

    cachedConfiguration = mo.getConfiguration();
    List<String> reasons = new LinkedList<String>();
    if (listener.isConfigurationDeleteAcceptable(cachedConfiguration,
        reasons)) {
      return true;
    } else {
      generateUnacceptableReason(reasons, unacceptableReason);
      return false;
    }
  }



  /**
   * Get the configuration delete listener associated with this
   * adaptor.
   *
   * @return Returns the configuration delete listener associated
   *         with this adaptor.
   */
  ConfigurationDeleteListener<S> getConfigurationDeleteListener() {
    return listener;
  }
}
