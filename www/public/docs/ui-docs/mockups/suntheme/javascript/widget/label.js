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

dojo.provide("webui.suntheme.widget.label");

dojo.require("dojo.widget.*");
dojo.require("webui.suntheme.*");
dojo.require("webui.suntheme.widget.*");

/**
 * This function will be invoked when creating a Dojo widget. Please see
 * webui.suntheme.widget.label.setProps for a list of supported
 * properties.
 *
 * Note: This is considered a private API, do not use.
 */
webui.suntheme.widget.label = function() {
    // Set defaults.
    this.level = 2;
    this.required = false;
    this.valid = true;
    this.widgetType = "label";

    // Register widget.
    dojo.widget.Widget.call(this);

    /**
     * This function is used to generate a template based widget.
     */
    this.fillInTemplate = function() {
        // Set ids.
        if (this.id) {
            this.requiredImageContainer.id = this.id + "_requiredImageContainer";
            this.errorImageContainer.id = this.id + "_errorImageContainer";
            this.valueContainer.id = this.id + "_valueContainer";
            this.contentsContainer.id = this.id + "_contentsContainer";
        }

        // Set public functions. 
	this.domNode.getProps = function() { return dojo.widget.byId(this.id).getProps(); }
        this.domNode.setProps = function(props) { return dojo.widget.byId(this.id).setProps(props); }

        // Set private functions.
        this.getClassName = webui.suntheme.widget.label.getClassName;
	this.getProps = webui.suntheme.widget.label.getProps;
        this.setProps = webui.suntheme.widget.label.setProps;

        // Initialize properties.
        return webui.suntheme.widget.common.initProps(this);
    }
}

/**
 * Helper function to obtain widget class names.
 */
webui.suntheme.widget.label.getClassName = function() {
    // Set style for default label level.
    var className = webui.suntheme.widget.props.label.levelTwoStyleClass;

    if (this.valid == false) {
        className = webui.suntheme.widget.props.label.errorStyleClass;
    } else if (this.level == 1) {
        className = webui.suntheme.widget.props.label.levelOneStyleClass;
    } else if (this.level == 3) {
        className = webui.suntheme.widget.props.label.levelThreeStyleClass;
    }
    return (this.className)
        ? className + " " + this.className
        : className;
}

/**
 * This function is used to get widget properties. Please see
 * webui.suntheme.widget.label.setProps for a list of supported
 * properties.
 */
webui.suntheme.widget.label.getProps = function() {
    var props = {};

    // Set properties.
    if (this.contents) { props.contents = this.contents; }
    if (this.errorImage) { props.errorImage = this.errorImage; }
    if (this.htmlFor) { props.htmlFor = this.htmlFor; }
    if (this.level != null) { props.level = this.level; }
    if (this.required != null) { props.required = this.required; }
    if (this.requiredImage) { props.requiredImage = this.requiredImage; }
    if (this.valid != null) { props.valid = this.valid; }
    if (this.value) { props.value = this.value; }

    // Add DOM node properties.
    Object.extend(props, webui.suntheme.widget.common.getCommonProps(this));
    Object.extend(props, webui.suntheme.widget.common.getCoreProps(this));
    Object.extend(props, webui.suntheme.widget.common.getJavaScriptProps(this));

    return props;
}

/**
 * This function is used to set widget properties with the
 * following Object literals. In addition the "contents" 
 * property is an array of children of the label.
 *
 * <ul>
 *  <li>accesskey</li>
 *  <li>className</li>
 *  <li>contents</li>
 *  <li>dir</li>
 *  <li>errorImage</li>
 *  <li>htmlFor</li>
 *  <li>id</li>
 *  <li>lang</li>
 *  <li>level</li>
 *  <li>onClick</li>
 *  <li>onDblClick</li>
 *  <li>onFocus</li>
 *  <li>onKeyDown</li>
 *  <li>onKeyPress</li>
 *  <li>onKeyUp</li>
 *  <li>onMouseDown</li>
 *  <li>onMouseOut</li>
 *  <li>onMouseOver</li>
 *  <li>onMouseUp</li>
 *  <li>onMouseMove</li>
 *  <li>required</li>
 *  <li>requiredImage</li>
 *  <li>style</li>
 *  <li>title</li>
 *  <li>valid</li>
 *  <li>value</li>
 *  <li>visible</li>
 * </ul>
 *
 * @param props Key-Value pairs of properties.
 */
webui.suntheme.widget.label.setProps = function(props) {
    // Save properties for later updates.
    if (props != null) {
        webui.suntheme.widget.common.extend(this, props);
    } else {
        props = this.getProps(); // Widget is being initialized.
    }

    // Set style class -- must be set before calling setCoreProps().
    props.className = this.getClassName();
    
    // Set DOM node properties.
    webui.suntheme.widget.common.setCoreProps(this.domNode, props);
    webui.suntheme.widget.common.setCommonProps(this.domNode, props);
    webui.suntheme.widget.common.setJavaScriptProps(this.domNode, props);

    if (props.htmlFor) { this.domNode.htmlFor = props.htmlFor; }

    // Set label value.
    if (props.value) {
        webui.suntheme.widget.common.addFragment(this.valueContainer,
            dojo.string.escape("html", props.value)); 
    }
  
    // Set error image properties.
    if (props.errorImage || props.valid != null && this.errorImage) {
        // Ensure property exists so we can call setProps just once.
        if (props.errorImage == null) {
            props.errorImage = {};
        }

        // Show error image.
        props.errorImage.visible = (this.valid != null)
            ? !this.valid : false;

        // Update widget/add fragment.
        var errorImageWidget = dojo.widget.byId(this.errorImage.id); 
        if (errorImageWidget) {
            errorImageWidget.setProps(props.errorImage);
        } else {
            webui.suntheme.widget.common.addFragment(this.errorImageContainer,
                props.errorImage);
        }
    }

    // Set required image properties.
    if (props.requiredImage || props.required != null && this.requiredImage) {       
        // Ensure property exists so we can call setProps just once.
        if (props.requiredImage == null) {
            props.requiredImage = {};
        }

        // Show required image.
        props.requiredImage.visible = (this.required != null)
            ? this.required : false;

        // Update widget/add fragment.
        var requiredImageWidget = dojo.widget.byId(this.requiredImage.id);
        if (requiredImageWidget) {
            requiredImageWidget.setProps(props.requiredImage);
        } else {
            webui.suntheme.widget.common.addFragment(this.requiredImageContainer,
                props.requiredImage);
        }
    }

    // Set contents.
    if (props.contents) {
	this.contentsContainer.innerHtml = "";
	for (var i = 0; i < props.contents.length; i++) {
            webui.suntheme.widget.common.addFragment(this.contentsContainer, 
		props.contents[i], "last");
        }
    }
    return true;
}

dojo.inherits(webui.suntheme.widget.label, dojo.widget.HtmlWidget);

//-->
