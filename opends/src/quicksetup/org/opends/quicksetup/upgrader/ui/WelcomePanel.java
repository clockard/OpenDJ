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

package org.opends.quicksetup.upgrader.ui;

import org.opends.quicksetup.ui.*;
import org.opends.quicksetup.util.Utils;
import org.opends.quicksetup.Installation;
import org.opends.quicksetup.UserData;
import org.opends.quicksetup.event.BrowseActionListener;
import org.opends.quicksetup.upgrader.Upgrader;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.ArrayList;

/**
 * This panel is used to show a welcome message.  If this is the WebStart,
 * allows the user to select a build to upgrade.  Otherwise shows the user
 * some readonly information about the current build.
 */
public class WelcomePanel extends QuickSetupStepPanel {

  private static final long serialVersionUID = 8695606871542491768L;

  private JTextComponent tcServerLocation;

  private JTextComponent tcCurrentServerBuildNumber;

  /**
   * Default constructor.
   * @param application Upgrader application
   */
  public WelcomePanel(Upgrader application) {
    super(application);
  }

  /**
   * {@inheritDoc}
   */
  public void beginDisplay(UserData data) {
    super.beginDisplay(data);
    tcServerLocation.setText(data.getServerLocation());
  }

  /**
   * {@inheritDoc}
   */
  public Object getFieldValue(FieldName fieldName) {
    Object v = null;
    if (FieldName.SERVER_LOCATION.equals(fieldName)) {
      v = tcServerLocation.getText();
    }
    return v;
  }

  /**
   * {@inheritDoc}
   */
  protected String getTitle() {
    return getMsg("upgrade-welcome-panel-title");
  }

  /**
   * {@inheritDoc}
   */
  protected String getInstructions() {
    /*
     * We can use org.opends.server.util.DynamicConstants without problems as it
     * has been added to quicksetup.jar during build time.
     */
    java.util.List<String> args = new ArrayList<String>();
    String msgKey;
    if (Utils.isWebStart()) {
      msgKey = "upgrade-welcome-panel-webstart-instructions";
      String cmd = Utils.isWindows() ? Installation.WINDOWS_UPGRADE_FILE_NAME :
              Installation.UNIX_UPGRADE_FILE_NAME;
      args.add(UIFactory.applyFontToHtml(cmd,
              UIFactory.INSTRUCTIONS_MONOSPACE_FONT));
    } else {
      msgKey = "upgrade-welcome-panel-offline-instructions";
    }
    return getMsg(msgKey, args.toArray(new String[0]));
  }

  /**
   * {@inheritDoc}
   */
  protected Component createInputPanel() {
    Component c;

    LabelFieldDescriptor serverLocationDescriptor =
            new LabelFieldDescriptor(getMsg("upgrade-location-label"),
                    getMsg("upgrade-location-tooltip"),
                    LabelFieldDescriptor.FieldType.TEXTFIELD,
                    LabelFieldDescriptor.LabelType.PRIMARY,
                    UIFactory.PATH_FIELD_SIZE);

    LabelFieldDescriptor serverLocationDescriptorRO =
            new LabelFieldDescriptor(getMsg("upgrade-location-label"),
                    getMsg("upgrade-location-tooltip"),
                    LabelFieldDescriptor.FieldType.READ_ONLY,
                    LabelFieldDescriptor.LabelType.PRIMARY,
                    UIFactory.PATH_FIELD_SIZE);

    LabelFieldDescriptor serverBuildDescriptorRO =
            new LabelFieldDescriptor(getMsg("upgrade-build-id-label"),
                    getMsg("upgrade-build-id-tooltip"),
                    LabelFieldDescriptor.FieldType.READ_ONLY,
                    LabelFieldDescriptor.LabelType.PRIMARY,
                    UIFactory.PATH_FIELD_SIZE);

    JPanel pnlBuildInfo = UIFactory.makeJPanel();
    pnlBuildInfo.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    UserData userData = getApplication().getUserData();

    // The WebStart version of this tool allows the user to
    // select a build to upgrade.  Running the tool from the
    // command line implies a build.
    if (Utils.isWebStart()) {

      tcServerLocation =
              UIFactory.makeJTextComponent(serverLocationDescriptor,
                      userData.getServerLocation());

      JButton butBrowse =
              UIFactory.makeJButton(getMsg("browse-button-label"),
                      getMsg("browse-button-tooltip"));

      BrowseActionListener l =
              new BrowseActionListener(tcServerLocation,
                      BrowseActionListener.BrowseType.LOCATION_DIRECTORY,
                      getMainWindow());
      butBrowse.addActionListener(l);

      JPanel pnlBrowser = Utilities.createBrowseButtonPanel(
              UIFactory.makeJLabel(serverLocationDescriptor),
              tcServerLocation,
              butBrowse);
      pnlBrowser.setOpaque(false);

      //pnlBrowser.setBorder(BorderFactory.createLineBorder(Color.GREEN));
      gbc.insets.top = 15; // non-standard but looks better
      gbc.insets.left = UIFactory.LEFT_INSET_PRIMARY_FIELD;
      gbc.anchor = GridBagConstraints.FIRST_LINE_START;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbc.weightx = 1.0;
      gbc.fill = GridBagConstraints.BOTH;
      pnlBuildInfo.add(pnlBrowser, gbc);

      gbc.gridy = 1;
      gbc.weighty = 1.0;
      gbc.weightx = 1.0;
      gbc.fill = GridBagConstraints.BOTH;
      gbc.anchor = GridBagConstraints.LINE_START;
      JPanel fill = UIFactory.makeJPanel();
      // fill.setBorder(BorderFactory.createLineBorder(Color.BLUE));
      pnlBuildInfo.add(fill, gbc);

    } else {

      tcServerLocation = UIFactory.makeJTextComponent(
                       serverLocationDescriptorRO, null);

      String buildId = null;
      Installation installation = getApplication().getInstallation();
      try {
        buildId = installation.getBuildInformation().getBuildId();
      } catch (Exception e) {
        buildId = getMsg("upgrade-build-id-unknown");
      }

      tcCurrentServerBuildNumber = UIFactory.makeJTextComponent(
                        serverBuildDescriptorRO,
                        buildId);

      gbc.gridx = 0;
      gbc.gridy = 0;
      gbc.anchor = GridBagConstraints.FIRST_LINE_START;
      gbc.insets.top = 15; // non-standard but looks better
      gbc.insets.left = UIFactory.LEFT_INSET_PRIMARY_FIELD;
      pnlBuildInfo.add(UIFactory.makeJLabel(serverLocationDescriptorRO), gbc);


      gbc.gridx = 1;
      gbc.gridy = 0;
      gbc.weightx = 1.0;
      gbc.insets.left = UIFactory.LEFT_INSET_PRIMARY_FIELD;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.anchor = GridBagConstraints.PAGE_START;
      pnlBuildInfo.add(tcServerLocation, gbc);

      gbc.gridx = 0;
      gbc.gridy = 1;
      gbc.anchor = GridBagConstraints.LINE_START;
      gbc.insets.left = UIFactory.LEFT_INSET_PRIMARY_FIELD;
      gbc.insets.top = UIFactory.TOP_INSET_PRIMARY_FIELD;
      pnlBuildInfo.add(UIFactory.makeJLabel(serverBuildDescriptorRO), gbc);

      gbc.gridx = 1;
      gbc.gridy = 1;
      gbc.insets.left = UIFactory.LEFT_INSET_PRIMARY_FIELD;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      pnlBuildInfo.add(tcCurrentServerBuildNumber, gbc);

      gbc.gridy = 2;
      gbc.weighty = 1.0;
      gbc.weightx = 1.0;
      gbc.fill = GridBagConstraints.BOTH;
      gbc.anchor = GridBagConstraints.LINE_START;
      JPanel fill = UIFactory.makeJPanel();
      //fill.setBorder(BorderFactory.createLineBorder(Color.BLUE));
      pnlBuildInfo.add(fill, gbc);
    }

    c = pnlBuildInfo;

    return c;
  }


}
