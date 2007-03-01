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
 *      Portions Copyright 2006-2007 Sun Microsystems, Inc.
 */

package org.opends.quicksetup.ui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

import org.opends.quicksetup.installer.DataOptions;
import org.opends.quicksetup.installer.FieldName;
import org.opends.quicksetup.installer.LabelFieldDescriptor;
import org.opends.quicksetup.installer.UserInstallData;

/**
 * This is the panel that contains the Review Panel.
 *
 */
class ReviewPanel extends QuickSetupStepPanel
{
  private static final long serialVersionUID = -7356174829193265699L;

  private boolean displayServerLocation;

  private UserInstallData defaultUserData;

  private JCheckBox checkBox;

  private HashMap<FieldName, JLabel> hmLabels =
      new HashMap<FieldName, JLabel>();

  private HashMap<FieldName, JTextComponent> hmFields =
      new HashMap<FieldName, JTextComponent>();

  /**
   * Constructor of the panel.
   * @param defaultUserData the default values that must be used to initialize
   * the fields of the panel.
   */
  public ReviewPanel(UserInstallData defaultUserData)
  {
    this.defaultUserData = defaultUserData;
    this.displayServerLocation = isWebStart();
    populateLabelAndFieldsMap();
    createLayout();
  }

  /**
   * {@inheritDoc}
   */
  public void beginDisplay(UserInstallData userData)
  {
    if (displayServerLocation)
    {
      setFieldValue(FieldName.SERVER_LOCATION, userData.getServerLocation());
    }
    setFieldValue(FieldName.SERVER_PORT, String.valueOf(userData
        .getServerPort()));
    setFieldValue(FieldName.DIRECTORY_MANAGER_DN, userData
        .getDirectoryManagerDn());
    setFieldValue(FieldName.DIRECTORY_BASE_DN, userData.getDataOptions()
        .getBaseDn());
    setFieldValue(FieldName.DATA_OPTIONS, getDisplayString(userData
        .getDataOptions()));
  }

  /**
   * {@inheritDoc}
   */
  public Object getFieldValue(FieldName fieldName)
  {
    Object value = null;
    if (fieldName == FieldName.SERVER_START)
    {
      value = new Boolean(getCheckBox().isSelected());
    }
    return value;
  }

  /**
   * {@inheritDoc}
   */
  protected Component createInputPanel()
  {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setOpaque(false);

    GridBagConstraints gbc = new GridBagConstraints();

    gbc.insets = UIFactory.getEmptyInsets();
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(createFieldsPanel(), gbc);

    addVerticalGlue(panel);

    gbc.insets.top = UIFactory.TOP_INSET_PRIMARY_FIELD;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    panel.add(getCheckBox(), gbc);

    return panel;
  }

  /**
   * {@inheritDoc}
   */
  protected String getInstructions()
  {
    return getMsg("review-panel-instructions");
  }

  /**
   * {@inheritDoc}
   */
  protected String getTitle()
  {
    return getMsg("review-panel-title");
  }

  /**
   * Create the components and populate the Maps.
   */
  private void populateLabelAndFieldsMap()
  {
    HashMap<FieldName, LabelFieldDescriptor> hm =
        new HashMap<FieldName, LabelFieldDescriptor>();

    if (displayServerLocation)
    {
      hm.put(FieldName.SERVER_LOCATION, new LabelFieldDescriptor(
          getMsg("server-location-label"), getMsg("server-port-tooltip"),
          LabelFieldDescriptor.FieldType.READ_ONLY,
          LabelFieldDescriptor.LabelType.PRIMARY, 0));
    }

    hm.put(FieldName.SERVER_PORT, new LabelFieldDescriptor(
        getMsg("server-port-label"), getMsg("server-port-tooltip"),
        LabelFieldDescriptor.FieldType.READ_ONLY,
        LabelFieldDescriptor.LabelType.PRIMARY, 0));

    hm.put(FieldName.DIRECTORY_MANAGER_DN, new LabelFieldDescriptor(
        getMsg("server-directory-manager-dn-label"),
        getMsg("server-directory-manager-dn-tooltip"),
        LabelFieldDescriptor.FieldType.READ_ONLY,
        LabelFieldDescriptor.LabelType.PRIMARY, 0));

    hm.put(FieldName.DIRECTORY_BASE_DN, new LabelFieldDescriptor(
        getMsg("base-dn-label"), getMsg("base-dn-tooltip"),
        LabelFieldDescriptor.FieldType.READ_ONLY,
        LabelFieldDescriptor.LabelType.PRIMARY, 0));

    hm.put(FieldName.DATA_OPTIONS, new LabelFieldDescriptor(
        getMsg("directory-data-label"), null,
        LabelFieldDescriptor.FieldType.READ_ONLY,
        LabelFieldDescriptor.LabelType.PRIMARY, 0));
    for (FieldName fieldName : hm.keySet())
    {
      LabelFieldDescriptor desc = hm.get(fieldName);

      JTextComponent field = makeJTextComponent(desc, null);
      field.setOpaque(false);
      JLabel label = makeJLabel(desc);

      hmFields.put(fieldName, field);
      label.setLabelFor(field);

      hmLabels.put(fieldName, label);
    }
  }

  /**
   * Returns the label associated with the given field name.
   * @param fieldName the field name for which we want to retrieve the JLabel.
   * @return the label associated with the given field name.
   */
  private JLabel getLabel(FieldName fieldName)
  {
    return hmLabels.get(fieldName);
  }

  /**
   * Returns the JTextComponent associated with the given field name.
   * @param fieldName the field name for which we want to retrieve the
   * JTextComponent.
   * @return the JTextComponent associated with the given field name.
   */
  private JTextComponent getField(FieldName fieldName)
  {
    return hmFields.get(fieldName);
  }

  /**
   * Updates the JTextComponent associated with a FieldName with a text value.
   * @param fieldName the field name of the JTextComponent that we want to
   * update.
   * @param value the value to be set.
   */
  private void setFieldValue(FieldName fieldName, String value)
  {
    getField(fieldName).setText(value);
  }

  /**
   * Returns the localized string describing the DataOptions chosen by the user.
   * @param options the DataOptions of the user.
   * @return the localized string describing the DataOptions chosen by the user.
   */
  private String getDisplayString(DataOptions options)
  {
    String msg;

    switch (options.getType())
    {
    case CREATE_BASE_ENTRY:
      msg = getMsg("review-create-base-entry-label", new String[]
        { options.getBaseDn() });

      break;

    case LEAVE_DATABASE_EMPTY:
      msg = getMsg("review-leave-database-empty-label");
      break;

    case IMPORT_FROM_LDIF_FILE:
      msg = getMsg("review-import-ldif", new String[]
        { options.getLDIFPath() });
      break;

    case IMPORT_AUTOMATICALLY_GENERATED_DATA:
      msg = getMsg("review-import-automatically-generated", new String[]
        { String.valueOf(options.getNumberEntries()) });
      break;

    default:
      throw new IllegalArgumentException("Unknow type: " + options.getType());

    }

    return msg;
  }

  /**
   * Returns the start server check box.
   * If it does not exist creates the start server check box.
   * @return the start server check box.
   */
  private JCheckBox getCheckBox()
  {
    if (checkBox == null)
    {
      checkBox =
          UIFactory.makeJCheckBox(getMsg("start-server-label"),
              getMsg("start-server-tooltip"), UIFactory.TextStyle.CHECKBOX);
      checkBox.setOpaque(false);
      checkBox.setSelected(defaultUserData.getStartServer());
    }
    return checkBox;
  }

  /**
   * Returns and creates the fields panel.
   * @return the fields panel.
   */
  private JPanel createFieldsPanel()
  {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setOpaque(false);
    GridBagConstraints gbc = new GridBagConstraints();

    FieldName[] fieldNames;
    if (displayServerLocation)
    {
      fieldNames =
          new FieldName[]
            { FieldName.SERVER_LOCATION, FieldName.SERVER_PORT,
                FieldName.DIRECTORY_MANAGER_DN,
                FieldName.DIRECTORY_BASE_DN,
                FieldName.DATA_OPTIONS };
    } else
    {
      fieldNames =
          new FieldName[]
            { FieldName.SERVER_PORT, FieldName.DIRECTORY_MANAGER_DN,
                FieldName.DIRECTORY_BASE_DN, FieldName.DATA_OPTIONS };
    }

    for (int i = 0; i < fieldNames.length; i++)
    {
      gbc.gridwidth = GridBagConstraints.RELATIVE;
      gbc.weightx = 0.0;
      if (i > 0)
      {
        gbc.insets.top = UIFactory.TOP_INSET_PRIMARY_FIELD;
      } else
      {
        gbc.insets.top = 0;
      }
      gbc.insets.left = 0;
      gbc.anchor = GridBagConstraints.NORTHWEST;
      panel.add(getLabel(fieldNames[i]), gbc);

      JPanel auxPanel = new JPanel(new GridBagLayout());
      auxPanel.setOpaque(false);
      gbc.weightx = 1.0;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      if (i > 0)
      {
        gbc.insets.top = UIFactory.TOP_INSET_PRIMARY_FIELD;
      } else
      {
        gbc.insets.top = 0;
      }
      gbc.insets.left = UIFactory.LEFT_INSET_PRIMARY_FIELD;

      gbc.gridwidth = GridBagConstraints.REMAINDER;
      panel.add(auxPanel, gbc);

      gbc.insets = UIFactory.getEmptyInsets();
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.weightx = 1.0;
      auxPanel.add(getField(fieldNames[i]), gbc);
    }

    return panel;
  }
}
