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
 *      Copyright 2006-2008 Sun Microsystems, Inc.
 */
package org.opends.server.plugins;



import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.opends.server.admin.std.server.PluginCfg;
import org.opends.server.api.plugin.DirectoryServerPlugin;
import org.opends.server.api.plugin.PluginType;
import org.opends.server.api.plugin.PluginResult;
import org.opends.server.config.ConfigException;
import org.opends.server.protocols.asn1.ASN1Long;
import org.opends.server.protocols.asn1.ASN1OctetString;
import org.opends.server.protocols.ldap.LDAPControl;
import org.opends.server.types.Control;
import org.opends.server.types.ResultCode;
import org.opends.server.types.CanceledOperationException;
import org.opends.server.types.operation.*;
import org.opends.messages.Message;


/**
 * This class defines a very simple pre-operation plugin that sleeps for up to
 * five seconds for add, compare, delete, extended, modify, modify DN, and
 * search operations (and therefore not for abandon, bind, and unbind
 * operations, since those operations cannot be cancelled).  While it is
 * sleeping, it also checks for a request to cancel the associated operation and
 * will respond to it accordingly.
 */
public class DelayPreOpPlugin
       extends DirectoryServerPlugin<PluginCfg>
{
  /**
   * The OID for the delay request control, which is used to flag operations
   * that should be delayed.
   */
  public static final String OID_DELAY_REQUEST = "1.3.6.1.4.1.26027.1.999.1";



  /**
   * Creates a new instance of this Directory Server plugin.  Every
   * plugin must implement a default constructor (it is the only one
   * that will be used to create plugins defined in the
   * configuration), and every plugin constructor must call
   * <CODE>super()</CODE> as its first element.
   */
  public DelayPreOpPlugin()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void initializePlugin(Set<PluginType> pluginTypes,
                               PluginCfg configuration)
         throws ConfigException
  {
    // This plugin may only be used as a pre-operation plugin.
    for (PluginType t : pluginTypes)
    {
      switch (t)
      {
        case PRE_OPERATION_ADD:
        case PRE_OPERATION_BIND:
        case PRE_OPERATION_COMPARE:
        case PRE_OPERATION_DELETE:
        case PRE_OPERATION_EXTENDED:
        case PRE_OPERATION_MODIFY:
        case PRE_OPERATION_MODIFY_DN:
        case PRE_OPERATION_SEARCH:
          // This is fine.
          break;
        default:
          throw new ConfigException(Message.raw("Invalid plugin type " + t +
                                    " for delay pre-op plugin."));
      }
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public PluginResult.PreOperation
       doPreOperation(PreOperationAddOperation addOperation)
      throws CanceledOperationException {
    return doPreOperationInternal(addOperation);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public PluginResult.PreOperation
         doPreOperation(PreOperationBindOperation bindOperation)
  {
    try
    {
      return doPreOperationInternal(bindOperation);
    }
    catch(CanceledOperationException coe)
    {
      // Bind ops can't be canceled. Just ignore.
      return PluginResult.PreOperation.continueOperationProcessing();
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public PluginResult.PreOperation
         doPreOperation(PreOperationCompareOperation compareOperation)
      throws CanceledOperationException {
    return doPreOperationInternal(compareOperation);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public PluginResult.PreOperation
       doPreOperation(PreOperationDeleteOperation deleteOperation)
      throws CanceledOperationException {
    return doPreOperationInternal(deleteOperation);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public PluginResult.PreOperation
       doPreOperation(PreOperationExtendedOperation extendedOperation)
      throws CanceledOperationException {
    return doPreOperationInternal(extendedOperation);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public PluginResult.PreOperation
       doPreOperation(PreOperationModifyOperation modifyOperation)
      throws CanceledOperationException {
    return doPreOperationInternal(modifyOperation);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public PluginResult.PreOperation
       doPreOperation(PreOperationModifyDNOperation modifyDNOperation)
      throws CanceledOperationException {
    return doPreOperationInternal(modifyDNOperation);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public PluginResult.PreOperation
       doPreOperation(PreOperationSearchOperation searchOperation)
      throws CanceledOperationException {
    return doPreOperationInternal(searchOperation);
  }



  /**
   * Looks for a delay request control in the operation, and if one is found
   * then sleep in 10 millisecond increments up to the length of time specified
   * in the control value.  If the operation receives a cancel request during
   * this time, then the control will stop sleeping immediately.
   *
   * @param  operation  The operation to be processed.
   *
   * @return  The result of the plugin processing.
   */
  private PluginResult.PreOperation
       doPreOperationInternal(PreOperationOperation operation)
      throws CanceledOperationException {
    long delayDuration = 0L;
    List<Control> requestControls = operation.getRequestControls();
    if (requestControls != null)
    {
      for (Control c : requestControls)
      {
        if (c.getOID().equals(OID_DELAY_REQUEST))
        {
          try
          {
            delayDuration =
                 ASN1Long.decodeAsLong(c.getValue().value()).longValue();
          }
          catch (Exception e)
          {
            return PluginResult.PreOperation.stopProcessing(
                ResultCode.PROTOCOL_ERROR,
                Message.raw("Unable to decode the delay request control:  " +
                    e));
          }
        }
      }
    }

    if (delayDuration <= 0)
    {
      return PluginResult.PreOperation.continueOperationProcessing();
    }

    long stopSleepTime = System.currentTimeMillis() + delayDuration;
    while (System.currentTimeMillis() < stopSleepTime)
    {
      operation.checkIfCanceled(false);

      try
      {
        Thread.sleep(10);
      } catch (Exception e) {}
    }

    return PluginResult.PreOperation.continueOperationProcessing();
  }



  /**
   * Creates a delay request control with the specified delay.
   *
   * @param  delay  The length of time in milliseconds to sleep.
   *
   * @return  The appropriate delay request control.
   */
  public static Control createDelayControl(long delay)
  {
    return new Control(OID_DELAY_REQUEST, false,
                       new ASN1OctetString(new ASN1Long(delay).encode()));
  }



  /**
   * Retrieves a list containing a delay request control with the specified
   * delay.
   *
   * @param  delay  The length of time in milliseconds to sleep.
   *
   * @return  A list containing the appropriate delay request control.
   */
  public static List<Control> createDelayControlList(long delay)
  {
    ArrayList<Control> controlList = new ArrayList<Control>(1);

    ASN1OctetString controlValue =
         new ASN1OctetString(new ASN1Long(delay).encode());
    controlList.add(new Control(OID_DELAY_REQUEST, false, controlValue));

    return controlList;
  }



  /**
   * Creates a delay request LDAP control with the specified delay.
   *
   * @param  delay  The length of time in milliseconds to sleep.
   *
   * @return  The appropriate delay request LDAP control.
   */
  public static LDAPControl createDelayLDAPControl(long delay)
  {
    return new LDAPControl(OID_DELAY_REQUEST, false,
                           new ASN1OctetString(new ASN1Long(delay).encode()));
  }



  /**
   * Retrieves a list containing a delay request LDAP control with the specified
   * delay.
   *
   * @param  delay  The length of time in milliseconds to sleep.
   *
   * @return  A list containing the appropriate delay request LDAP control.
   */
  public static ArrayList<LDAPControl> createDelayLDAPControlList(long delay)
  {
    ArrayList<LDAPControl> controlList = new ArrayList<LDAPControl>(1);

    ASN1OctetString controlValue =
         new ASN1OctetString(new ASN1Long(delay).encode());
    controlList.add(new LDAPControl(OID_DELAY_REQUEST, false, controlValue));

    return controlList;
  }
}

