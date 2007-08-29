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
package org.opends.server.tools.dsconfig;



import static org.opends.messages.DSConfigMessages.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.opends.messages.Message;
import org.opends.messages.MessageBuilder;
import org.opends.server.admin.AbsoluteInheritedDefaultBehaviorProvider;
import org.opends.server.admin.AbstractManagedObjectDefinition;
import org.opends.server.admin.AliasDefaultBehaviorProvider;
import org.opends.server.admin.BooleanPropertyDefinition;
import org.opends.server.admin.DefaultBehaviorProviderVisitor;
import org.opends.server.admin.DefinedDefaultBehaviorProvider;
import org.opends.server.admin.EnumPropertyDefinition;
import org.opends.server.admin.IllegalPropertyValueException;
import org.opends.server.admin.IllegalPropertyValueStringException;
import org.opends.server.admin.ManagedObjectDefinition;
import org.opends.server.admin.PropertyDefinition;
import org.opends.server.admin.PropertyDefinitionUsageBuilder;
import org.opends.server.admin.PropertyDefinitionVisitor;
import org.opends.server.admin.PropertyIsMandatoryException;
import org.opends.server.admin.PropertyIsReadOnlyException;
import org.opends.server.admin.PropertyIsSingleValuedException;
import org.opends.server.admin.PropertyOption;
import org.opends.server.admin.RelativeInheritedDefaultBehaviorProvider;
import org.opends.server.admin.UndefinedDefaultBehaviorProvider;
import org.opends.server.admin.UnknownPropertyDefinitionException;
import org.opends.server.admin.client.ManagedObject;
import org.opends.server.util.Validator;
import org.opends.server.util.cli.CLIException;
import org.opends.server.util.cli.ConsoleApplication;
import org.opends.server.util.cli.HelpCallback;
import org.opends.server.util.cli.Menu;
import org.opends.server.util.cli.MenuBuilder;
import org.opends.server.util.cli.MenuCallback;
import org.opends.server.util.cli.MenuResult;
import org.opends.server.util.table.TableBuilder;
import org.opends.server.util.table.TextTablePrinter;



/**
 * Common methods used for interactively editing properties.
 */
public final class PropertyValueEditor {

  /**
   * A help call-back which displays a description and summary of a
   * component and its properties.
   */
  private static final class ComponentHelpCallback implements HelpCallback {

    // The managed object being edited.
    private final ManagedObject<?> mo;

    // The properties that can be edited.
    private final Collection<PropertyDefinition<?>> properties;



    // Creates a new component helper for the specified property.
    private ComponentHelpCallback(ManagedObject<?> mo,
        Collection<PropertyDefinition<?>> c) {
      this.mo = mo;
      this.properties = c;
    }



    /**
     * {@inheritDoc}
     */
    public void display(ConsoleApplication app) {
      app.println();
      HelpSubCommandHandler.displaySingleComponent(app, mo
          .getManagedObjectDefinition(), properties);
      app.println();
      app.pressReturnToContinue();
    }
  }



  /**
   * A simple interface for querying and retrieving common default
   * behavior properties.
   */
  private static final class DefaultBehaviorQuery<T> {

    /**
     * The type of default behavior.
     */
    private enum Type {
      /**
       * Alias default behavior.
       */
      ALIAS,

      /**
       * Defined default behavior.
       */
      DEFINED,

      /**
       * Inherited default behavior.
       */
      INHERITED,

      /**
       * Undefined default behavior.
       */
      UNDEFINED;
    };



    /**
     * Create a new default behavior query object based on the provied
     * property definition.
     *
     * @param <T>
     *          The type of property definition.
     * @param pd
     *          The property definition.
     * @return The default behavior query object.
     */
    public static <T> DefaultBehaviorQuery<T> query(PropertyDefinition<T> pd) {
      DefaultBehaviorProviderVisitor<T, DefaultBehaviorQuery<T>,
        PropertyDefinition<T>> visitor =
          new DefaultBehaviorProviderVisitor<T, DefaultBehaviorQuery<T>,
          PropertyDefinition<T>>() {

        /**
         * {@inheritDoc}
         */
        public DefaultBehaviorQuery<T> visitAbsoluteInherited(
            AbsoluteInheritedDefaultBehaviorProvider<T> d,
            PropertyDefinition<T> p) {
          AbstractManagedObjectDefinition<?, ?> mod = d
              .getManagedObjectDefinition();
          String propertyName = d.getPropertyName();
          PropertyDefinition<?> pd2 = mod.getPropertyDefinition(propertyName);

          DefaultBehaviorQuery<?> query = query(pd2);
          return new DefaultBehaviorQuery<T>(Type.INHERITED, query
              .getAliasDescription());
        }



        /**
         * {@inheritDoc}
         */
        public DefaultBehaviorQuery<T> visitAlias(
            AliasDefaultBehaviorProvider<T> d, PropertyDefinition<T> p) {
          return new DefaultBehaviorQuery<T>(Type.ALIAS, d.getSynopsis());
        }



        /**
         * {@inheritDoc}
         */
        public DefaultBehaviorQuery<T> visitDefined(
            DefinedDefaultBehaviorProvider<T> d, PropertyDefinition<T> p) {
          return new DefaultBehaviorQuery<T>(Type.DEFINED, null);
        }



        /**
         * {@inheritDoc}
         */
        public DefaultBehaviorQuery<T> visitRelativeInherited(
            RelativeInheritedDefaultBehaviorProvider<T> d,
            PropertyDefinition<T> p) {
          AbstractManagedObjectDefinition<?, ?> mod = d
              .getManagedObjectDefinition();
          String propertyName = d.getPropertyName();
          PropertyDefinition<?> pd2 = mod.getPropertyDefinition(propertyName);

          DefaultBehaviorQuery<?> query = query(pd2);
          return new DefaultBehaviorQuery<T>(Type.INHERITED, query
              .getAliasDescription());
        }



        /**
         * {@inheritDoc}
         */
        public DefaultBehaviorQuery<T> visitUndefined(
            UndefinedDefaultBehaviorProvider<T> d, PropertyDefinition<T> p) {
          return new DefaultBehaviorQuery<T>(Type.UNDEFINED, null);
        }
      };

      return pd.getDefaultBehaviorProvider().accept(visitor, pd);
    }

    // The description of the behavior if it is an alias default
    // behavior.
    private final Message aliasDescription;

    // The type of behavior.
    private final Type type;



    // Private constructor.
    private DefaultBehaviorQuery(Type type, Message aliasDescription) {
      this.type = type;
      this.aliasDescription = aliasDescription;
    }



    /**
     * Gets the detailed description of this default behavior if it is
     * an alias default behavior or if it inherits from an alias
     * default behavior.
     *
     * @return Returns the detailed description of this default
     *         behavior if it is an alias default behavior or if it
     *         inherits from an alias default behavior, otherwise
     *         <code>null</code>.
     */
    public Message getAliasDescription() {
      return aliasDescription;
    }



    /**
     * Determines whether or not the default behavior is alias.
     *
     * @return Returns <code>true</code> if the default behavior is
     *         alias.
     */
    public boolean isAlias() {
      return type == Type.ALIAS;
    }



    /**
     * Determines whether or not the default behavior is defined.
     *
     * @return Returns <code>true</code> if the default behavior is
     *         defined.
     */
    public boolean isDefined() {
      return type == Type.DEFINED;
    }



    /**
     * Determines whether or not the default behavior is inherited.
     *
     * @return Returns <code>true</code> if the default behavior is
     *         inherited.
     */
    public boolean isInherited() {
      return type == Type.INHERITED;
    }



    /**
     * Determines whether or not the default behavior is undefined.
     *
     * @return Returns <code>true</code> if the default behavior is
     *         undefined.
     */
    public boolean isUndefined() {
      return type == Type.UNDEFINED;
    }

  }



  /**
   * A property definition visitor which initializes mandatory
   * properties.
   */
  private final class MandatoryPropertyInitializer extends
      PropertyDefinitionVisitor<MenuResult<Void>, ManagedObject<?>> {

    // Any exception that was caught during processing.
    private CLIException e = null;



    // Private constructor.
    private MandatoryPropertyInitializer() {
      // No implementation required.
    }



    /**
     * Read the initial value(s) for a mandatory property.
     *
     * @param mo
     *          The managed object.
     * @param pd
     *          The property definition.
     * @return Returns <code>true</code> if new values were read
     *         successfully, or <code>false</code> if no values were
     *         read and the user chose to quit.
     * @throws CLIException
     *           If the user input could not be retrieved for some
     *           reason.
     */
    public MenuResult<Void> read(ManagedObject<?> mo, PropertyDefinition<?> pd)
        throws CLIException {
      displayPropertyHeader(app, pd);

      MenuResult<Void> result = pd.accept(this, mo);

      if (e != null) {
        throw e;
      } else {
        return result;
      }
    }



    /**
     * /** {@inheritDoc}
     */
    @Override
    public MenuResult<Void> visitBoolean(BooleanPropertyDefinition d,
        ManagedObject<?> p) {
      MenuBuilder<Boolean> builder = new MenuBuilder<Boolean>(app);

      builder
          .setPrompt(INFO_EDITOR_PROMPT_SELECT_VALUE_SINGLE.get(d.getName()));

      builder
          .addNumberedOption(INFO_VALUE_TRUE.get(), MenuResult.success(true));
      builder.addNumberedOption(INFO_VALUE_FALSE.get(), MenuResult
          .success(false));

      builder.addHelpOption(new PropertyHelpCallback(p
          .getManagedObjectDefinition(), d));
      if (app.isMenuDrivenMode()) {
        builder.addCancelOption(true);
      }
      builder.addQuitOption();

      Menu<Boolean> menu = builder.toMenu();
      try {
        app.println();
        MenuResult<Boolean> result = menu.run();

        if (result.isQuit()) {
          return MenuResult.quit();
        } else if (result.isCancel()) {
          return MenuResult.cancel();
        } else {
          p.setPropertyValue(d, result.getValue());
          return MenuResult.success();
        }
      } catch (CLIException e) {
        this.e = e;
        return MenuResult.cancel();
      }
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> MenuResult<Void> visitEnum(
        EnumPropertyDefinition<E> d, ManagedObject<?> p) {
      MenuBuilder<E> builder = new MenuBuilder<E>(app);
      builder.setMultipleColumnThreshold(MULTI_COLUMN_THRESHOLD);

      if (d.hasOption(PropertyOption.MULTI_VALUED)) {
        builder
            .setPrompt(INFO_EDITOR_PROMPT_SELECT_VALUE_MULTI.get(d.getName()));
        builder.setAllowMultiSelect(true);
      } else {
        builder.setPrompt(INFO_EDITOR_PROMPT_SELECT_VALUE_SINGLE
            .get(d.getName()));
      }

      Set<E> values = new TreeSet<E>(d);
      values.addAll(EnumSet.allOf(d.getEnumClass()));
      for (E value : values) {
        Message option = getPropertyValues(d, Collections.singleton(value));
        builder.addNumberedOption(option, MenuResult.success(value));
      }

      builder.addHelpOption(new PropertyHelpCallback(p
          .getManagedObjectDefinition(), d));
      if (app.isMenuDrivenMode()) {
        builder.addCancelOption(true);
      }
      builder.addQuitOption();

      Menu<E> menu = builder.toMenu();
      try {
        app.println();
        MenuResult<E> result = menu.run();

        if (result.isQuit()) {
          return MenuResult.quit();
        } else if (result.isCancel()) {
          return MenuResult.cancel();
        } else {
          p.setPropertyValues(d, result.getValues());
          return MenuResult.success();
        }
      } catch (CLIException e) {
        this.e = e;
        return MenuResult.cancel();
      }
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public <T> MenuResult<Void> visitUnknown(PropertyDefinition<T> d,
        ManagedObject<?> p) throws UnknownPropertyDefinitionException {
      PropertyDefinitionUsageBuilder b = new PropertyDefinitionUsageBuilder(
          true);
      app.println();
      app.println(INFO_EDITOR_HEADING_SYNTAX.get(b.getUsage(d)), 4);

      // Set the new property value(s).
      try {
        p.setPropertyValues(d, readPropertyValues(app, p
            .getManagedObjectDefinition(), d));
        return MenuResult.success();
      } catch (CLIException e) {
        this.e = e;
        return MenuResult.cancel();
      }
    }

  }



  /**
   * A menu call-back for editing a modifiable multi-valued property.
   */
  private static final class MultiValuedPropertyEditor extends
      PropertyDefinitionVisitor<MenuResult<Boolean>, ConsoleApplication>
      implements MenuCallback<Boolean> {

    // Any exception that was caught during processing.
    private CLIException e = null;

    // The managed object being edited.
    private final ManagedObject<?> mo;

    // The property to be edited.
    private final PropertyDefinition<?> pd;



    // Creates a new property editor for the specified property.
    private MultiValuedPropertyEditor(ManagedObject<?> mo,
        PropertyDefinition<?> pd) {
      Validator.ensureTrue(pd.hasOption(PropertyOption.MULTI_VALUED));

      this.mo = mo;
      this.pd = pd;
    }



    /**
     * {@inheritDoc}
     */
    public MenuResult<Boolean> invoke(ConsoleApplication app)
        throws CLIException {
      displayPropertyHeader(app, pd);

      MenuResult<Boolean> result = pd.accept(this, app);
      if (e != null) {
        throw e;
      } else {
        return result;
      }
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Enum<T>> MenuResult<Boolean> visitEnum(
        final EnumPropertyDefinition<T> d, ConsoleApplication app) {
      final SortedSet<T> defaultValues = mo.getPropertyDefaultValues(d);
      final SortedSet<T> oldValues = mo.getPropertyValues(d);
      final SortedSet<T> currentValues = mo.getPropertyValues(d);

      boolean isFirst = true;
      while (true) {
        if (!isFirst) {
          app.println();
          app.println(INFO_EDITOR_HEADING_CONFIGURE_PROPERTY_CONT.get(d
              .getName()));
        } else {
          isFirst = false;
        }

        if (currentValues.size() > 1) {
          app.println();
          app.println(INFO_EDITOR_HEADING_VALUES_SUMMARY.get(d.getName()));
          app.println();
          displayPropertyValues(app, d, currentValues);
        }

        // Create the add values call-back.
        MenuCallback<Boolean> addCallback = null;

        final EnumSet<T> values = EnumSet.allOf(d.getEnumClass());
        values.removeAll(currentValues);

        if (!values.isEmpty()) {
          addCallback = new MenuCallback<Boolean>() {

            public MenuResult<Boolean> invoke(ConsoleApplication app)
                throws CLIException {
              MenuBuilder<T> builder = new MenuBuilder<T>(app);

              builder.setPrompt(INFO_EDITOR_PROMPT_SELECT_VALUES_ADD.get());
              builder.setAllowMultiSelect(true);
              builder.setMultipleColumnThreshold(MULTI_COLUMN_THRESHOLD);

              for (T value : values) {
                Message svalue = getPropertyValues(d, Collections
                    .singleton(value));
                builder.addNumberedOption(svalue, MenuResult.success(value));
              }

              if (values.size() > 1) {
                // No point in having this option if there's only one
                // possible value.
                builder.addNumberedOption(INFO_EDITOR_OPTION_ADD_ALL_VALUES
                    .get(), MenuResult.success(values));
              }

              builder.addHelpOption(new PropertyHelpCallback(mo
                  .getManagedObjectDefinition(), d));

              builder.addCancelOption(true);
              builder.addQuitOption();

              app.println();
              app.println();
              Menu<T> menu = builder.toMenu();
              MenuResult<T> result = menu.run();

              if (result.isSuccess()) {
                // Set the new property value(s).
                currentValues.addAll(result.getValues());
                app.println();
                app.pressReturnToContinue();
                return MenuResult.success(false);
              } else if (result.isCancel()) {
                app.println();
                app.pressReturnToContinue();
                return MenuResult.success(false);
              } else {
                return MenuResult.quit();
              }
            }

          };
        }

        // Create the remove values call-back.
        MenuCallback<Boolean> removeCallback = new MenuCallback<Boolean>() {

          public MenuResult<Boolean> invoke(ConsoleApplication app)
              throws CLIException {
            MenuBuilder<T> builder = new MenuBuilder<T>(app);

            builder.setPrompt(INFO_EDITOR_PROMPT_SELECT_VALUES_REMOVE.get());
            builder.setAllowMultiSelect(true);
            builder.setMultipleColumnThreshold(MULTI_COLUMN_THRESHOLD);

            for (T value : currentValues) {
              Message svalue = getPropertyValues(d, Collections
                  .singleton(value));
              builder.addNumberedOption(svalue, MenuResult.success(value));
            }

            builder.addHelpOption(new PropertyHelpCallback(mo
                .getManagedObjectDefinition(), d));

            builder.addCancelOption(true);
            builder.addQuitOption();

            app.println();
            app.println();
            Menu<T> menu = builder.toMenu();
            MenuResult<T> result = menu.run();

            if (result.isSuccess()) {
              // Set the new property value(s).
              currentValues.removeAll(result.getValues());
              app.println();
              app.pressReturnToContinue();
              return MenuResult.success(false);
            } else if (result.isCancel()) {
              app.println();
              app.pressReturnToContinue();
              return MenuResult.success(false);
            } else {
              return MenuResult.quit();
            }
          }

        };

        MenuResult<Boolean> result = runMenu(d, app, defaultValues, oldValues,
            currentValues, addCallback, removeCallback);
        if (!result.isAgain()) {
          return result;
        }
      }
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public <T> MenuResult<Boolean> visitUnknown(final PropertyDefinition<T> d,
        ConsoleApplication app) {
      PropertyDefinitionUsageBuilder b = new PropertyDefinitionUsageBuilder(
          true);
      app.println();
      app.println(INFO_EDITOR_HEADING_SYNTAX.get(b.getUsage(d)), 4);

      final SortedSet<T> defaultValues = mo.getPropertyDefaultValues(d);
      final SortedSet<T> oldValues = mo.getPropertyValues(d);
      final SortedSet<T> currentValues = mo.getPropertyValues(d);

      boolean isFirst = true;
      while (true) {
        if (!isFirst) {
          app.println();
          app.println(INFO_EDITOR_HEADING_CONFIGURE_PROPERTY_CONT.get(d
              .getName()));
        } else {
          isFirst = false;
        }

        if (currentValues.size() > 1) {
          app.println();
          app.println(INFO_EDITOR_HEADING_VALUES_SUMMARY.get(d.getName()));
          app.println();
          displayPropertyValues(app, d, currentValues);
        }

        // Create the add values call-back.
        MenuCallback<Boolean> addCallback = new MenuCallback<Boolean>() {

          public MenuResult<Boolean> invoke(ConsoleApplication app)
              throws CLIException {
            app.println();
            readPropertyValues(app, mo.getManagedObjectDefinition(), d,
                currentValues);
            return MenuResult.success(false);
          }

        };

        // Create the remove values call-back.
        MenuCallback<Boolean> removeCallback = new MenuCallback<Boolean>() {

          public MenuResult<Boolean> invoke(ConsoleApplication app)
              throws CLIException {
            MenuBuilder<T> builder = new MenuBuilder<T>(app);

            builder.setPrompt(INFO_EDITOR_PROMPT_SELECT_VALUES_REMOVE.get());
            builder.setAllowMultiSelect(true);
            builder.setMultipleColumnThreshold(MULTI_COLUMN_THRESHOLD);

            for (T value : currentValues) {
              Message svalue = getPropertyValues(d, Collections
                  .singleton(value));
              builder.addNumberedOption(svalue, MenuResult.success(value));
            }

            builder.addHelpOption(new PropertyHelpCallback(mo
                .getManagedObjectDefinition(), d));

            builder.addCancelOption(true);
            builder.addQuitOption();

            app.println();
            app.println();
            Menu<T> menu = builder.toMenu();
            MenuResult<T> result = menu.run();

            if (result.isSuccess()) {
              // Set the new property value(s).
              currentValues.removeAll(result.getValues());
              app.println();
              app.pressReturnToContinue();
              return MenuResult.success(false);
            } else if (result.isCancel()) {
              app.println();
              app.pressReturnToContinue();
              return MenuResult.success(false);
            } else {
              return MenuResult.quit();
            }
          }

        };

        MenuResult<Boolean> result = runMenu(d, app, defaultValues, oldValues,
            currentValues, addCallback, removeCallback);
        if (!result.isAgain()) {
          return result;
        }
      }
    }



    /**
     * Generate an appropriate menu option for a property which asks
     * the user whether or not they want to keep the property's
     * current settings.
     */
    private <T> Message getKeepDefaultValuesMenuOption(
        PropertyDefinition<T> pd, SortedSet<T> defaultValues,
        SortedSet<T> oldValues, SortedSet<T> currentValues) {
      DefaultBehaviorQuery<T> query = DefaultBehaviorQuery.query(pd);

      boolean isModified = !currentValues.equals(oldValues);
      boolean isDefault = currentValues.equals(defaultValues);

      if (isModified) {
        switch (currentValues.size()) {
        case 0:
          if (query.isAlias()) {
            return INFO_EDITOR_OPTION_USE_DEFAULT_ALIAS.get(query
                .getAliasDescription());
          } else if (query.isInherited()) {
            if (query.getAliasDescription() != null) {
              return INFO_EDITOR_OPTION_USE_DEFAULT_INHERITED_ALIAS.get(query
                  .getAliasDescription());
            } else {
              return INFO_EDITOR_OPTION_USE_DEFAULT_INHERITED_ALIAS_UNDEFINED
                  .get();
            }
          } else {
            return INFO_EDITOR_OPTION_LEAVE_UNDEFINED.get();
          }
        case 1:
          Message svalue = getPropertyValues(pd, currentValues);
          if (isDefault) {
            if (query.isInherited()) {
              return INFO_EDITOR_OPTION_USE_INHERITED_DEFAULT_VALUE.get(svalue);
            } else {
              return INFO_EDITOR_OPTION_USE_DEFAULT_VALUE.get(svalue);
            }
          } else {
            return INFO_EDITOR_OPTION_USE_VALUE.get(svalue);
          }
        default:
          if (isDefault) {
            if (query.isInherited()) {
              return INFO_EDITOR_OPTION_USE_INHERITED_DEFAULT_VALUES.get();
            } else {
              return INFO_EDITOR_OPTION_USE_DEFAULT_VALUES.get();
            }
          } else {
            return INFO_EDITOR_OPTION_USE_VALUES.get();
          }
        }
      } else {
        switch (currentValues.size()) {
        case 0:
          if (query.isAlias()) {
            return INFO_EDITOR_OPTION_KEEP_DEFAULT_ALIAS.get(query
                .getAliasDescription());
          } else if (query.isInherited()) {
            if (query.getAliasDescription() != null) {
              return INFO_EDITOR_OPTION_KEEP_DEFAULT_INHERITED_ALIAS.get(query
                  .getAliasDescription());
            } else {
              return INFO_EDITOR_OPTION_KEEP_DEFAULT_INHERITED_ALIAS_UNDEFINED
                  .get();
            }
          } else {
            return INFO_EDITOR_OPTION_LEAVE_UNDEFINED.get();
          }
        case 1:
          Message svalue = getPropertyValues(pd, currentValues);
          if (isDefault) {
            if (query.isInherited()) {
              return INFO_EDITOR_OPTION_KEEP_INHERITED_DEFAULT_VALUE
                  .get(svalue);
            } else {
              return INFO_EDITOR_OPTION_KEEP_DEFAULT_VALUE.get(svalue);
            }
          } else {
            return INFO_EDITOR_OPTION_KEEP_VALUE.get(svalue);
          }
        default:
          if (isDefault) {
            if (query.isInherited()) {
              return INFO_EDITOR_OPTION_KEEP_INHERITED_DEFAULT_VALUES.get();
            } else {
              return INFO_EDITOR_OPTION_KEEP_DEFAULT_VALUES.get();
            }
          } else {
            return INFO_EDITOR_OPTION_KEEP_VALUES.get();
          }
        }
      }
    }



    /**
     * Generate an appropriate menu option which should be used in the
     * case where a property can be reset to its default behavior.
     */
    private <T> Message getResetToDefaultValuesMenuOption(
        PropertyDefinition<T> pd, SortedSet<T> defaultValues,
        SortedSet<T> currentValues) {
      DefaultBehaviorQuery<T> query = DefaultBehaviorQuery.query(pd);
      boolean isMandatory = pd.hasOption(PropertyOption.MANDATORY);

      if (!isMandatory && query.isAlias()) {
        return INFO_EDITOR_OPTION_RESET_DEFAULT_ALIAS.get(query
            .getAliasDescription());
      } else if (query.isDefined()) {
        // Only show this option if the current value is different
        // to the default.
        if (!currentValues.equals(defaultValues)) {
          Message svalue = getPropertyValues(pd, defaultValues);
          if (defaultValues.size() > 1) {
            return INFO_EDITOR_OPTION_RESET_DEFAULT_VALUES.get(svalue);
          } else {
            return INFO_EDITOR_OPTION_RESET_DEFAULT_VALUE.get(svalue);
          }
        } else {
          return null;
        }
      } else if (!isMandatory && query.isInherited()) {
        if (defaultValues.isEmpty()) {
          if (query.getAliasDescription() != null) {
            return INFO_EDITOR_OPTION_RESET_DEFAULT_INHERITED_ALIAS.get(query
                .getAliasDescription());
          } else {
            return INFO_EDITOR_OPTION_RESET_DEFAULT_INHERITED_ALIAS_UNDEFINED
                .get();
          }
        } else {
          Message svalue = getPropertyValues(pd, defaultValues);
          if (defaultValues.size() > 1) {
            return INFO_EDITOR_OPTION_RESET_INHERITED_DEFAULT_VALUES
                .get(svalue);
          } else {
            return INFO_EDITOR_OPTION_RESET_INHERITED_DEFAULT_VALUE.get(svalue);
          }
        }
      } else if (!isMandatory && query.isUndefined()) {
        return INFO_EDITOR_OPTION_LEAVE_UNDEFINED.get();
      } else {
        return null;
      }
    }



    // Common menu processing.
    private <T> MenuResult<Boolean> runMenu(final PropertyDefinition<T> d,
        ConsoleApplication app, final SortedSet<T> defaultValues,
        final SortedSet<T> oldValues, final SortedSet<T> currentValues,
        MenuCallback<Boolean> addCallback,
        MenuCallback<Boolean> removeCallback) {
      // Construct a menu of actions.
      MenuBuilder<Boolean> builder = new MenuBuilder<Boolean>(app);
      builder.setPrompt(INFO_EDITOR_PROMPT_MODIFY_MENU.get(d.getName()));

      // First option is for leaving the property unchanged or
      // applying changes, but only if the state of the property is
      // valid.
      if (!(d.hasOption(PropertyOption.MANDATORY) && currentValues.isEmpty())) {
        MenuResult<Boolean> result;
        if (!oldValues.equals(currentValues)) {
          result = MenuResult.success(true);
        } else {
          result = MenuResult.<Boolean> cancel();
        }

        Message option = getKeepDefaultValuesMenuOption(d, defaultValues,
            oldValues, currentValues);
        builder.addNumberedOption(option, result);
        builder.setDefault(Message.raw("1"), result);
      }

      // Add an option for adding some values.
      if (addCallback != null) {
        int i = builder.addNumberedOption(
            INFO_EDITOR_OPTION_ADD_ONE_OR_MORE_VALUES.get(), addCallback);
        if (d.hasOption(PropertyOption.MANDATORY) && currentValues.isEmpty()) {
          builder.setDefault(Message.raw("%d", i), addCallback);
        }
      }

      // Add options for removing values if applicable.
      if (!currentValues.isEmpty()) {
        builder.addNumberedOption(INFO_EDITOR_OPTION_REMOVE_ONE_OR_MORE_VALUES
            .get(), removeCallback);
      }

      // Add options for removing all values and for resetting the
      // property to its default behavior.
      Message resetOption = null;
      if (!currentValues.equals(defaultValues)) {
        resetOption = getResetToDefaultValuesMenuOption(d, defaultValues,
            currentValues);
      }

      if (!currentValues.isEmpty()) {
        if (resetOption == null || !defaultValues.isEmpty()) {
          MenuCallback<Boolean> callback = new MenuCallback<Boolean>() {

            public MenuResult<Boolean> invoke(ConsoleApplication app)
                throws CLIException {
              currentValues.clear();
              app.println();
              app.pressReturnToContinue();
              return MenuResult.success(false);
            }

          };

          builder.addNumberedOption(INFO_EDITOR_OPTION_REMOVE_ALL_VALUES.get(),
              callback);
        }
      }

      if (resetOption != null) {
        MenuCallback<Boolean> callback = new MenuCallback<Boolean>() {

          public MenuResult<Boolean> invoke(ConsoleApplication app)
              throws CLIException {
            currentValues.clear();
            currentValues.addAll(defaultValues);
            app.println();
            app.pressReturnToContinue();
            return MenuResult.success(false);
          }

        };

        builder.addNumberedOption(resetOption, callback);
      }

      // Add an option for undoing any changes.
      if (!oldValues.equals(currentValues)) {
        MenuCallback<Boolean> callback = new MenuCallback<Boolean>() {

          public MenuResult<Boolean> invoke(ConsoleApplication app)
              throws CLIException {
            currentValues.clear();
            currentValues.addAll(oldValues);
            app.println();
            app.pressReturnToContinue();
            return MenuResult.success(false);
          }

        };

        builder.addNumberedOption(INFO_EDITOR_OPTION_REVERT_CHANGES.get(),
            callback);
      }

      builder.addHelpOption(new PropertyHelpCallback(mo
          .getManagedObjectDefinition(), d));
      builder.addQuitOption();

      Menu<Boolean> menu = builder.toMenu();
      MenuResult<Boolean> result;
      try {
        app.println();
        result = menu.run();
      } catch (CLIException e) {
        this.e = e;
        return null;
      }

      if (result.isSuccess()) {
        if (result.getValue() == true) {
          // Set the new property value(s).
          mo.setPropertyValues(d, currentValues);
          app.println();
          app.pressReturnToContinue();
          return MenuResult.success(false);
        } else {
          // Continue until cancel/apply changes.
          app.println();
          return MenuResult.again();
        }
      } else if (result.isCancel()) {
        app.println();
        app.pressReturnToContinue();
        return MenuResult.success(false);
      } else {
        return MenuResult.quit();
      }
    }
  }



  /**
   * A help call-back which displays a description and summary of a
   * single property.
   */
  private static final class PropertyHelpCallback implements HelpCallback {

    // The managed object definition.
    private final ManagedObjectDefinition<?, ?> d;

    // The property to be edited.
    private final PropertyDefinition<?> pd;



    // Creates a new property helper for the specified property.
    private PropertyHelpCallback(ManagedObjectDefinition<?, ?> d,
        PropertyDefinition<?> pd) {
      this.d = d;
      this.pd = pd;
    }



    /**
     * {@inheritDoc}
     */
    public void display(ConsoleApplication app) {
      app.println();
      HelpSubCommandHandler.displayVerboseSingleProperty(app, d, pd.getName());
      app.println();
      app.pressReturnToContinue();
    }
  }



  /**
   * A menu call-back for viewing a read-only properties.
   */
  private static final class ReadOnlyPropertyViewer extends
      PropertyDefinitionVisitor<MenuResult<Boolean>, ConsoleApplication>
      implements MenuCallback<Boolean> {

    // Any exception that was caught during processing.
    private CLIException e = null;

    // The managed object being edited.
    private final ManagedObject<?> mo;

    // The property to be edited.
    private final PropertyDefinition<?> pd;



    // Creates a new property editor for the specified property.
    private ReadOnlyPropertyViewer(ManagedObject<?> mo,
        PropertyDefinition<?> pd) {
      Validator.ensureTrue(!pd.hasOption(PropertyOption.MULTI_VALUED));

      this.mo = mo;
      this.pd = pd;
    }



    /**
     * {@inheritDoc}
     */
    public MenuResult<Boolean> invoke(ConsoleApplication app)
        throws CLIException {
      MenuResult<Boolean> result = pd.accept(this, app);
      if (e != null) {
        throw e;
      } else {
        return result;
      }
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public <T> MenuResult<Boolean> visitUnknown(PropertyDefinition<T> pd,
        ConsoleApplication app) {
      SortedSet<T> values = mo.getPropertyValues(pd);

      app.println();
      app.println();
      switch (values.size()) {
      case 0:
        // Only alias, undefined, or inherited alias or undefined
        // properties should apply here.
        DefaultBehaviorQuery<T> query = DefaultBehaviorQuery.query(pd);
        Message aliasDescription = query.getAliasDescription();
        if (aliasDescription == null) {
          app.println(INFO_EDITOR_HEADING_READ_ONLY_ALIAS_UNDEFINED.get(pd
              .getName()));
        } else {
          app.println(INFO_EDITOR_HEADING_READ_ONLY_ALIAS.get(pd.getName(),
              aliasDescription));
        }
        break;
      case 1:
        Message svalue = getPropertyValues(pd, mo);
        app.println(INFO_EDITOR_HEADING_READ_ONLY_VALUE.get(pd.getName(),
            svalue));
        break;
      default:
        app.println(INFO_EDITOR_HEADING_READ_ONLY_VALUES.get(pd.getName()));
        app.println();
        displayPropertyValues(app, pd, values);
        break;
      }

      app.println();
      boolean result;
      try {
        result = app.confirmAction(INFO_EDITOR_PROMPT_READ_ONLY.get(), false);
      } catch (CLIException e) {
        this.e = e;
        return null;
      }

      if (result) {
        app.println();
        HelpSubCommandHandler.displayVerboseSingleProperty(app, mo
            .getManagedObjectDefinition(), pd.getName());
        app.println();
        app.pressReturnToContinue();
      }

      return MenuResult.again();
    }
  }



  /**
   * A menu call-back for editing a modifiable single-valued property.
   */
  private static final class SingleValuedPropertyEditor extends
      PropertyDefinitionVisitor<MenuResult<Boolean>, ConsoleApplication>
      implements MenuCallback<Boolean> {

    // Any exception that was caught during processing.
    private CLIException e = null;

    // The managed object being edited.
    private final ManagedObject<?> mo;

    // The property to be edited.
    private final PropertyDefinition<?> pd;



    // Creates a new property editor for the specified property.
    private SingleValuedPropertyEditor(ManagedObject<?> mo,
        PropertyDefinition<?> pd) {
      Validator.ensureTrue(!pd.hasOption(PropertyOption.MULTI_VALUED));

      this.mo = mo;
      this.pd = pd;
    }



    /**
     * {@inheritDoc}
     */
    public MenuResult<Boolean> invoke(ConsoleApplication app)
        throws CLIException {
      displayPropertyHeader(app, pd);

      MenuResult<Boolean> result = pd.accept(this, app);
      if (e != null) {
        throw e;
      } else {
        return result;
      }
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public MenuResult<Boolean> visitBoolean(BooleanPropertyDefinition d,
        ConsoleApplication app) {
      // Construct a menu of actions.
      MenuBuilder<Boolean> builder = new MenuBuilder<Boolean>(app);
      builder.setPrompt(INFO_EDITOR_PROMPT_MODIFY_MENU.get(d.getName()));

      DefaultBehaviorQuery<Boolean> query = DefaultBehaviorQuery.query(d);
      SortedSet<Boolean> currentValues = mo.getPropertyValues(d);
      SortedSet<Boolean> defaultValues = mo.getPropertyDefaultValues(d);

      Boolean currentValue = currentValues.isEmpty() ? null : currentValues
          .first();
      Boolean defaultValue = defaultValues.isEmpty() ? null : defaultValues
          .first();

      // First option is for leaving the property unchanged.
      Message option = getKeepDefaultValuesMenuOption(d);
      builder.addNumberedOption(option, MenuResult.<Boolean> cancel());
      builder.setDefault(Message.raw("1"), MenuResult.<Boolean> cancel());

      // The second (and possibly third) option is to always change
      // the property's value.
      if (currentValue == null || currentValue == false) {
        Message svalue = getPropertyValues(d, Collections.singleton(true));

        if (defaultValue != null && defaultValue == true) {
          option = INFO_EDITOR_OPTION_CHANGE_TO_DEFAULT_VALUE.get(svalue);
        } else {
          option = INFO_EDITOR_OPTION_CHANGE_TO_VALUE.get(svalue);
        }

        builder.addNumberedOption(option, MenuResult.success(true));
      }

      if (currentValue == null || currentValue == true) {
        Message svalue = getPropertyValues(d, Collections.singleton(false));

        if (defaultValue != null && defaultValue == false) {
          option = INFO_EDITOR_OPTION_CHANGE_TO_DEFAULT_VALUE.get(svalue);
        } else {
          option = INFO_EDITOR_OPTION_CHANGE_TO_VALUE.get(svalue);
        }

        builder.addNumberedOption(option, MenuResult.success(false));
      }

      // Final option is to reset the value back to its default.
      if (mo.isPropertyPresent(d) && !query.isDefined()) {
        option = getResetToDefaultValuesMenuOption(d);
        if (option != null) {
          builder.addNumberedOption(option, MenuResult.<Boolean> success());
        }
      }

      return runMenu(app, d, builder);
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends Enum<E>> MenuResult<Boolean> visitEnum(
        EnumPropertyDefinition<E> d, ConsoleApplication app) {
      // Construct a menu of actions.
      MenuBuilder<E> builder = new MenuBuilder<E>(app);
      builder.setMultipleColumnThreshold(MULTI_COLUMN_THRESHOLD);
      builder.setPrompt(INFO_EDITOR_PROMPT_MODIFY_MENU.get(d.getName()));

      DefaultBehaviorQuery<E> query = DefaultBehaviorQuery.query(d);
      SortedSet<E> currentValues = mo.getPropertyValues(d);
      SortedSet<E> defaultValues = mo.getPropertyDefaultValues(d);
      E currentValue = currentValues.isEmpty() ? null : currentValues.first();
      E defaultValue = defaultValues.isEmpty() ? null : defaultValues.first();

      // First option is for leaving the property unchanged.
      Message option = getKeepDefaultValuesMenuOption(d);
      builder.addNumberedOption(option, MenuResult.<E> cancel());
      builder.setDefault(Message.raw("1"), MenuResult.<E> cancel());

      // Create options for changing to other values.
      Set<E> values = new TreeSet<E>(d);
      values.addAll(EnumSet.allOf(d.getEnumClass()));
      for (E value : values) {
        if (value.equals(currentValue) && query.isDefined()) {
          // This option is unnecessary.
          continue;
        }

        Message svalue = getPropertyValues(d, Collections.singleton(value));

        if (value.equals(defaultValue) && query.isDefined()) {
          option = INFO_EDITOR_OPTION_CHANGE_TO_DEFAULT_VALUE.get(svalue);
        } else {
          option = INFO_EDITOR_OPTION_CHANGE_TO_VALUE.get(svalue);
        }

        builder.addNumberedOption(option, MenuResult.success(value));
      }

      // Third option is to reset the value back to its default.
      if (mo.isPropertyPresent(d) && !query.isDefined()) {
        option = getResetToDefaultValuesMenuOption(d);
        if (option != null) {
          builder.addNumberedOption(option, MenuResult.<E> success());
        }
      }

      return runMenu(app, d, builder);
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public <T> MenuResult<Boolean> visitUnknown(final PropertyDefinition<T> d,
        ConsoleApplication app) {
      PropertyDefinitionUsageBuilder b = new PropertyDefinitionUsageBuilder(
          true);
      app.println();
      app.println(INFO_EDITOR_HEADING_SYNTAX.get(b.getUsage(d)), 4);

      // Construct a menu of actions.
      MenuBuilder<T> builder = new MenuBuilder<T>(app);
      builder.setPrompt(INFO_EDITOR_PROMPT_MODIFY_MENU.get(d.getName()));

      // First option is for leaving the property unchanged.
      Message option = getKeepDefaultValuesMenuOption(d);
      builder.addNumberedOption(option, MenuResult.<T> cancel());
      builder.setDefault(Message.raw("1"), MenuResult.<T> cancel());

      // The second option is to always change the property's value.
      builder.addNumberedOption(INFO_EDITOR_OPTION_CHANGE_VALUE.get(),
          new MenuCallback<T>() {

            public MenuResult<T> invoke(ConsoleApplication app)
                throws CLIException {
              app.println();
              Set<T> values = readPropertyValues(app, mo
                  .getManagedObjectDefinition(), d);
              return MenuResult.success(values);
            }

          });

      // Third option is to reset the value back to its default.
      if (mo.isPropertyPresent(d)) {
        option = getResetToDefaultValuesMenuOption(d);
        if (option != null) {
          builder.addNumberedOption(option, MenuResult.<T> success());
        }
      }

      return runMenu(app, d, builder);
    }



    /**
     * Generate an appropriate menu option for a property which asks
     * the user whether or not they want to keep the property's
     * current settings.
     */
    private <T> Message getKeepDefaultValuesMenuOption(
        PropertyDefinition<T> pd) {
      DefaultBehaviorQuery<T> query = DefaultBehaviorQuery.query(pd);
      SortedSet<T> currentValues = mo.getPropertyValues(pd);
      SortedSet<T> defaultValues = mo.getPropertyDefaultValues(pd);

      if (query.isDefined() && currentValues.equals(defaultValues)) {
        Message svalue = getPropertyValues(pd, currentValues);
        return INFO_EDITOR_OPTION_KEEP_DEFAULT_VALUE.get(svalue);
      } else if (mo.isPropertyPresent(pd)) {
        Message svalue = getPropertyValues(pd, currentValues);
        return INFO_EDITOR_OPTION_KEEP_VALUE.get(svalue);
      } else if (query.isAlias()) {
        return INFO_EDITOR_OPTION_KEEP_DEFAULT_ALIAS.get(query
            .getAliasDescription());
      } else if (query.isInherited()) {
        if (defaultValues.isEmpty()) {
          if (query.getAliasDescription() != null) {
            return INFO_EDITOR_OPTION_KEEP_DEFAULT_INHERITED_ALIAS.get(query
                .getAliasDescription());
          } else {
            return INFO_EDITOR_OPTION_KEEP_DEFAULT_INHERITED_ALIAS_UNDEFINED
                .get();
          }
        } else {
          Message svalue = getPropertyValues(pd, defaultValues);
          return INFO_EDITOR_OPTION_KEEP_INHERITED_DEFAULT_VALUE.get(svalue);
        }
      } else {
        return INFO_EDITOR_OPTION_LEAVE_UNDEFINED.get();
      }
    }



    /**
     * Generate an appropriate menu option which should be used in the
     * case where a property can be reset to its default behavior.
     */
    private <T> Message getResetToDefaultValuesMenuOption(
        PropertyDefinition<T> pd) {
      DefaultBehaviorQuery<T> query = DefaultBehaviorQuery.query(pd);
      SortedSet<T> currentValues = mo.getPropertyValues(pd);
      SortedSet<T> defaultValues = mo.getPropertyDefaultValues(pd);

      boolean isMandatory = pd.hasOption(PropertyOption.MANDATORY);

      if (!isMandatory && query.isAlias()) {
        return INFO_EDITOR_OPTION_RESET_DEFAULT_ALIAS.get(query
            .getAliasDescription());
      } else if (query.isDefined()) {
        // Only show this option if the current value is different
        // to the default.
        if (!currentValues.equals(defaultValues)) {
          Message svalue = getPropertyValues(pd, defaultValues);
          return INFO_EDITOR_OPTION_RESET_DEFAULT_VALUE.get(svalue);
        } else {
          return null;
        }
      } else if (!isMandatory && query.isInherited()) {
        if (defaultValues.isEmpty()) {
          if (query.getAliasDescription() != null) {
            return INFO_EDITOR_OPTION_RESET_DEFAULT_INHERITED_ALIAS.get(query
                .getAliasDescription());
          } else {
            return INFO_EDITOR_OPTION_RESET_DEFAULT_INHERITED_ALIAS_UNDEFINED
                .get();
          }
        } else {
          Message svalue = getPropertyValues(pd, defaultValues);
          return INFO_EDITOR_OPTION_RESET_INHERITED_DEFAULT_VALUE.get(svalue);
        }
      } else if (!isMandatory && query.isUndefined()) {
        return INFO_EDITOR_OPTION_LEAVE_UNDEFINED.get();
      } else {
        return null;
      }
    }



    // Common menu processing.
    private <T> MenuResult<Boolean> runMenu(ConsoleApplication app,
        final PropertyDefinition<T> d, MenuBuilder<T> builder)
        throws IllegalPropertyValueException, PropertyIsSingleValuedException,
        PropertyIsReadOnlyException, PropertyIsMandatoryException,
        IllegalArgumentException {
      builder.addHelpOption(new PropertyHelpCallback(mo
          .getManagedObjectDefinition(), d));
      builder.addQuitOption();

      Menu<T> menu = builder.toMenu();
      MenuResult<T> result;
      try {
        app.println();
        result = menu.run();
      } catch (CLIException e) {
        this.e = e;
        return null;
      }

      if (result.isSuccess()) {
        // Set the new property value(s).
        mo.setPropertyValues(d, result.getValues());
        app.println();
        app.pressReturnToContinue();
        return MenuResult.success(false);
      } else if (result.isCancel()) {
        app.println();
        app.pressReturnToContinue();
        return MenuResult.success(false);
      } else {
        return MenuResult.quit();
      }
    }
  }



  // Display a title and a description of the property.
  private static void displayPropertyHeader(ConsoleApplication app,
      PropertyDefinition<?> pd) {
    app.println();
    app.println();
    app.println(INFO_EDITOR_HEADING_CONFIGURE_PROPERTY.get(pd.getName()));
    app.println();
    app.println(pd.getSynopsis(), 4);
    if (pd.getDescription() != null) {
      app.println();
      app.println(pd.getDescription(), 4);
    }
  }



  // Display a table of property values.
  private static <T> void displayPropertyValues(ConsoleApplication app,
      PropertyDefinition<T> pd, Collection<T> values)
      throws IllegalArgumentException {
    TableBuilder builder = new TableBuilder();
    PropertyValuePrinter valuePrinter = new PropertyValuePrinter(null,
        null, false);

    int sz = values.size();
    boolean useMultipleColumns = (sz >= MULTI_COLUMN_THRESHOLD);
    int rows = sz;
    if (useMultipleColumns) {
      // Display in two columns the first column should contain
      // half the values. If there are an odd number of columns
      // then the first column should contain an additional value
      // (e.g. if there are 23 values, the first column should
      // contain 12 values and the second column 11 values).
      rows /= 2;
      rows += sz % 2;
    }

    List<T> vl = new ArrayList<T>(values);
    for (int i = 0, j = rows; i < rows; i++, j++) {
      builder.startRow();
      builder.appendCell();
      builder.appendCell(INFO_EDITOR_OPTION_VALUES.get(i + 1));
      builder.appendCell(valuePrinter.print(pd, vl.get(i)));

      if (useMultipleColumns && (j < sz)) {
        builder.appendCell();
        builder.appendCell(INFO_EDITOR_OPTION_VALUES.get(j + 1));
        builder.appendCell(valuePrinter.print(pd, vl.get(j)));
      }
    }

    TextTablePrinter printer = new TextTablePrinter(app.getErrorStream());
    printer.setDisplayHeadings(false);
    printer.setColumnWidth(0, 2);
    printer.setColumnWidth(2, 0);
    if (useMultipleColumns) {
      printer.setColumnWidth(3, 2);
      printer.setColumnWidth(5, 0);
    }
    builder.print(printer);
  }



  // Display the set of values associated with a property.
  private static <T> Message getPropertyValues(PropertyDefinition<T> pd,
      Collection<T> values) {
    if (values.isEmpty()) {
      // There are no values or default values. Display the default
      // behavior for alias values.
      DefaultBehaviorQuery<T> query = DefaultBehaviorQuery.query(pd);
      Message content = query.getAliasDescription();
      if (content == null) {
        return Message.raw("-");
      } else {
        return content;
      }
    } else {
      PropertyValuePrinter printer =
        new PropertyValuePrinter(null, null, false);
      MessageBuilder builder = new MessageBuilder();

      boolean isFirst = true;
      for (T value : values) {
        if (!isFirst) {
          builder.append(", ");
        }
        builder.append(printer.print(pd, value));
        isFirst = false;
      }

      return builder.toMessage();
    }
  }



  // Display the set of values associated with a property.
  private static <T> Message getPropertyValues(
      PropertyDefinition<T> pd,
      ManagedObject<?> mo) {
    SortedSet<T> values = mo.getPropertyValues(pd);
    return getPropertyValues(pd, values);
  }



  // Read new values for a property.
  private static <T> SortedSet<T> readPropertyValues(ConsoleApplication app,
      ManagedObjectDefinition<?, ?> d, PropertyDefinition<T> pd)
      throws CLIException {
    SortedSet<T> values = new TreeSet<T>(pd);
    readPropertyValues(app, d, pd, values);
    return values;
  }



  // Add values to a property.
  private static <T> void readPropertyValues(ConsoleApplication app,
      ManagedObjectDefinition<?, ?> d, PropertyDefinition<T> pd,
      SortedSet<T> values) throws CLIException {
    // Make sure there is at least one value if mandatory and empty.
    if (values.isEmpty()) {
      while (true) {
        try {
          Message prompt;

          if (pd.hasOption(PropertyOption.MANDATORY)) {
            prompt = INFO_EDITOR_PROMPT_READ_FIRST_VALUE.get(pd.getName());
          } else {
            prompt = INFO_EDITOR_PROMPT_READ_FIRST_VALUE_OPTIONAL.get(pd
                .getName());
          }

          app.println();
          String s = app.readLineOfInput(prompt);
          if (s.trim().length() == 0) {
            if (!pd.hasOption(PropertyOption.MANDATORY)) {
              return;
            }
          }

          T value = pd.decodeValue(s);
          if (values.contains(value)) {
            // Prevent addition of duplicates.
            app.println();
            app.println(ERR_EDITOR_READ_FIRST_DUPLICATE.get(s));
          } else {
            values.add(value);
          }

          break;
        } catch (IllegalPropertyValueStringException e) {
          app.println();
          app.println(ArgumentExceptionFactory.adaptPropertyException(e, d)
              .getMessageObject());
        }
      }
    }

    if (pd.hasOption(PropertyOption.MULTI_VALUED)) {
      // Prompt for more values if multi-valued.
      while (true) {
        try {
          Message prompt = INFO_EDITOR_PROMPT_READ_NEXT_VALUE.get(pd.getName());

          app.println();
          String s = app.readLineOfInput(prompt);
          if (s.trim().length() == 0) {
            return;
          }

          T value = pd.decodeValue(s);
          if (values.contains(value)) {
            // Prevent addition of duplicates.
            app.println();
            app.println(ERR_EDITOR_READ_NEXT_DUPLICATE.get(s));
          } else {
            values.add(value);
          }
        } catch (IllegalPropertyValueStringException e) {
          app.println();
          app.println(ArgumentExceptionFactory.adaptPropertyException(e, d)
              .getMessageObject());
          app.println();
        }
      }
    }
  }

  // The threshold above which choice menus should be displayed in
  // multiple columns.
  private static final int MULTI_COLUMN_THRESHOLD = 8;

  // The application console.
  private final ConsoleApplication app;



  /**
   * Create a new property value editor which will read from the
   * provided application console.
   *
   * @param app
   *          The application console.
   */
  public PropertyValueEditor(ConsoleApplication app) {
    this.app = app;
  }



  /**
   * Interactively edits the properties of a managed object. Only the
   * properties listed in the provided collection will be accessible
   * to the client. It is up to the caller to ensure that the list of
   * properties does not include read-only, monitoring, hidden, or
   * advanced properties as appropriate.
   *
   * @param mo
   *          The managed object.
   * @param c
   *          The collection of properties which can be edited.
   * @param isCreate
   *          Flag indicating whether or not the managed object is
   *          being created. If it is then read-only properties will
   *          be modifiable.
   * @return Returns {@link MenuResult#success()} if the changes made
   *         to the managed object should be applied, or
   *         {@link MenuResult#cancel()} if the user to chose to
   *         cancel any changes, or {@link MenuResult#quit()} if the
   *         user chose to quit the application.
   * @throws CLIException
   *           If the user input could not be retrieved for some
   *           reason.
   */
  public MenuResult<Void> edit(ManagedObject<?> mo,
      Collection<PropertyDefinition<?>> c, boolean isCreate)
      throws CLIException {
    // Get values for this missing mandatory property.
    MandatoryPropertyInitializer mpi = new MandatoryPropertyInitializer();
    for (PropertyDefinition<?> pd : c) {
      if (pd.hasOption(PropertyOption.MANDATORY)) {
        if (mo.getPropertyValues(pd).isEmpty()) {
          MenuResult<Void> result = mpi.read(mo, pd);
          if (!result.isSuccess()) {
            return result;
          }
        }
      }
    }

    while (true) {
      // Construct the main menu.
      MenuBuilder<Boolean> builder = new MenuBuilder<Boolean>(app);

      Message ufn = mo.getManagedObjectDefinition().getUserFriendlyName();
      builder.setPrompt(INFO_EDITOR_HEADING_CONFIGURE_COMPONENT.get(ufn));

      Message heading1 = INFO_DSCFG_HEADING_PROPERTY_NAME.get();
      Message heading2 = INFO_DSCFG_HEADING_PROPERTY_VALUE.get();
      builder.setColumnHeadings(heading1, heading2);
      builder.setColumnWidths(null, 0);

      // Create an option for editing/viewing each property.
      for (PropertyDefinition<?> pd : c) {
        // Determine whether this property should be modifiable.
        boolean isReadOnly = false;

        if (pd.hasOption(PropertyOption.MONITORING)) {
          isReadOnly = true;
        }

        if (!isCreate && pd.hasOption(PropertyOption.READ_ONLY)) {
          isReadOnly = true;
        }

        // Create the appropriate property action.
        MenuCallback<Boolean> callback;
        if (pd.hasOption(PropertyOption.MULTI_VALUED)) {
          if (isReadOnly) {
            callback = new ReadOnlyPropertyViewer(mo, pd);
          } else {
            callback = new MultiValuedPropertyEditor(mo, pd);
          }
        } else {
          if (isReadOnly) {
            callback = new ReadOnlyPropertyViewer(mo, pd);
          } else {
            callback = new SingleValuedPropertyEditor(mo, pd);
          }
        }

        // Create the numeric option.
        Message values = getPropertyValues(pd, mo);
        builder.addNumberedOption(Message.raw("%s", pd.getName()), callback,
            values);
      }

      // Add a help option which displays a summary of the managed
      // object's definition.
      HelpCallback helpCallback = new ComponentHelpCallback(mo, c);
      builder.addHelpOption(helpCallback);

      // Add an option to apply the changes.
      if (isCreate) {
        builder.addCharOption(INFO_EDITOR_OPTION_FINISH_KEY.get(),
            INFO_EDITOR_OPTION_FINISH_CREATE_COMPONENT.get(ufn), MenuResult
                .success(true));
      } else {
        builder.addCharOption(INFO_EDITOR_OPTION_FINISH_KEY.get(),
            INFO_EDITOR_OPTION_FINISH_MODIFY_COMPONENT.get(ufn), MenuResult
                .success(true));
      }

      builder.setDefault(INFO_EDITOR_OPTION_FINISH_KEY.get(), MenuResult
          .success(true));

      // Add options for canceling and quitting.
      if (app.isMenuDrivenMode()) {
        builder.addCancelOption(false);
      }
      builder.addQuitOption();

      // Run the menu - success indicates that any changes should be
      // committed.
      app.println();
      app.println();
      Menu<Boolean> menu = builder.toMenu();
      MenuResult<Boolean> result = menu.run();

      if (result.isSuccess()) {
        if (result.getValue()) {
          return MenuResult.<Void>success();
        }
      } else if (result.isCancel()) {
        return MenuResult.cancel();
      } else {
        return MenuResult.quit();
      }
    }
  }
}
