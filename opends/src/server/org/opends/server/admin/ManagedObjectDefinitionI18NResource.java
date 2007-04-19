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



import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;



/**
 * A class for retrieving internationalized resource properties
 * associated with a managed object definition.
 */
public final class ManagedObjectDefinitionI18NResource {

  // Application-wide set of instances.
  private static final Map<String, ManagedObjectDefinitionI18NResource>
    INSTANCES = new HashMap<String, ManagedObjectDefinitionI18NResource>();



  /**
   * Gets the internationalized resource instance which can be used to
   * retrieve the localized descriptions for the managed objects and
   * their associated properties and relations.
   *
   * @return Returns the I18N resource instance.
   */
  public static ManagedObjectDefinitionI18NResource getInstance() {
    return getInstance("admin.messages");
  }



  /**
   * Gets the internationalized resource instance for the named
   * profile.
   *
   * @param profile
   *          The name of the profile.
   * @return Returns the I18N resource instance for the named profile.
   */
  public static ManagedObjectDefinitionI18NResource getInstanceForProfile(
      String profile) {
    return getInstance("admin.profiles." + profile);
  }



  // Get a resource instance creating it if necessary.
  private synchronized static ManagedObjectDefinitionI18NResource getInstance(
      String prefix) {
    ManagedObjectDefinitionI18NResource instance = INSTANCES
        .get(prefix);

    if (instance == null) {
      instance = new ManagedObjectDefinitionI18NResource(prefix);
      INSTANCES.put(prefix, instance);
    }

    return instance;
  }



  // Mapping from definition to locale-based resource bundle.
  private final Map<AbstractManagedObjectDefinition,
    Map<Locale, ResourceBundle>> resources;



  // The resource name prefix.
  private final String prefix;



  // Private constructor.
  private ManagedObjectDefinitionI18NResource(String prefix) {
    this.resources = new HashMap<AbstractManagedObjectDefinition,
      Map<Locale, ResourceBundle>>();
    this.prefix = prefix;
  }



  /**
   * Get the internationalized message associated with the specified
   * key in the default locale.
   *
   * @param d
   *          The managed object definition.
   * @param key
   *          The resource key.
   * @return Returns the internationalized message associated with the
   *         specified key in the default locale.
   * @throws MissingResourceException
   *           If the key was not found.
   */
  public String getMessage(AbstractManagedObjectDefinition d,
      String key) throws MissingResourceException {
    return getMessage(d, key, Locale.getDefault(), (String[]) null);
  }



  /**
   * Get the internationalized message associated with the specified
   * key and locale.
   *
   * @param d
   *          The managed object definition.
   * @param key
   *          The resource key.
   * @param locale
   *          The locale.
   * @return Returns the internationalized message associated with the
   *         specified key and locale.
   * @throws MissingResourceException
   *           If the key was not found.
   */
  public String getMessage(AbstractManagedObjectDefinition d,
      String key, Locale locale) throws MissingResourceException {
    return getMessage(d, key, locale, (String[]) null);
  }



  /**
   * Get the parameterized internationalized message associated with
   * the specified key and locale.
   *
   * @param d
   *          The managed object definition.
   * @param key
   *          The resource key.
   * @param locale
   *          The locale.
   * @param args
   *          Arguments that should be inserted into the retrieved
   *          message.
   * @return Returns the internationalized message associated with the
   *         specified key and locale.
   * @throws MissingResourceException
   *           If the key was not found.
   */
  public String getMessage(AbstractManagedObjectDefinition d,
      String key, Locale locale, String... args)
      throws MissingResourceException {
    ResourceBundle resource = getResourceBundle(d, locale);

    if (args == null) {
      return resource.getString(key);
    } else {
      MessageFormat mf = new MessageFormat(resource.getString(key));
      return mf.format(args);
    }
  }



  /**
   * Get the parameterized internationalized message associated with
   * the specified key in the default locale.
   *
   * @param d
   *          The managed object definition.
   * @param key
   *          The resource key.
   * @param args
   *          Arguments that should be inserted into the retrieved
   *          message.
   * @return Returns the internationalized message associated with the
   *         specified key in the default locale.
   * @throws MissingResourceException
   *           If the key was not found.
   */
  public String getMessage(AbstractManagedObjectDefinition d,
      String key, String... args) throws MissingResourceException {
    return getMessage(d, key, Locale.getDefault(), args);
  }



  // Retrieve the resource bundle associated with a managed object and
  // locale, lazily loading it if necessary.
  private synchronized ResourceBundle getResourceBundle(
      AbstractManagedObjectDefinition d, Locale locale)
      throws MissingResourceException {
    // First get the locale-resource mapping, creating it if
    // necessary.
    Map<Locale, ResourceBundle> map = resources.get(d);
    if (map == null) {
      map = new HashMap<Locale, ResourceBundle>();
      resources.put(d, map);
    }

    // Now get the resource based on the locale, loading it if
    // necessary.
    ResourceBundle resourceBundle = map.get(locale);
    if (resourceBundle == null) {
      String baseName = prefix + "." + d.getClass().getName();
      resourceBundle = ResourceBundle.getBundle(baseName, locale,
          ClassLoaderProvider.getInstance().getClassLoader());
      map.put(locale, resourceBundle);
    }

    return resourceBundle;
  }
}
