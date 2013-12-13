/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at legal-notices/CDDLv1_0.txt
 * or http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at legal-notices/CDDLv1_0.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2008 Sun Microsystems, Inc.
 */

package org.opends.server.admin;

import static com.forgerock.opendj.ldap.AdminMessages.*;

/**
 * Thrown when an attempt is made to add more than value to a single-valued
 * property.
 */
public class PropertyIsSingleValuedException extends PropertyException {

    /**
     * Serialization ID.
     */
    private static final long serialVersionUID = -8056602690887917027L;

    /**
     * Create a new property is single valued exception.
     *
     * @param pd
     *            The property definition.
     */
    public PropertyIsSingleValuedException(PropertyDefinition<?> pd) {
        super(pd, ERR_PROPERTY_IS_SINGLE_VALUED_EXCEPTION.get(pd.getName()));
    }
}
