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
 *      Copyright 2006-2008 Sun Microsystems, Inc.
 */
package org.opends.server.types.operation;



import java.util.List;

import org.opends.server.types.AttributeValue;
import org.opends.server.types.ByteString;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.DN;
import org.opends.server.types.Entry;
import org.opends.server.types.Modification;
import org.opends.server.types.RawModification;



/**
 * This class defines a set of methods that are available for use by
 * pre-operation plugins for modify operations.  Note that this
 * interface is intended only to define an API for use by plugins and
 * is not intended to be implemented by any custom classes.
 */
@org.opends.server.types.PublicAPI(
     stability=org.opends.server.types.StabilityLevel.UNCOMMITTED,
     mayInstantiate=false,
     mayExtend=false,
     mayInvoke=true)
public interface PreOperationModifyOperation
       extends PreOperationOperation
{
  /**
   * Retrieves the raw, unprocessed entry DN as included in the client
   * request.  The DN that is returned may or may not be a valid DN,
   * since no validation will have been performed upon it.
   *
   * @return  The raw, unprocessed entry DN as included in the client
   *          request.
   */
  public ByteString getRawEntryDN();



  /**
   * Retrieves the DN of the entry to modify.
   *
   * @return  The DN of the entry to modify.
   */
  public DN getEntryDN();



  /**
   * Retrieves the set of raw, unprocessed modifications as included
   * in the client request.  Note that this may contain one or more
   * invalid modifications, as no validation will have been performed
   * on this information.  The list returned must not be altered by
   * the caller.
   *
   * @return  The set of raw, unprocessed modifications as included
   *          in the client request.
   */
  public List<RawModification> getRawModifications();



  /**
   * Retrieves the set of modifications for this modify operation.
   Its contents should not be altered.
   *
   * @return  The set of modifications for this modify operation.
   */
  public List<Modification> getModifications();



  /**
   * Adds the provided modification to the set of modifications to
   * this modify operation.  Note that this will be called after the
   * schema and access control processing, so the caller must be
   * careful to avoid making any changes that will violate schema or
   * access control constraints.
   *
   * @param  modification  The modification to add to the set of
   *                       changes for this modify operation.
   *
   * @throws  DirectoryException  If an unexpected problem occurs
   *                              while applying the modification to
   *                              the entry.
   */
  public void addModification(Modification modification)
         throws DirectoryException;



  /**
   * Retrieves the current entry before any modifications are applied.
   * It should not be modified by the caller.
   *
   * @return  The current entry before any modifications are applied.
   */
  public Entry getCurrentEntry();



  /**
   * Retrieves the modified entry that is to be written to the
   * backend.  This entry should not be modified directly, but should
   * only be altered through the <CODE>addModification</CODE> method.
   *
   * @return  The modified entry that is to be written to the backend.
   */
  public Entry getModifiedEntry();



  /**
   * Retrieves the set of clear-text current passwords for the user,
   * if available.  This will only be available if the modify
   * operation contains one or more delete elements that target the
   * password attribute and provide the values to delete in the clear.
   * This list should not be altered by the caller.
   *
   * @return  The set of clear-text current password values as
   *          provided in the modify request, or <CODE>null</CODE> if
   *          there were none.
   */
  public List<AttributeValue> getCurrentPasswords();



  /**
   * Retrieves the set of clear-text new passwords for the user, if
   * available.  This will only be available if the modify operation
   * contains one or more add or replace elements that target the
   * password attribute and provide the values in the clear.  This
   * list should not be altered by the caller.
   *
   * @return  The set of clear-text new passwords as provided in the
   *          modify request, or <CODE>null</CODE> if there were none.
   */
  public List<AttributeValue> getNewPasswords();
}

