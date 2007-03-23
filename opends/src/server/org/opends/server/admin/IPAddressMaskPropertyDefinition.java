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

package org.opends.server.admin;



import static org.opends.server.util.Validator.ensureNotNull;

import java.util.EnumSet;

import org.opends.server.config.ConfigException;
import org.opends.server.types.AddressMask;



/**
 * IP address mask property definition.
 */
public final class IPAddressMaskPropertyDefinition extends
    AbstractPropertyDefinition<AddressMask> {

  /**
   * Serialization ID.
   */
  private static final long serialVersionUID = -6641292526738863824L;



  /**
   * An interface for incrementally constructing IP address mask property
   * definitions.
   */
  public static class Builder extends
      AbstractBuilder<AddressMask, IPAddressMaskPropertyDefinition> {

    // Private constructor
    private Builder(String propertyName) {
      super(propertyName);
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected IPAddressMaskPropertyDefinition buildInstance(
        String propertyName, EnumSet<PropertyOption> options,
        DefaultBehaviorProvider<AddressMask> defaultBehavior) {
      return new IPAddressMaskPropertyDefinition(propertyName, options,
          defaultBehavior);
    }

  }



  /**
   * Create a IP address mask property definition builder.
   *
   * @param propertyName
   *          The property name.
   * @return Returns the new IP address mask property definition builder.
   */
  public static Builder createBuilder(String propertyName) {
    return new Builder(propertyName);
  }



  // Private constructor.
  private IPAddressMaskPropertyDefinition(String propertyName,
      EnumSet<PropertyOption> options,
      DefaultBehaviorProvider<AddressMask> defaultBehavior) {
    super(AddressMask.class, propertyName, options, defaultBehavior);
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public void validateValue(AddressMask value)
      throws IllegalPropertyValueException {
    ensureNotNull(value);

    // No additional validation required.
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public AddressMask decodeValue(String value)
      throws IllegalPropertyValueStringException {
    ensureNotNull(value);

    try {
      return AddressMask.decode(value);
    } catch (ConfigException e) {
      // TODO: it would be nice to throw the cause.
      throw new IllegalPropertyValueStringException(this, value);
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public <R, P> R accept(PropertyDefinitionVisitor<R, P> v, P p) {
    return v.visitIPAddressMask(this, p);
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public int compare(AddressMask o1, AddressMask o2) {
    return o1.toString().compareTo(o2.toString());
  }
}
