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
package org.opends.server.core;



import java.util.ArrayList;

import org.opends.server.api.CertificateMapper;
import org.opends.server.api.ConfigAddListener;
import org.opends.server.api.ConfigChangeListener;
import org.opends.server.api.ConfigDeleteListener;
import org.opends.server.config.BooleanConfigAttribute;
import org.opends.server.config.ConfigEntry;
import org.opends.server.config.StringConfigAttribute;
import org.opends.server.extensions.SubjectEqualsDNCertificateMapper;
import org.opends.server.types.ConfigChangeResult;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.DN;
import org.opends.server.types.ErrorLogCategory;
import org.opends.server.types.ErrorLogSeverity;
import org.opends.server.types.InitializationException;
import org.opends.server.types.ResultCode;

import static org.opends.server.config.ConfigConstants.*;
import static org.opends.server.loggers.Debug.*;
import static org.opends.server.loggers.Error.*;
import static org.opends.server.messages.ConfigMessages.*;
import static org.opends.server.messages.MessageHandler.*;
import static org.opends.server.util.StaticUtils.*;



/**
 * This class defines a utility that will be used to manage the configuration
 * for the Directory Server certificate mapper.  Only a single certificate
 * mapper may be defined, but if it is absent or disabled, then a default
 * provider will be used that will assume that the certificate subject is equal
 * to the user entry's DN.
 */
public class CertificateMapperConfigManager
       implements ConfigChangeListener, ConfigAddListener, ConfigDeleteListener
{
  /**
   * The fully-qualified name of this class for debugging purposes.
   */
  private static final String CLASS_NAME =
       "org.opends.server.core.CertificateMapperConfigManager";



  /**
   * Creates a new instance of this certificate mapper provider config manager.
   */
  public CertificateMapperConfigManager()
  {
    assert debugConstructor(CLASS_NAME);

    // No implementation is required.
  }



  /**
   * Initializes the configuration associated with the Directory Server
   * certificate mapper.  This should only be called at Directory Server
   * startup.  If an error occurs, then a message will be logged and the default
   * certificate mapper will be installed.
   *
   * @throws  InitializationException  If a problem occurs while trying to
   *                                   install the default certificate mapper.
   */
  public void initializeCertificateMapper()
         throws InitializationException
  {
    assert debugEnter(CLASS_NAME, "initializeCertificateMapper");


    // First, install the default certificate mapper so that there will be one
    // even if we encounter a problem later.
    try
    {
      SubjectEqualsDNCertificateMapper defaultMapper =
           new SubjectEqualsDNCertificateMapper();
      defaultMapper.initializeCertificateMapper(null);
      DirectoryServer.setCertificateMapper(defaultMapper);
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "initializeCertificateMapper", e);

      int msgID = MSGID_CONFIG_CERTMAPPER_CANNOT_INSTALL_DEFAULT_MAPPER;
      String message = getMessage(msgID, stackTraceToSingleLineString(e));
      throw new InitializationException(msgID, message, e);
    }


    // Get the certificate mapper configuration entry.  If it is not present,
    // then register an add listener and just go with the default mapper.
    DN configEntryDN;
    ConfigEntry configEntry;
    try
    {
      configEntryDN = DN.decode(DN_CERTMAPPER_CONFIG);
      configEntry   = DirectoryServer.getConfigEntry(configEntryDN);
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "initializeCertificateMapper", e);

      logError(ErrorLogCategory.CONFIGURATION, ErrorLogSeverity.SEVERE_ERROR,
               MSGID_CONFIG_CERTMAPPER_CANNOT_GET_CONFIG_ENTRY,
               stackTraceToSingleLineString(e));
      return;
    }

    if (configEntry == null)
    {
      logError(ErrorLogCategory.CONFIGURATION, ErrorLogSeverity.SEVERE_WARNING,
               MSGID_CONFIG_CERTMAPPER_NO_CONFIG_ENTRY);

      try
      {
        ConfigEntry parentEntry =
             DirectoryServer.getConfigEntry(configEntryDN.getParent());
        if (parentEntry != null)
        {
          parentEntry.registerAddListener(this);
        }
      }
      catch (Exception e)
      {
        assert debugException(CLASS_NAME, "initializeCertificateMapper", e);

        logError(ErrorLogCategory.CONFIGURATION, ErrorLogSeverity.SEVERE_ERROR,
                 MSGID_CONFIG_CERTMAPPER_CANNOT_REGISTER_ADD_LISTENER,
                 stackTraceToSingleLineString(e));
      }

      return;
    }


    // At this point, we have a configuration entry.  Register a change listener
    // with it so we can be notified of changes to it over time.  We will also
    // want to register a delete listener with its parent to allow us to
    // determine if the entry is deleted.
    configEntry.registerChangeListener(this);
    try
    {
      DN parentDN = configEntryDN.getParent();
      ConfigEntry parentEntry = DirectoryServer.getConfigEntry(parentDN);
      if (parentEntry != null)
      {
        parentEntry.registerDeleteListener(this);
      }
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "initializeCertificateMapper", e);

      logError(ErrorLogCategory.CONFIGURATION, ErrorLogSeverity.SEVERE_WARNING,
               MSGID_CONFIG_CERTMAPPER_CANNOT_REGISTER_DELETE_LISTENER,
               stackTraceToSingleLineString(e));
    }


    // See if the entry indicates whether the certificate mapper should be
    // enabled.
    int msgID = MSGID_CONFIG_CERTMAPPER_DESCRIPTION_ENABLED;
    BooleanConfigAttribute enabledStub =
         new BooleanConfigAttribute(ATTR_CERTMAPPER_ENABLED, getMessage(msgID),
                                    false);
    try
    {
      BooleanConfigAttribute enabledAttr =
           (BooleanConfigAttribute)
           configEntry.getConfigAttribute(enabledStub);
      if (enabledAttr == null)
      {
        // The attribute is not present, so the certificate mapper will be
        // disabled.  Log a warning message and return.
        logError(ErrorLogCategory.CONFIGURATION,
                 ErrorLogSeverity.SEVERE_WARNING,
                 MSGID_CONFIG_CERTMAPPER_NO_ENABLED_ATTR);
        return;
      }
      else if (! enabledAttr.activeValue())
      {
        // The certificate mapper is explicitly disabled.  Log a mild warning
        // and return.
        logError(ErrorLogCategory.CONFIGURATION, ErrorLogSeverity.MILD_WARNING,
                 MSGID_CONFIG_CERTMAPPER_DISABLED);
        return;
      }
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "initializeCertificateMapper", e);

      logError(ErrorLogCategory.CONFIGURATION, ErrorLogSeverity.SEVERE_ERROR,
               MSGID_CONFIG_CERTMAPPER_UNABLE_TO_DETERMINE_ENABLED_STATE,
               stackTraceToSingleLineString(e));
      return;
    }


    // See if it specifies the class name for the certificate mapper
    // implementation.
    String className;
    msgID = MSGID_CONFIG_CERTMAPPER_DESCRIPTION_CLASS;
    StringConfigAttribute classStub =
         new StringConfigAttribute(ATTR_CERTMAPPER_CLASS, getMessage(msgID),
                                   true, false, false);
    try
    {
      StringConfigAttribute classAttr =
           (StringConfigAttribute) configEntry.getConfigAttribute(classStub);
      if (classAttr == null)
      {
        logError(ErrorLogCategory.CONFIGURATION, ErrorLogSeverity.SEVERE_ERROR,
                 MSGID_CONFIG_CERTMAPPER_NO_CLASS_ATTR);
        return;
      }
      else
      {
        className = classAttr.activeValue();
      }
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "initializeCertificateMapper", e);

      logError(ErrorLogCategory.CONFIGURATION, ErrorLogSeverity.SEVERE_ERROR,
               MSGID_CONFIG_CERTMAPPER_CANNOT_DETERMINE_CLASS,
               stackTraceToSingleLineString(e));
      return;
    }


    // Try to load the class and instantiate it as a certificate mapper.
    Class certificateMapperClass;
    try
    {
      // FIXME -- Should we use a custom class loader for this?
      certificateMapperClass = Class.forName(className);
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "initializeCertificateMapper", e);

      logError(ErrorLogCategory.CONFIGURATION, ErrorLogSeverity.SEVERE_ERROR,
               MSGID_CONFIG_CERTMAPPER_CANNOT_LOAD_CLASS,
               String.valueOf(className), stackTraceToSingleLineString(e));
      return;
    }

    CertificateMapper certificateMapper;
    try
    {
      certificateMapper =
           (CertificateMapper) certificateMapperClass.newInstance();
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "initializeCertificateMapper", e);

      logError(ErrorLogCategory.CONFIGURATION, ErrorLogSeverity.SEVERE_ERROR,
               MSGID_CONFIG_CERTMAPPER_CANNOT_INSTANTIATE_CLASS,
               String.valueOf(className), stackTraceToSingleLineString(e));
      return;
    }


    // Try to initialize the certificate mapper with the contents of the
    // configuration entry.
    try
    {
      certificateMapper.initializeCertificateMapper(configEntry);
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "initializeCertificateMapper", e);

      logError(ErrorLogCategory.CONFIGURATION, ErrorLogSeverity.SEVERE_ERROR,
               MSGID_CONFIG_CERTMAPPER_CANNOT_INITIALIZE,
               String.valueOf(className), stackTraceToSingleLineString(e));
      return;
    }


    // Install the new certificate mapper in the server.  We don't need to do
    // anything to get rid of the previous null provider since it doesn't
    // consume any resources.
    DirectoryServer.setCertificateMapper(certificateMapper);
  }



  /**
   * Indicates whether the configuration entry that will result from a proposed
   * modification is acceptable to this change listener.
   *
   * @param  configEntry         The configuration entry that will result from
   *                             the requested update.
   * @param  unacceptableReason  A buffer to which this method can append a
   *                             human-readable message explaining why the
   *                             proposed change is not acceptable.
   *
   * @return  <CODE>true</CODE> if the proposed entry contains an acceptable
   *          configuration, or <CODE>false</CODE> if it does not.
   */
  public boolean configChangeIsAcceptable(ConfigEntry configEntry,
                                          StringBuilder unacceptableReason)
  {
    assert debugEnter(CLASS_NAME, "configChangeIsAcceptable",
                      String.valueOf(configEntry), "java.lang.StringBuilder");


    // See if the entry indicates whether the certificate mapper should be
    // enabled.
    int msgID = MSGID_CONFIG_CERTMAPPER_DESCRIPTION_ENABLED;
    BooleanConfigAttribute enabledStub =
         new BooleanConfigAttribute(ATTR_CERTMAPPER_ENABLED, getMessage(msgID),
                                    false);
    try
    {
      BooleanConfigAttribute enabledAttr =
           (BooleanConfigAttribute)
           configEntry.getConfigAttribute(enabledStub);
      if (enabledAttr == null)
      {
        msgID = MSGID_CONFIG_CERTMAPPER_NO_ENABLED_ATTR;
        unacceptableReason.append(getMessage(msgID));
        return false;
      }
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "configChangeIsAcceptable", e);

      msgID = MSGID_CONFIG_CERTMAPPER_UNABLE_TO_DETERMINE_ENABLED_STATE;
      unacceptableReason.append(getMessage(msgID,
                                           stackTraceToSingleLineString(e)));
      return false;
    }


    // See if it specifies the class name for the certificate mapper
    // implementation.
    String className;
    msgID = MSGID_CONFIG_CERTMAPPER_DESCRIPTION_CLASS;
    StringConfigAttribute classStub =
         new StringConfigAttribute(ATTR_CERTMAPPER_CLASS, getMessage(msgID),
                                   true, false, false);
    try
    {
      StringConfigAttribute classAttr =
           (StringConfigAttribute) configEntry.getConfigAttribute(classStub);
      if (classAttr == null)
      {
        msgID = MSGID_CONFIG_CERTMAPPER_NO_CLASS_ATTR;
        unacceptableReason.append(getMessage(msgID));
        return false;
      }
      else
      {
        className = classAttr.activeValue();
      }
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "configChangeIsAcceptable", e);

      msgID = MSGID_CONFIG_CERTMAPPER_CANNOT_DETERMINE_CLASS;
      unacceptableReason.append(getMessage(msgID,
                                           stackTraceToSingleLineString(e)));
      return false;
    }


    // Try to load the class and instantiate it as a certificate mapper.
    Class certificateMapperClass;
    try
    {
      // FIXME -- Should we use a custom class loader for this?
      certificateMapperClass = Class.forName(className);
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "configChangeIsAcceptable", e);

      msgID = MSGID_CONFIG_CERTMAPPER_CANNOT_LOAD_CLASS;
      unacceptableReason.append(getMessage(msgID, String.valueOf(className),
                                           stackTraceToSingleLineString(e)));
      return false;
    }

    try
    {
      CertificateMapper certificateMapper =
           (CertificateMapper) certificateMapperClass.newInstance();
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "configChangeIsAcceptable", e);

      msgID = MSGID_CONFIG_CERTMAPPER_CANNOT_INSTANTIATE_CLASS;
      unacceptableReason.append(getMessage(msgID, String.valueOf(className),
                                           stackTraceToSingleLineString(e)));
      return false;
    }


    // If we've gotten to this point, then it is acceptable as far as we are
    // concerned.  If it is unacceptable according to the configuration, then
    // the certificate mapper itself will make that determination.
    return true;
  }



  /**
   * Attempts to apply a new configuration to this Directory Server component
   * based on the provided changed entry.
   *
   * @param  configEntry  The configuration entry that containing the updated
   *                      configuration for this component.
   *
   * @return  Information about the result of processing the configuration
   *          change.
   */
  public ConfigChangeResult applyConfigurationChange(ConfigEntry configEntry)
  {
    assert debugEnter(CLASS_NAME, "applyConfigurationChange",
                      String.valueOf(configEntry));

    ResultCode        resultCode          = ResultCode.SUCCESS;
    boolean           adminActionRequired = false;
    ArrayList<String> messages            = new ArrayList<String>();


    // See if the entry indicates whether the certificate mapper should be
    // enabled.  If not, then make sure that the certificate mapper is disabled
    // and return since we don't need to do anything else.
    boolean needsEnabled          = false;
    String  existingProviderClass = null;
    int msgID = MSGID_CONFIG_CERTMAPPER_DESCRIPTION_ENABLED;
    BooleanConfigAttribute enabledStub =
         new BooleanConfigAttribute(ATTR_CERTMAPPER_ENABLED, getMessage(msgID),
                                    false);
    try
    {
      BooleanConfigAttribute enabledAttr =
           (BooleanConfigAttribute)
           configEntry.getConfigAttribute(enabledStub);
      if (enabledAttr == null)
      {
        msgID = MSGID_CONFIG_CERTMAPPER_NO_ENABLED_ATTR;
        messages.add(getMessage(msgID));
        resultCode = ResultCode.OBJECTCLASS_VIOLATION;
        return new ConfigChangeResult(resultCode, adminActionRequired,
                                      messages);
      }
      else if (! enabledAttr.pendingValue())
      {
        DirectoryServer.getCertificateMapper().finalizeCertificateMapper();

        // The provider should be disabled, so install the default certificate
        // mapper and return.
        try
        {
          SubjectEqualsDNCertificateMapper defaultMapper =
               new SubjectEqualsDNCertificateMapper();
          defaultMapper.initializeCertificateMapper(null);
          DirectoryServer.setCertificateMapper(defaultMapper);
          return new ConfigChangeResult(resultCode, adminActionRequired,
                                        messages);
        }
        catch (Exception e)
        {
          assert debugException(CLASS_NAME, "applyConfigurationChange", e);

          msgID = MSGID_CONFIG_CERTMAPPER_CANNOT_INSTALL_DEFAULT_MAPPER;
          messages.add(getMessage(msgID, stackTraceToSingleLineString(e)));
          resultCode = DirectoryServer.getServerErrorResultCode();
          return new ConfigChangeResult(resultCode, adminActionRequired,
                                        messages);
        }
      }
      else
      {
        // The provider should be enabled.  If it isn't, then set a flag to
        // indicate that we need to create it when we have more information.
        if (DirectoryServer.getCertificateMapper() instanceof
            SubjectEqualsDNCertificateMapper)
        {
          needsEnabled = true;
        }
        else
        {
          existingProviderClass =
               DirectoryServer.getCertificateMapper().getClass().getName();
        }
      }
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "applyConfigurationChange", e);

      msgID = MSGID_CONFIG_CERTMAPPER_UNABLE_TO_DETERMINE_ENABLED_STATE;
      messages.add(getMessage(msgID, stackTraceToSingleLineString(e)));
      resultCode = DirectoryServer.getServerErrorResultCode();
      return new ConfigChangeResult(resultCode, adminActionRequired, messages);
    }


    // Get the class name from the configuration entry.
    String className;
    msgID = MSGID_CONFIG_CERTMAPPER_DESCRIPTION_CLASS;
    StringConfigAttribute classStub =
         new StringConfigAttribute(ATTR_CERTMAPPER_CLASS, getMessage(msgID),
                                   true, false, false);
    try
    {
      StringConfigAttribute classAttr =
           (StringConfigAttribute) configEntry.getConfigAttribute(classStub);
      if (classAttr == null)
      {
        msgID = MSGID_CONFIG_CERTMAPPER_NO_CLASS_ATTR;
        messages.add(getMessage(msgID));
        resultCode = ResultCode.OBJECTCLASS_VIOLATION;
        return new ConfigChangeResult(resultCode, adminActionRequired,
                                      messages);
      }
      else
      {
        className = classAttr.activeValue();
      }
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "applyConfigurationChange", e);

      msgID = MSGID_CONFIG_CERTMAPPER_CANNOT_DETERMINE_CLASS;
      messages.add(getMessage(msgID, stackTraceToSingleLineString(e)));
      resultCode = DirectoryServer.getServerErrorResultCode();
      return new ConfigChangeResult(resultCode, adminActionRequired, messages);
    }


    // If the certificate mapper is already enabled and the specified class is
    // different from the class that is currently in use, then we won't try to
    // do anything.  The certificate mapper must be disabled and re-enabled
    // before the configuration change will be accepted.
    if (! needsEnabled)
    {
      if (! className.equals(existingProviderClass))
      {
        msgID = MSGID_CONFIG_CERTMAPPER_NOT_SWITCHING_CLASSES;
        messages.add(getMessage(msgID, String.valueOf(existingProviderClass),
                                String.valueOf(className)));
        resultCode = ResultCode.UNWILLING_TO_PERFORM;
        adminActionRequired = true;
        return new ConfigChangeResult(resultCode, adminActionRequired,
                                      messages);
      }
      else
      {
        // We don't need to do anything because it's already enabled and has the
        // right class.
        return new ConfigChangeResult(resultCode, adminActionRequired,
                                      messages);
      }
    }


    // Try to load the class and instantiate it as a certificate mapper.
    Class certificateMapperClass;
    try
    {
      // FIXME -- Should we use a custom class loader for this?
      certificateMapperClass = Class.forName(className);
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "applyConfigurationChange", e);

      msgID = MSGID_CONFIG_CERTMAPPER_CANNOT_LOAD_CLASS;
      messages.add(getMessage(msgID, String.valueOf(className),
                              stackTraceToSingleLineString(e)));
      resultCode = DirectoryServer.getServerErrorResultCode();
      return new ConfigChangeResult(resultCode, adminActionRequired, messages);
    }

    CertificateMapper certificateMapper;
    try
    {
      certificateMapper =
           (CertificateMapper) certificateMapperClass.newInstance();
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "applyConfigurationChange", e);

      msgID = MSGID_CONFIG_CERTMAPPER_CANNOT_INSTANTIATE_CLASS;
      messages.add(getMessage(msgID, String.valueOf(className),
                              stackTraceToSingleLineString(e)));
      resultCode = DirectoryServer.getServerErrorResultCode();
      return new ConfigChangeResult(resultCode, adminActionRequired, messages);
    }


    // Try to initialize the certificate mapper with the contents of the
    // configuration entry.
    try
    {
      certificateMapper.initializeCertificateMapper(configEntry);
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "applyConfigurationChange", e);

      msgID = MSGID_CONFIG_CERTMAPPER_CANNOT_INITIALIZE;
      messages.add(getMessage(msgID, String.valueOf(className),
                              stackTraceToSingleLineString(e)));
      resultCode = DirectoryServer.getServerErrorResultCode();
      return new ConfigChangeResult(resultCode, adminActionRequired, messages);
    }


    // Install the new certificate mapper in the server.  We don't need to do
    // anything to get rid of the previous default mapper since it doesn't
    // consume any resources.
    DirectoryServer.setCertificateMapper(certificateMapper);


    return new ConfigChangeResult(resultCode, adminActionRequired, messages);
  }



  /**
   * Indicates whether the configuration entry that will result from a proposed
   * add is acceptable to this add listener.
   *
   * @param  configEntry         The configuration entry that will result from
   *                             the requested add.
   * @param  unacceptableReason  A buffer to which this method can append a
   *                             human-readable message explaining why the
   *                             proposed entry is not acceptable.
   *
   * @return  <CODE>true</CODE> if the proposed entry contains an acceptable
   *          configuration, or <CODE>false</CODE> if it does not.
   */
  public boolean configAddIsAcceptable(ConfigEntry configEntry,
                                       StringBuilder unacceptableReason)
  {
    assert debugEnter(CLASS_NAME, "configAddIsAcceptable",
                      String.valueOf(configEntry), "java.lang.StringBuilder");


    // Get the DN of the provided entry and see if it is the DN that we expect
    // for the certificate mapper configuration.  If it is not, then it's not an
    // entry that we care about so return true.
    DN providedEntryDN = configEntry.getDN();
    DN expectedEntryDN;
    try
    {
      expectedEntryDN = DN.decode(DN_CERTMAPPER_CONFIG);
    }
    catch (DirectoryException de)
    {
      assert debugException(CLASS_NAME, "configAddIsAcceptable", de);

      unacceptableReason.append(de.getErrorMessage());
      return false;
    }

    if (! providedEntryDN.equals(expectedEntryDN))
    {
      return true;
    }


    // See if the entry indicates whether the certificate mapper should be
    // enabled.
    int msgID = MSGID_CONFIG_CERTMAPPER_DESCRIPTION_ENABLED;
    BooleanConfigAttribute enabledStub =
         new BooleanConfigAttribute(ATTR_CERTMAPPER_ENABLED, getMessage(msgID),
                                    false);
    try
    {
      BooleanConfigAttribute enabledAttr =
           (BooleanConfigAttribute)
           configEntry.getConfigAttribute(enabledStub);
      if (enabledAttr == null)
      {
        msgID = MSGID_CONFIG_CERTMAPPER_NO_ENABLED_ATTR;
        unacceptableReason.append(getMessage(msgID));
        return false;
      }
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "configAddIsAcceptable", e);

      msgID = MSGID_CONFIG_CERTMAPPER_UNABLE_TO_DETERMINE_ENABLED_STATE;
      unacceptableReason.append(getMessage(msgID,
                                           stackTraceToSingleLineString(e)));
      return false;
    }


    // See if it specifies the class name for the certificate mapper
    // implementation.
    String className;
    msgID = MSGID_CONFIG_CERTMAPPER_DESCRIPTION_CLASS;
    StringConfigAttribute classStub =
         new StringConfigAttribute(ATTR_CERTMAPPER_CLASS, getMessage(msgID),
                                   true, false, false);
    try
    {
      StringConfigAttribute classAttr =
           (StringConfigAttribute) configEntry.getConfigAttribute(classStub);
      if (classAttr == null)
      {
        msgID = MSGID_CONFIG_CERTMAPPER_NO_CLASS_ATTR;
        unacceptableReason.append(getMessage(msgID));
        return false;
      }
      else
      {
        className = classAttr.activeValue();
      }
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "configAddIsAcceptable", e);

      msgID = MSGID_CONFIG_CERTMAPPER_CANNOT_DETERMINE_CLASS;
      unacceptableReason.append(getMessage(msgID,
                                           stackTraceToSingleLineString(e)));
      return false;
    }


    // Try to load the class and instantiate it as a certificate mapper.
    Class certificateMapperClass;
    try
    {
      // FIXME -- Should we use a custom class loader for this?
      certificateMapperClass = Class.forName(className);
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "configAddIsAcceptable", e);

      msgID = MSGID_CONFIG_CERTMAPPER_CANNOT_LOAD_CLASS;
      unacceptableReason.append(getMessage(msgID, String.valueOf(className),
                                           stackTraceToSingleLineString(e)));
      return false;
    }

    try
    {
      CertificateMapper certificateMapper =
           (CertificateMapper) certificateMapperClass.newInstance();
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "configAddIsAcceptable", e);

      msgID = MSGID_CONFIG_CERTMAPPER_CANNOT_INSTANTIATE_CLASS;
      unacceptableReason.append(getMessage(msgID, String.valueOf(className),
                                           stackTraceToSingleLineString(e)));
      return false;
    }


    // If we've gotten to this point, then it is acceptable as far as we are
    // concerned.  If it is unacceptable according to the configuration, then
    // the certificate mapper itself will make that determination.
    return true;
  }



  /**
   * Attempts to apply a new configuration based on the provided added entry.
   *
   * @param  configEntry  The new configuration entry that contains the
   *                      configuration to apply.
   *
   * @return  Information about the result of processing the configuration
   *          change.
   */
  public ConfigChangeResult applyConfigurationAdd(ConfigEntry configEntry)
  {
    assert debugEnter(CLASS_NAME, "applyConfigurationAdd",
                      String.valueOf(configEntry));

    ResultCode        resultCode          = ResultCode.SUCCESS;
    boolean           adminActionRequired = false;
    ArrayList<String> messages            = new ArrayList<String>();


    // Get the DN of the provided entry and see if it is the DN that we expect
    // for the certificate mapper configuration.  If it is not, then it's not an
    // entry that we care about so return without doing anything.
    DN providedEntryDN = configEntry.getDN();
    DN expectedEntryDN;
    try
    {
      expectedEntryDN = DN.decode(DN_CERTMAPPER_CONFIG);
    }
    catch (DirectoryException de)
    {
      assert debugException(CLASS_NAME, "applyConfigurationAdd", de);

      messages.add(de.getErrorMessage());
      resultCode = de.getResultCode();
      return new ConfigChangeResult(resultCode, adminActionRequired, messages);
    }

    if (! providedEntryDN.equals(expectedEntryDN))
    {
      return new ConfigChangeResult(resultCode, adminActionRequired, messages);
    }


    // Register as a change listener of the provided entry so that we will be
    // notified of changes to it.  We will also want to register a delete
    // listener with its parent to allow us to determine if the entry is
    // deleted.
    configEntry.registerChangeListener(this);
    try
    {
      DN parentDN = configEntry.getDN().getParent();
      ConfigEntry parentEntry = DirectoryServer.getConfigEntry(parentDN);
      if (parentEntry != null)
      {
        parentEntry.registerDeleteListener(this);
      }
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "applyConfigurationAdd", e);

      logError(ErrorLogCategory.CONFIGURATION, ErrorLogSeverity.SEVERE_WARNING,
               MSGID_CONFIG_CERTMAPPER_CANNOT_REGISTER_DELETE_LISTENER,
               stackTraceToSingleLineString(e));
    }


    // See if the entry indicates whether the certificate mapper should be
    // enabled.
    int msgID = MSGID_CONFIG_CERTMAPPER_DESCRIPTION_ENABLED;
    BooleanConfigAttribute enabledStub =
         new BooleanConfigAttribute(ATTR_CERTMAPPER_ENABLED, getMessage(msgID),
                                    false);
    try
    {
      BooleanConfigAttribute enabledAttr =
           (BooleanConfigAttribute)
           configEntry.getConfigAttribute(enabledStub);
      if (enabledAttr == null)
      {
        // The attribute is not present, so the certificate mapper will be
        // disabled.  Log a warning message and return.
        messages.add(getMessage(MSGID_CONFIG_CERTMAPPER_NO_ENABLED_ATTR));
        resultCode = ResultCode.OBJECTCLASS_VIOLATION;
        return new ConfigChangeResult(resultCode, adminActionRequired,
                                      messages);
      }
      else if (! enabledAttr.activeValue())
      {
        // The certificate mapper is explicitly disabled.  Log a mild warning
        // and return.
        messages.add(getMessage(MSGID_CONFIG_CERTMAPPER_DISABLED));
        return new ConfigChangeResult(resultCode, adminActionRequired,
                                      messages);
      }
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "applyConfigurationAdd", e);

      msgID = MSGID_CONFIG_CERTMAPPER_UNABLE_TO_DETERMINE_ENABLED_STATE;
      messages.add(getMessage(msgID, stackTraceToSingleLineString(e)));
      resultCode = DirectoryServer.getServerErrorResultCode();
      return new ConfigChangeResult(resultCode, adminActionRequired, messages);
    }


    // See if it specifies the class name for the certificate mapper
    // implementation.
    String className;
    msgID = MSGID_CONFIG_CERTMAPPER_DESCRIPTION_CLASS;
    StringConfigAttribute classStub =
         new StringConfigAttribute(ATTR_CERTMAPPER_CLASS, getMessage(msgID),
                                   true, false, false);
    try
    {
      StringConfigAttribute classAttr =
           (StringConfigAttribute) configEntry.getConfigAttribute(classStub);
      if (classAttr == null)
      {
        messages.add(getMessage(MSGID_CONFIG_CERTMAPPER_NO_CLASS_ATTR));
        resultCode = ResultCode.OBJECTCLASS_VIOLATION;
        return new ConfigChangeResult(resultCode, adminActionRequired,
                                      messages);
      }
      else
      {
        className = classAttr.activeValue();
      }
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "applyConfigurationAdd", e);

      msgID = MSGID_CONFIG_CERTMAPPER_CANNOT_DETERMINE_CLASS;
      messages.add(getMessage(msgID, stackTraceToSingleLineString(e)));
      resultCode = DirectoryServer.getServerErrorResultCode();
      return new ConfigChangeResult(resultCode, adminActionRequired, messages);
    }


    // Try to load the class and instantiate it as a certificate mapper.
    Class certificateMapperClass;
    try
    {
      // FIXME -- Should we use a custom class loader for this?
      certificateMapperClass = Class.forName(className);
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "applyConfigurationAdd", e);

      msgID = MSGID_CONFIG_CERTMAPPER_CANNOT_LOAD_CLASS;
      messages.add(getMessage(msgID, String.valueOf(className),
                              stackTraceToSingleLineString(e)));
      resultCode = DirectoryServer.getServerErrorResultCode();
      return new ConfigChangeResult(resultCode, adminActionRequired, messages);
    }

    CertificateMapper certificateMapper;
    try
    {
      certificateMapper =
           (CertificateMapper) certificateMapperClass.newInstance();
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "applyConfigurationAdd", e);

      msgID = MSGID_CONFIG_CERTMAPPER_CANNOT_INSTANTIATE_CLASS;
      messages.add(getMessage(msgID, String.valueOf(className),
                              stackTraceToSingleLineString(e)));
      resultCode = DirectoryServer.getServerErrorResultCode();
      return new ConfigChangeResult(resultCode, adminActionRequired, messages);
    }


    // Try to initialize the certificate mapper with the contents of the
    // configuration entry.
    try
    {
      certificateMapper.initializeCertificateMapper(configEntry);
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "applyConfigurationAdd", e);

      msgID = MSGID_CONFIG_CERTMAPPER_CANNOT_INITIALIZE;
      messages.add(getMessage(msgID, String.valueOf(className),
                              stackTraceToSingleLineString(e)));
      resultCode = DirectoryServer.getServerErrorResultCode();
      return new ConfigChangeResult(resultCode, adminActionRequired, messages);
    }


    // Install the new certificate mapper in the server.  We don't need to do
    // anything to get rid of the previous default mapper since it doesn't
    // consume any resources.
    DirectoryServer.setCertificateMapper(certificateMapper);


    return new ConfigChangeResult(resultCode, adminActionRequired, messages);
  }



  /**
   * Indicates whether it is acceptable to remove the provided configuration
   * entry.
   *
   * @param  configEntry         The configuration entry that will be removed
   *                             from the configuration.
   * @param  unacceptableReason  A buffer to which this method can append a
   *                             human-readable message explaining why the
   *                             proposed delete is not acceptable.
   *
   * @return  <CODE>true</CODE> if the proposed entry may be removed from the
   *          configuration, or <CODE>false</CODE> if not.
   */
  public boolean configDeleteIsAcceptable(ConfigEntry configEntry,
                                          StringBuilder unacceptableReason)
  {
    assert debugEnter(CLASS_NAME, "configDeleteIsAcceptable",
                      String.valueOf(configEntry), "java.lang.StringBuilder");


    // Get the DN of the provided entry and see if it is the DN that we expect
    // for the certificate mapper configuration.  If it is not, then it's not an
    // entry that we care about so return true.
    DN providedEntryDN = configEntry.getDN();
    DN expectedEntryDN;
    try
    {
      expectedEntryDN = DN.decode(DN_CERTMAPPER_CONFIG);
    }
    catch (DirectoryException de)
    {
      assert debugException(CLASS_NAME, "configAddIsAcceptable", de);

      unacceptableReason.append(de.getErrorMessage());
      return false;
    }

    if (! providedEntryDN.equals(expectedEntryDN))
    {
      return true;
    }


    // Determine whether there is a valid certificate mapper installed (i.e.,
    // not the default mapper).  If a valid mapper is installed, then we will
    // not allow the entry to be removed.
    CertificateMapper installedMapper =
         DirectoryServer.getCertificateMapper();
    if (! (installedMapper instanceof SubjectEqualsDNCertificateMapper))
    {
      int msgID = MSGID_CONFIG_CERTMAPPER_CANNOT_REMOVE_ACTIVE_PROVIDER;
      unacceptableReason.append(getMessage(msgID,
                                     installedMapper.getClass().getName()));
      return false;
    }


    // If we've gotten to this point, then it is acceptable as far as we are
    // concerned.  If it is unacceptable according to the configuration, then
    // the certificate mapper itself will make that determination.
    return true;
  }



  /**
   * Attempts to apply a new configuration based on the provided deleted entry.
   *
   * @param  configEntry  The new configuration entry that has been deleted.
   *
   * @return  Information about the result of processing the configuration
   *          change.
   */
  public ConfigChangeResult applyConfigurationDelete(ConfigEntry configEntry)
  {
    assert debugEnter(CLASS_NAME, "applyConfigurationDelete",
                      String.valueOf(configEntry));

    ResultCode        resultCode          = ResultCode.SUCCESS;
    boolean           adminActionRequired = false;
    ArrayList<String> messages            = new ArrayList<String>();


    // Since we can never delete an active configuration, there is nothing that
    // we need to do if a delete does go through.
    return new ConfigChangeResult(resultCode, adminActionRequired, messages);
  }
}

