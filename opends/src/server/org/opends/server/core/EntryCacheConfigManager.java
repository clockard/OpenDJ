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
 *      Portions Copyright 2006-2007 Sun Microsystems, Inc.
 */
package org.opends.server.core;
import org.opends.messages.Message;



import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opends.server.admin.ClassPropertyDefinition;
import org.opends.server.admin.server.ConfigurationAddListener;
import org.opends.server.admin.server.ConfigurationChangeListener;
import org.opends.server.admin.server.ConfigurationDeleteListener;
import org.opends.server.admin.server.ServerManagementContext;
import org.opends.server.admin.std.server.EntryCacheCfg;
import org.opends.server.admin.std.server.RootCfg;
import org.opends.server.admin.std.meta.EntryCacheCfgDefn;
import org.opends.server.api.EntryCache;
import org.opends.server.config.ConfigException;
import org.opends.server.extensions.DefaultEntryCache;
import org.opends.server.loggers.debug.DebugTracer;
import org.opends.server.types.ConfigChangeResult;
import org.opends.server.types.DebugLogLevel;
import org.opends.server.types.InitializationException;
import org.opends.server.types.ResultCode;
import org.opends.messages.MessageBuilder;

import static org.opends.server.loggers.debug.DebugLogger.*;
import static org.opends.server.loggers.ErrorLogger.*;
import static org.opends.messages.ConfigMessages.*;
import static org.opends.server.util.StaticUtils.*;



/**
 * This class defines a utility that will be used to manage the configuration
 * for the Directory Server entry cache.  Only a single entry cache may be
 * defined, but if it is absent or disabled, then a default cache will be used.
 */
public class EntryCacheConfigManager
       implements
          ConfigurationChangeListener <EntryCacheCfg>,
          ConfigurationAddListener    <EntryCacheCfg>,
          ConfigurationDeleteListener <EntryCacheCfg>
{
  /**
   * The tracer object for the debug logger.
   */
  private static final DebugTracer TRACER = getTracer();

  // The current entry cache registered in the server
  private EntryCache _entryCache = null;

  // The default entry cache to use when no entry cache has been configured
  // or when the configured entry cache could not be initialized.
  private EntryCache _defaultEntryCache = null;


  /**
   * Creates a new instance of this entry cache config manager.
   */
  public EntryCacheConfigManager()
  {
    // No implementation is required.
  }


  /**
   * Initializes the default entry cache.
   * This should only be called at Directory Server startup.
   *
   * @throws  InitializationException  If a problem occurs while trying to
   *                                   install the default entry cache.
   */
  public void initializeDefaultEntryCache()
         throws InitializationException
  {
    try
    {
      DefaultEntryCache defaultCache = new DefaultEntryCache();
      defaultCache.initializeEntryCache(null);
      DirectoryServer.setEntryCache(defaultCache);
      _defaultEntryCache = defaultCache;
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }

      Message message = ERR_CONFIG_ENTRYCACHE_CANNOT_INSTALL_DEFAULT_CACHE.get(
          stackTraceToSingleLineString(e));
      throw new InitializationException(message, e);
    }

  }


  /**
   * Initializes the configuration associated with the Directory Server entry
   * cache.  This should only be called at Directory Server startup.  If an
   * error occurs, then a message will be logged and the default entry cache
   * will be installed.
   *
   * @throws  ConfigException  If a configuration problem causes the entry
   *                           cache initialization process to fail.
   */
  public void initializeEntryCache()
         throws ConfigException
  {
    // Get the root configuration object.
    ServerManagementContext managementContext =
      ServerManagementContext.getInstance();
    RootCfg rootConfiguration =
      managementContext.getRootConfiguration();

    // Default entry cache should be already installed with
    // <CODE>initializeDefaultEntryCache()</CODE> method so
    // that there will be one even if we encounter a problem
    // later.

    // Register as an add and delete listener with the root configuration so we
    // can be notified if any entry cache entry is added or removed.
    rootConfiguration.addEntryCacheAddListener(this);
    rootConfiguration.addEntryCacheDeleteListener(this);

    // If the entry cache configuration is not present then keep the
    // default entry cache already installed.
    if (!rootConfiguration.hasEntryCache())
    {
      logError(WARN_CONFIG_ENTRYCACHE_NO_CONFIG_ENTRY.get());
      return;
    }

    // Get the entry cache configuration.
    EntryCacheCfg configuration = rootConfiguration.getEntryCache();

    // At this point, we have a configuration entry. Register a change
    // listener with it so we can be notified of changes to it over time.
    configuration.addChangeListener(this);

    // Initialize the entry cache.
    if (configuration.isEnabled())
    {
      // Load the entry cache implementation class and install the entry
      // cache with the server.
      String className = configuration.getEntryCacheClass();
      try
      {
        loadAndInstallEntryCache (className);
      }
      catch (InitializationException ie)
      {
        logError(ie.getMessageObject());
      }
    }
  }


  /**
   * {@inheritDoc}
   */
  public boolean isConfigurationChangeAcceptable(
      EntryCacheCfg configuration,
      List<Message> unacceptableReasons
      )
  {
    // returned status -- all is fine by default
    boolean status = true;

    // Get the name of the class and make sure we can instantiate it as an
    // entry cache.
    String className = configuration.getEntryCacheClass();
    try {
      // Load the class but don't initialize it.
      loadEntryCache(className, configuration, false);
    } catch (InitializationException ie) {
      unacceptableReasons.add(ie.getMessageObject());
      status = false;
    }

    return status;
  }


  /**
   * {@inheritDoc}
   */
  public ConfigChangeResult applyConfigurationChange(
      EntryCacheCfg configuration
      )
  {
    // Returned result.
    ConfigChangeResult changeResult = new ConfigChangeResult(
        ResultCode.SUCCESS, false, new ArrayList<Message>()
        );

    // If the new configuration has the entry cache disabled, then install
    // the default entry cache with the server.
    if (! configuration.isEnabled())
    {
      DirectoryServer.setEntryCache (_defaultEntryCache);

      // If an entry cache was installed then clean it.
      if (_entryCache != null)
      {
        _entryCache.finalizeEntryCache();
        _entryCache = null;
      }
      return changeResult;
    }

    // At this point, new configuration is enabled...
    // If the current entry cache is already enabled then we don't do
    // anything unless the class has changed in which case we should
    // indicate that administrative action is required.
    String newClassName = configuration.getEntryCacheClass();
    if (_entryCache !=null)
    {
      String curClassName = _entryCache.getClass().getName();
      boolean classIsNew = (! newClassName.equals (curClassName));
      if (classIsNew)
      {
        changeResult.setAdminActionRequired (true);
      }
      return changeResult;
    }

    // New entry cache is enabled and there were no previous one.
    // Instantiate the new class and initalize it.
    try
    {
      loadAndInstallEntryCache (newClassName);
    }
    catch (InitializationException ie)
    {
      changeResult.addMessage (ie.getMessageObject());
      changeResult.setResultCode (DirectoryServer.getServerErrorResultCode());
      return changeResult;
    }

    return changeResult;
  }


  /**
   * {@inheritDoc}
   */
  public boolean isConfigurationAddAcceptable(
      EntryCacheCfg configuration,
      List<Message> unacceptableReasons
      )
  {
    // returned status -- all is fine by default
    boolean status = true;

    if (configuration.isEnabled())
    {
      // Get the name of the class and make sure we can instantiate it as
      // an entry cache.
      String className = configuration.getEntryCacheClass();
      try
      {
        // Load the class but don't initialize it.
        loadEntryCache(className, configuration, false);
      }
      catch (InitializationException ie)
      {
        unacceptableReasons.add (ie.getMessageObject());
        status = false;
      }
    }

    return status;
  }


  /**
   * {@inheritDoc}
   */
  public ConfigChangeResult applyConfigurationAdd(
      EntryCacheCfg configuration
      )
  {
    // Returned result.
    ConfigChangeResult changeResult = new ConfigChangeResult(
        ResultCode.SUCCESS, false, new ArrayList<Message>()
        );

    // Register a change listener with it so we can be notified of changes
    // to it over time.
    configuration.addChangeListener(this);

    if (configuration.isEnabled())
    {
      // Instantiate the class as an entry cache and initialize it.
      String className = configuration.getEntryCacheClass();
      try
      {
        loadAndInstallEntryCache (className);
      }
      catch (InitializationException ie)
      {
        changeResult.addMessage (ie.getMessageObject());
        changeResult.setResultCode (DirectoryServer.getServerErrorResultCode());
        return changeResult;
      }
    }

    return changeResult;
  }


  /**
   * {@inheritDoc}
   */
  public boolean isConfigurationDeleteAcceptable(
      EntryCacheCfg configuration,
      List<Message> unacceptableReasons
      )
  {
    // If we've gotten to this point, then it is acceptable as far as we are
    // concerned.  If it is unacceptable according to the configuration, then
    // the entry cache itself will make that determination.
    return true;
  }


  /**
   * {@inheritDoc}
   */
  public ConfigChangeResult applyConfigurationDelete(
      EntryCacheCfg configuration
      )
  {
    // Returned result.
    ConfigChangeResult changeResult = new ConfigChangeResult(
        ResultCode.SUCCESS, false, new ArrayList<Message>()
        );

    // If the entry cache was installed then replace it with the
    // default entry cache, and clean it.
    if (_entryCache != null)
    {
      DirectoryServer.setEntryCache (_defaultEntryCache);
      _entryCache.finalizeEntryCache();
      _entryCache = null;
    }

    return changeResult;
  }


  /**
   * Loads the specified class, instantiates it as an entry cache,
   * and optionally initializes that instance. Any initialize entry
   * cache is registered in the server.
   *
   * @param  className      The fully-qualified name of the entry cache
   *                        class to load, instantiate, and initialize.
   * @param  configuration  The configuration to use to initialize the
   *                        entry cache, or {@code null} if the
   *                        entry cache should not be initialized.
   *
   * @throws  InitializationException  If a problem occurred while attempting
   *                                   to initialize the entry cache.
   */
  private void loadAndInstallEntryCache(
    String        className
    )
    throws InitializationException
  {
    EntryCacheCfg configuration;

    // Get the root configuration object.
    ServerManagementContext managementContext =
      ServerManagementContext.getInstance();
    RootCfg rootConfiguration =
      managementContext.getRootConfiguration();

    // Get the entry cache configuration.
    try {
      configuration = rootConfiguration.getEntryCache();
    } catch (ConfigException ce) {
      Message message = ERR_CONFIG_ENTRYCACHE_CANNOT_INITIALIZE_CACHE.get(
        className, (ce.getCause() != null ? ce.getCause().getMessage() :
          stackTraceToSingleLineString(ce)));
      throw new InitializationException(message, ce);
    }

    // Load the entry cache class...
    EntryCache entryCache = loadEntryCache (className, configuration, true);

    // ... and install the entry cache in the server.
    DirectoryServer.setEntryCache(entryCache);
    _entryCache = entryCache;
  }


  /**
   * Loads the specified class, instantiates it as an entry cache, and
   * optionally initializes that instance.
   *
   * @param  className      The fully-qualified name of the entry cache class
   *                        to load, instantiate, and initialize.
   * @param  configuration  The configuration to use to initialize the entry
   *                        cache.  It must not be {@code null}.
   * @param  initialize     Indicates whether the entry cache instance should be
   *                        initialized.
   *
   * @return  The possibly initialized entry cache.
   *
   * @throws  InitializationException  If a problem occurred while attempting
   *                                   to initialize the entry cache.
   */
  private EntryCache<? extends EntryCacheCfg> loadEntryCache(
    String        className,
    EntryCacheCfg configuration,
    boolean initialize
    )
    throws InitializationException
  {
    try
    {
      EntryCacheCfgDefn                   definition;
      ClassPropertyDefinition             propertyDefinition;
      Class<? extends EntryCache>         cacheClass;
      EntryCache<? extends EntryCacheCfg> cache;

      definition = EntryCacheCfgDefn.getInstance();
      propertyDefinition = definition.getEntryCacheClassPropertyDefinition();
      cacheClass = propertyDefinition.loadClass(className, EntryCache.class);

      // If there is some entry cache instance already initialized work with
      // it instead of creating a new one unless explicit init is requested.
      if (initialize || (_entryCache == null)) {
        cache = (EntryCache<? extends EntryCacheCfg>) cacheClass.newInstance();
      } else {
        cache = (EntryCache<? extends EntryCacheCfg>) _entryCache;
      }

      if (initialize)
      {
        Method method = cache.getClass().getMethod(
            "initializeEntryCache",
            configuration.definition().getServerConfigurationClass()
            );
        method.invoke(cache, configuration);
      }
      // This will check if configuration is acceptable on disabled
      // and uninitialized cache instance that has no "acceptable"
      // change listener registered to invoke and verify on its own.
      else if (!configuration.isEnabled())
      {
        Method method = cache.getClass().getMethod("isConfigurationAcceptable",
                                                   EntryCacheCfg.class,
                                                   List.class);

        List<Message> unacceptableReasons = new ArrayList<Message>();
        Boolean acceptable = (Boolean) method.invoke(cache, configuration,
                                                     unacceptableReasons);
        if (! acceptable)
        {
          MessageBuilder buffer = new MessageBuilder();
          if (! unacceptableReasons.isEmpty())
          {
            Iterator<Message> iterator = unacceptableReasons.iterator();
            buffer.append(iterator.next());
            while (iterator.hasNext())
            {
              buffer.append(".  ");
              buffer.append(iterator.next());
            }
          }

          Message message = ERR_CONFIG_ENTRYCACHE_CONFIG_NOT_ACCEPTABLE.get(
              String.valueOf(configuration.dn()), buffer.toString());
          throw new InitializationException(message);
        }
      }

      return cache;
    }
    catch (Exception e)
    {
      if (debugEnabled()) {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }

      if (!initialize) {
        if (e instanceof InitializationException) {
          throw (InitializationException) e;
        } else {
          Message message = ERR_CONFIG_ENTRYCACHE_CONFIG_NOT_ACCEPTABLE.get(
            String.valueOf(configuration.dn()), e.getCause() != null ?
              e.getCause().getMessage() : stackTraceToSingleLineString(e));
          throw new InitializationException(message);
        }
      }
      Message message = ERR_CONFIG_ENTRYCACHE_CANNOT_INITIALIZE_CACHE.get(
        className, (e.getCause() != null ? e.getCause().getMessage() :
          stackTraceToSingleLineString(e)));
      throw new InitializationException(message, e);
    }
  }

}
