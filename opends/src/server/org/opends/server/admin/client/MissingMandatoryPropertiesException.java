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

package org.opends.server.admin.client;
import org.opends.messages.Message;



import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.opends.server.admin.OperationsException;
import org.opends.server.admin.PropertyIsMandatoryException;
import org.opends.server.util.Validator;



/**
 * This exception is thrown when an attempt is made to add or modify a
 * managed object when one or more of its mandatory properties are
 * undefined.
 */
public class MissingMandatoryPropertiesException extends OperationsException {

  /**
   * Serialization ID.
   */
  private static final long serialVersionUID = 6342522125252055588L;

  // The causes of this exception.
  private final Collection<PropertyIsMandatoryException> causes;



  /**
   * Creates a new missing mandatory properties exception with the
   * provided causes.
   *
   * @param causes
   *          The causes of this exception (must be non-<code>null</code>
   *          and non-empty).
   */
  public MissingMandatoryPropertiesException(
      Collection<PropertyIsMandatoryException> causes) {
    Validator.ensureNotNull(causes);
    Validator.ensureTrue(!causes.isEmpty());

    this.causes = new ArrayList<PropertyIsMandatoryException>(causes);
  }



  /**
   * Gets the first exception that caused this exception.
   *
   * @return Returns the first exception that caused this exception.
   */
  @Override
  public PropertyIsMandatoryException getCause() {
    return causes.iterator().next();
  }



  /**
   * Gets an unmodifiable collection view of the causes of this
   * exception.
   *
   * @return Returns an unmodifiable collection view of the causes of
   *         this exception.
   */
  public Collection<PropertyIsMandatoryException> getCauses() {
    return Collections.unmodifiableCollection(causes);
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public Message getMessageObject() {
    StringBuilder builder = new StringBuilder();
    builder.append("The following properties are mandatory: ");
    boolean isFirst = true;
    for (PropertyIsMandatoryException e : causes) {
      if (!isFirst) {
        builder.append(", ");
      }
      builder.append(e.getPropertyDefinition().getName());
      isFirst = false;
    }
    return Message.raw(builder.toString());
  }

}
