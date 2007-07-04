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

package org.opends.quicksetup.installer.ui;

import org.opends.admin.ads.ServerDescriptor;
import org.opends.admin.ads.SuffixDescriptor;
import org.opends.quicksetup.UserData;
import org.opends.quicksetup.installer.DataReplicationOptions;
import org.opends.quicksetup.installer.NewSuffixOptions;
import org.opends.quicksetup.installer.SuffixesToReplicateOptions;
import org.opends.quicksetup.ui.*;
import org.opends.quicksetup.util.Utils;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This is the panel that contains the Review Panel.
 *
 */
public class InstallReviewPanel extends ReviewPanel {

  private static final long serialVersionUID = -7356174829193265699L;

  private boolean displayServerLocation;

  private HashMap<FieldName, JLabel> hmLabels =
      new HashMap<FieldName, JLabel>();

  private HashMap<FieldName, JTextComponent> hmFields =
      new HashMap<FieldName, JTextComponent>();
  private JPanel bottomComponent;
  private JCheckBox startCheckBox;
  private JCheckBox enableWindowsServiceCheckBox;
  private JLabel warningLabel;

  /**
   * Constructor of the panel.
   * @param application Application represented by this panel
   * the fields of the panel.
   */
  public InstallReviewPanel(GuiApplication application)
  {
    super(application);
    this.displayServerLocation = isWebStart();
    populateLabelAndFieldsMap();
  }

  /**
   * {@inheritDoc}
   */
  public void beginDisplay(UserData userData)
  {
    if (displayServerLocation)
    {
      setFieldValue(FieldName.SERVER_LOCATION, userData.getServerLocation());
    }
    setFieldValue(FieldName.HOST_NAME,
        String.valueOf(userData.getHostName()));
    setFieldValue(FieldName.SERVER_PORT,
        String.valueOf(userData.getServerPort()));
    setFieldValue(FieldName.SECURITY_OPTIONS,
        getSecurityOptionsString(userData.getSecurityOptions(), false));
    setFieldValue(FieldName.DIRECTORY_MANAGER_DN, userData
        .getDirectoryManagerDn());
    setFieldValue(FieldName.DATA_OPTIONS, getDataDisplayString(userData));
    if (userData.mustCreateAdministrator())
    {
      setFieldValue(FieldName.GLOBAL_ADMINISTRATOR_UID,
          String.valueOf(userData.getGlobalAdministratorUID()));
      getField(FieldName.GLOBAL_ADMINISTRATOR_UID).setVisible(true);
      getLabel(FieldName.GLOBAL_ADMINISTRATOR_UID).setVisible(true);
    }
    else
    {
      getField(FieldName.GLOBAL_ADMINISTRATOR_UID).setVisible(false);
      getLabel(FieldName.GLOBAL_ADMINISTRATOR_UID).setVisible(false);
    }

    if (userData.getReplicationOptions().getType() ==
      DataReplicationOptions.Type.STANDALONE)
    {
      getField(FieldName.REPLICATION_PORT).setVisible(false);
      getLabel(FieldName.REPLICATION_PORT).setVisible(false);
    }
    else
    {
      setFieldValue(FieldName.REPLICATION_PORT,
          getReplicationPortString(userData));
      getField(FieldName.REPLICATION_PORT).setVisible(true);
      getLabel(FieldName.REPLICATION_PORT).setVisible(true);
    }
    checkStartWarningLabel();
  }

  /**
   * {@inheritDoc}
   */
  public Object getFieldValue(FieldName fieldName)
  {
    Object value = null;
    if (fieldName == FieldName.SERVER_START)
    {
      value = getStartCheckBox().isSelected();
    }
    else if (fieldName == FieldName.ENABLE_WINDOWS_SERVICE)
    {
      value = getEnableWindowsServiceCheckBox().isSelected();
    }
    return value;
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

    hm.put(FieldName.HOST_NAME, new LabelFieldDescriptor(
        getMsg("host-name-label"), getMsg("host-name-tooltip"),
        LabelFieldDescriptor.FieldType.READ_ONLY,
        LabelFieldDescriptor.LabelType.PRIMARY, 0));

    hm.put(FieldName.SERVER_PORT, new LabelFieldDescriptor(
        getMsg("server-port-label"), getMsg("server-port-tooltip"),
        LabelFieldDescriptor.FieldType.READ_ONLY,
        LabelFieldDescriptor.LabelType.PRIMARY, 0));

    hm.put(FieldName.SECURITY_OPTIONS, new LabelFieldDescriptor(
        getMsg("server-security-label"), getMsg("server-security-tooltip"),
        LabelFieldDescriptor.FieldType.READ_ONLY,
        LabelFieldDescriptor.LabelType.PRIMARY, 0));

    hm.put(FieldName.DIRECTORY_MANAGER_DN, new LabelFieldDescriptor(
        getMsg("server-directory-manager-dn-label"),
        getMsg("server-directory-manager-dn-tooltip"),
        LabelFieldDescriptor.FieldType.READ_ONLY,
        LabelFieldDescriptor.LabelType.PRIMARY, 0));

    hm.put(FieldName.GLOBAL_ADMINISTRATOR_UID, new LabelFieldDescriptor(
        getMsg("global-administrator-uid-label"), null,
        LabelFieldDescriptor.FieldType.READ_ONLY,
        LabelFieldDescriptor.LabelType.PRIMARY, 0));

    hm.put(FieldName.DATA_OPTIONS, new LabelFieldDescriptor(
        getMsg("directory-data-label"), null,
        LabelFieldDescriptor.FieldType.READ_ONLY,
        LabelFieldDescriptor.LabelType.PRIMARY, 0));

    hm.put(FieldName.REPLICATION_PORT, new LabelFieldDescriptor(
        getMsg("replication-port-label"), null,
        LabelFieldDescriptor.FieldType.READ_ONLY,
        LabelFieldDescriptor.LabelType.PRIMARY, 0));

    for (FieldName fieldName : hm.keySet())
    {
      LabelFieldDescriptor desc = hm.get(fieldName);

      JTextComponent field = UIFactory.makeJTextComponent(desc, null);
      field.setOpaque(false);
      JLabel label = UIFactory.makeJLabel(desc);

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
  private String getDataDisplayString(UserData userInstallData)
  {
    String msg;

    boolean createSuffix = false;

    DataReplicationOptions repl =
      userInstallData.getReplicationOptions();

    SuffixesToReplicateOptions suf =
      userInstallData.getSuffixesToReplicateOptions();

    createSuffix =
      repl.getType() == DataReplicationOptions.Type.FIRST_IN_TOPOLOGY ||
      repl.getType() == DataReplicationOptions.Type.STANDALONE ||
      suf.getType() == SuffixesToReplicateOptions.Type.NEW_SUFFIX_IN_TOPOLOGY;

    if (createSuffix)
    {
      String arg2;

      NewSuffixOptions options = userInstallData.getNewSuffixOptions();
      switch (options.getType())
      {
      case CREATE_BASE_ENTRY:
        arg2 = getMsg("review-create-base-entry-label",
            new String[] { options.getBaseDn() });

        break;

      case LEAVE_DATABASE_EMPTY:
        arg2 = getMsg("review-leave-database-empty-label");
        break;

      case IMPORT_FROM_LDIF_FILE:
        arg2 = getMsg("review-import-ldif",
            new String[] { options.getLDIFPath() });
        break;

      case IMPORT_AUTOMATICALLY_GENERATED_DATA:
        arg2 = getMsg("review-import-automatically-generated",
            new String[] { String.valueOf(options.getNumberEntries()) });
        break;

      default:
        throw new IllegalArgumentException("Unknow type: " + options.getType());
      }

      msg = getMsg("review-create-suffix",
          new String[] {options.getBaseDn(), arg2});
    }
    else
    {
      StringBuilder buf = new StringBuilder();
      Set<SuffixDescriptor> suffixes = suf.getSuffixes();
      for (SuffixDescriptor suffix : suffixes)
      {
        if (buf.length() > 0)
        {
          buf.append("\n");
        }
        buf.append(suffix.getDN());
      }
      msg = getMsg("review-replicate-suffix", new String[] {buf.toString()});
    }

    return msg;
  }
   /**
    * Returns the String representing the replication port configuration.
    * @param options the DataOptions of the user.
    * @return the localized string describing the Replication Ports chosen by
    * the user.
    */
  private String getReplicationPortString(UserData userInstallData)
  {
    StringBuilder buf = new StringBuilder();

    DataReplicationOptions repl =
      userInstallData.getReplicationOptions();

    SuffixesToReplicateOptions suf =
      userInstallData.getSuffixesToReplicateOptions();

    Map<ServerDescriptor, Integer> remotePorts =
      userInstallData.getRemoteWithNoReplicationPort();

    if ((repl.getType() == DataReplicationOptions.Type.IN_EXISTING_TOPOLOGY) &&
      (suf.getType() ==
        SuffixesToReplicateOptions.Type.REPLICATE_WITH_EXISTING_SUFFIXES) &&
        remotePorts.size() > 0)
    {
      buf.append(userInstallData.getReplicationOptions().getReplicationPort());
      TreeSet<String> remoteServerLines = new TreeSet<String>();
      for (ServerDescriptor server : remotePorts.keySet())
      {
        String[] args = {String.valueOf(remotePorts.get(server)),
            server.getHostPort(true)};
        remoteServerLines.add(getMsg("remote-server-replication-port", args));
      }
      for (String line : remoteServerLines)
      {
        buf.append("\n"+line);
      }
    }
    else
    {
      buf.append(userInstallData.getReplicationOptions().getReplicationPort());
    }
    return buf.toString();
  }
  /**
   * Returns and creates the fields panel.
   * @return the fields panel.
   */
  protected JPanel createFieldsPanel()
  {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setOpaque(false);
    GridBagConstraints gbc = new GridBagConstraints();

    FieldName[] fieldNames;
    if (displayServerLocation)
    {
      fieldNames =
        new FieldName[]
          {
            FieldName.SERVER_LOCATION, FieldName.HOST_NAME,
            FieldName.SERVER_PORT, FieldName.SECURITY_OPTIONS,
            FieldName.DIRECTORY_MANAGER_DN, FieldName.GLOBAL_ADMINISTRATOR_UID,
            FieldName.DATA_OPTIONS, FieldName.REPLICATION_PORT
          };
    }
    else
    {
      fieldNames =
        new FieldName[]
          {
            FieldName.HOST_NAME, FieldName.SERVER_PORT,
            FieldName.SECURITY_OPTIONS, FieldName.DIRECTORY_MANAGER_DN,
            FieldName.GLOBAL_ADMINISTRATOR_UID, FieldName.DATA_OPTIONS,
            FieldName.REPLICATION_PORT
          };
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
      panel.add(getField(fieldNames[i]), gbc);
    }

    return panel;
  }

  /**
   * {@inheritDoc}
   */
  protected JComponent getBottomComponent()
  {
    if (bottomComponent == null)
    {
      bottomComponent = new JPanel(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.anchor = GridBagConstraints.WEST;
      JPanel auxPanel = new JPanel(new GridBagLayout());
      gbc.gridwidth = 3;
      auxPanel.add(getStartCheckBox(), gbc);
      gbc.insets.left = UIFactory.LEFT_INSET_SECONDARY_FIELD;
      gbc.gridwidth = GridBagConstraints.RELATIVE;
      auxPanel.add(getWarningLabel(), gbc);
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.insets.left = 0;
      gbc.weightx = 1.0;
      auxPanel.add(Box.createHorizontalGlue(), gbc);
      bottomComponent.add(auxPanel, gbc);
      if (Utils.isWindows())
      {
        gbc.insets.top = UIFactory.TOP_INSET_PRIMARY_FIELD;
        bottomComponent.add(getEnableWindowsServiceCheckBox(), gbc);
      }
    }
    return bottomComponent;
  }

  private JLabel getWarningLabel()
  {
    if (warningLabel == null)
    {
      warningLabel = UIFactory.makeJLabel(UIFactory.IconType.WARNING,
          getMsg("install-server-must-be-temporarily-started"),
          UIFactory.TextStyle.READ_ONLY);
    }
    return warningLabel;
  }

  private JCheckBox getStartCheckBox()
  {
    if (startCheckBox == null)
    {
      startCheckBox =
          UIFactory.makeJCheckBox(getMsg("start-server-label"),
              getMsg("start-server-tooltip"), UIFactory.TextStyle.CHECKBOX);
      startCheckBox.setSelected(
          getApplication().getUserData().getStartServer());
      startCheckBox.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent ev)
        {
          checkStartWarningLabel();
        }
      });
    }
    return startCheckBox;
  }

  private JCheckBox getEnableWindowsServiceCheckBox()
  {
    if (enableWindowsServiceCheckBox == null)
    {
      enableWindowsServiceCheckBox =
          UIFactory.makeJCheckBox(getMsg("enable-windows-service-label"),
              getMsg("enable-windows-service-tooltip"),
              UIFactory.TextStyle.CHECKBOX);
      enableWindowsServiceCheckBox.setSelected(
          getApplication().getUserData().getEnableWindowsService());
    }
    return enableWindowsServiceCheckBox;
  }

  /**
   * Depending on whether we want to replicate or not, we do have to start
   * the server temporarily to update its configuration and initialize data.
   */
  private void checkStartWarningLabel()
  {
    boolean visible = !getStartCheckBox().isSelected();
    if (visible)
    {
      UserData userData = getApplication().getUserData();
      DataReplicationOptions rep = userData.getReplicationOptions();
      visible = rep.getType() != DataReplicationOptions.Type.STANDALONE;
    }
    getWarningLabel().setVisible(visible);
  }
}
