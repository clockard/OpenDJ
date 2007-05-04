//<!--
// The contents of this file are subject to the terms
// of the Common Development and Distribution License
// (the License).  You may not use this file except in
// compliance with the License.
// 
// You can obtain a copy of the license at
// https://woodstock.dev.java.net/public/CDDLv1.0.html.
// See the License for the specific language governing
// permissions and limitations under the License.
// 
// When distributing Covered Code, include this CDDL
// Header Notice in each file and include the License file
// at https://woodstock.dev.java.net/public/CDDLv1.0.html.
// If applicable, add the following below the CDDL Header,
// with the fields enclosed by brackets [] replaced by
// you own identifying information:
// "Portions Copyrighted [year] [name of copyright owner]"
// 
// Copyright 2007 Sun Microsystems, Inc. All rights reserved.
//

dojo.provide("webui.suntheme.widget.hiddenField");

dojo.require("dojo.widget.*");
dojo.require("webui.suntheme.*");
dojo.require("webui.suntheme.widget.*");

/**
 * This function will be invoked when creating a Dojo widget. Please see
 * webui.suntheme.widget.hiddenField.setProps for a list of supported
 * properties.
 *
 * Note: This is considered a private API, do not use.
 */
webui.suntheme.widget.hiddenField = function() {
    // Set defaults.
    this.disabled = false;
    this.widgetType = "hiddenField";

    // Register widget.
    dojo.widget.Widget.call(this);
    
    /**
     * This function is used to generate a template based widget.
     */
    this.fillInTemplate = function() {
        // Set public functions. 
        this.domNode.getProps = function() { return dojo.widget.byId(this.id).getProps(); }
        this.domNode.setProps = function(props) { return dojo.widget.byId(this.id).setProps(props); }

        // Set private functions.
        this.getProps = webui.suntheme.widget.hiddenField.getProps;
        this.setProps = webui.suntheme.widget.hiddenField.setProps;

        // Initialize properties.
        return webui.suntheme.widget.common.initProps(this);
    }
}

/**
 * This function is used to get widget properties. Please see
 * webui.suntheme.widget.hiddenField.setProps for a list of supported
 * properties.
 */
webui.suntheme.widget.hiddenField.getProps = function() {
    var props = {};

    // Set properties.
    if (this.disabled != null) { props.disabled = this.disabled; }
    if (this.name) { props.name = this.name; }
    if (this.value) { props.value = this.value; }

    // Add DOM node properties.
    Object.extend(props, webui.suntheme.widget.common.getCoreProps(this));

    return props;
}

/**
 * This function is used to set widget properties with the
 * following Object literals.
 *
 * <ul>
 *  <li>disabled</li>
 *  <li>id</li>
 *  <li>name</li>
 *  <li>value</li>
 * </ul>
 *
 * @param props Key-Value pairs of properties.
 */
webui.suntheme.widget.hiddenField.setProps = function(props) {
    // Save properties for later updates.
    if (props != null) {
        webui.suntheme.widget.common.extend(this, props);
    } else {
        props = this.getProps(); // Widget is being initialized.
    }

    // Set attributes.
    webui.suntheme.widget.common.setCoreProps(this.domNode, props);

    if (props.name) { this.domNode.name = props.name; }
    if (props.value) { this.domNode.value = props.value; }
    if (props.disabled != null) { 
        this.domNode.disabled = new Boolean(props.disabled).valueOf();
    }
    return true;
}

dojo.inherits(webui.suntheme.widget.hiddenField, dojo.widget.HtmlWidget);

//-->
