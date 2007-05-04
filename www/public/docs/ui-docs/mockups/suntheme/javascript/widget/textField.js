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

dojo.provide("webui.suntheme.widget.textField");

dojo.require("dojo.widget.*");
dojo.require("webui.suntheme.*");
dojo.require("webui.suntheme.widget.*");

/**
 * This function will be invoked when creating a Dojo widget. Please see
 * webui.suntheme.widget.textField.setProps for a list of supported
 * properties.
 *
 * Note: This is considered a private API, do not use.
 */
webui.suntheme.widget.textField = function() {
    // Set defaults.
    this.disabled   = false;
    this.required   = false;
    this.size       = 20;
    this.valid      = true;
    this.widgetType = "textField";
    
    // Register widget.
    dojo.widget.Widget.call(this);
    
    /**
     * This function is used to generate a template based widget.
     */
    this.fillInTemplate = function() {
        // Set ids.
        if (this.id) {
            this.labelContainer.id = this.id + "_label";
            this.textFieldNode.id = this.id + "_field";
            this.textFieldNode.name = this.id + "_field";
        }
        
        // Set public functions.
        this.domNode.setProps = function(props) { return dojo.widget.byId(this.id).setProps(props); }
        this.domNode.getInputElement = function() { return dojo.widget.byId(this.id).getInputElement(); }
        this.domNode.getProps = function() { return dojo.widget.byId(this.id).getProps(); }
        
        // Set private functions .
        this.setProps     = webui.suntheme.widget.textField.setProps;
        this.getProps     = webui.suntheme.widget.textField.getProps;
        this.getClassName = webui.suntheme.widget.textField.getClassName;
        
        // Set events.
        if (this.autoValidate == true) {
            // Generate the following event ONLY when 'autoValidate' == true.
            dojo.event.connect(this.textFieldNode, "onblur", 
                webui.suntheme.widget.textField.validation.processEvent);
        }

        // Initialize properties.
        return webui.suntheme.widget.common.initProps(this);
    }
}

/**
 * Helper function to obtain widget class names.
 */
webui.suntheme.widget.textField.getClassName = function() {
    // Set default style.    
    var className = (this.disabled == true)
        ? webui.suntheme.widget.props.textField.disabledClassName
        : webui.suntheme.widget.props.textField.className;
    
    return className;
}

/**
 * Returns the HTML input element that makes up the text field.
 *
 * @return a reference to the HTML input element. 
 */
webui.suntheme.widget.textField.getInputElement = function() {
    return this.textFieldNode;
}

/**
 * This function is used to get widget properties. 
 * @see webui.suntheme.widget.textField.setProps for a list of supported
 * properties.
 */
webui.suntheme.widget.textField.getProps = function() {
    var props = {};
    
    // Set properties.
    if (this.alt) { props.alt = this.alt; }
    if (this.disabled != null) { props.disabled = this.disabled; }
    if (this.label) { props.label= this.label; }
    if (this.text) { props.text = this.text; }
    if (this.title) { props.title = this.title; }
    if (this.type) { props.type= this.type; }
    if (this.required != null) { props.required = this.required; }
    if (this.size) { props.size = this.size; }
    if (this.valid != null) { props.valid = this.valid; }

    // After widget has been initialized, get user's input.
    if (this.initialized == true && this.textFieldNode.value != null) {
        props.value = this.textFieldNode.value;
    } else if (this.value != null) {
        props.value = this.value;
    }

    // Add DOM node properties.
    Object.extend(props, webui.suntheme.widget.common.getCommonProps(this));
    Object.extend(props, webui.suntheme.widget.common.getCoreProps(this));
    Object.extend(props, webui.suntheme.widget.common.getJavaScriptProps(this));

    return props;
}

/**
 * This function is used to set widget properties with the
 * following Object literals.
 *
 * <ul>
 *  <li>accesskey</li>
 *  <li>className</li>
 *  <li>dir</li>
 *  <li>disabled</li>
 *  <li>id</li>
 *  <li>label</li>
 *  <li>lang</li>
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
 *  <li>size</li>
 *  <li>style</li>
 *  <li>tabIndex</li>
 *  <li>title</li>
 *  <li>valid</li>
 *  <li>value</li>
 *  <li>visible</li> 
 * </ul>
 *
 * @param props Key-Value pairs of properties.
 */
webui.suntheme.widget.textField.setProps = function(props) {   
    // Save properties for later updates.
    if (props != null) {
        webui.suntheme.widget.common.extend(this, props);
    } else {
        props = this.getProps(); // Widget is being initialized.
    }
    
    // Set attributes.  
    webui.suntheme.widget.common.setCoreProps(this.domNode, props);
    webui.suntheme.widget.common.setCommonProps(this.textFieldNode, props);
    webui.suntheme.widget.common.setJavaScriptProps(this.textFieldNode, props);
    
    // Set text field attributes.    
    if (props.size) { this.textFieldNode.size = props.size; }
    if (props.value) { this.textFieldNode.value = props.value; }
    if (props.disabled != null) { 
        this.textFieldNode.disabled = new Boolean(props.disabled).valueOf();
    }
    
    this.textFieldNode.className = this.getClassName();

    // Set label properties.
    if (props.label || (props.valid != null || props.required != null) && this.label) {
        // Ensure property exists so we can call setProps just once.
        if (props.label == null) {
            props.label = {};
        }
        
        // Set valid.
        if (props.valid != null) { props.label.valid = props.valid; }
        
        // Set required.
        if (props.required != null) { props.label.required = props.required; }
        
        // Update widget/add fragment.                
        var labelWidget = dojo.widget.byId(this.label.id);
        if (labelWidget) {
            labelWidget.setProps(props.label);
        } else {
            webui.suntheme.widget.common.addFragment(this.labelContainer, props.label);
        }
    }
    return true;
}

/**
 * This closure is used to publish validation events.
 */
webui.suntheme.widget.textField.validation = {
    /**
     * Event topics for custom AJAX implementations to listen for.
     */
    beginEventTopic: "webui_suntheme_widget_textField_validation_begin",
    endEventTopic: "webui_suntheme_widget_textField_validation_end",
    
    /**
     * Process validation event.
     * This function interprets an event ( one of onXXX events, such as onBlur, etc) and
     * extracts data needed for subsequent ajax request generation - specifically 
     * <ol>
     *  <li>widget id it that has generated the event
     * </ol>
     * if widget id is not found, propagation of the event is stopped, otherwise
     * publishBeginEvent is called with extracted data.
     *
     * @param event Event generated by the widget.
     */
    processEvent: function(event) {
        if (event == null) {
            return false;
        }
        
        // Publish event to retrieve data.
        webui.suntheme.widget.textField.validation.publishBeginEvent({
            id: event.currentTarget.parentNode.id
        });
    },
    
    /**
     * Publish an event for custom AJAX implementations to listen for.
     *
     * @param event Event generated by the widget.
     */
    publishBeginEvent: function(props) {
        dojo.event.topic.publish(webui.suntheme.widget.textField.validation.beginEventTopic, props);
        return true;
    },
    
    /**
     * Publish an event for custom AJAX implementations to listen for. For
     * example, an alert component may need to be updated when ever a text
     * field value is found to be invalid.
     *
     * @param props Key-Value pairs of properties of the widget.
     */
    publishEndEvent: function(props) {
        dojo.event.topic.publish(webui.suntheme.widget.textField.validation.endEventTopic, props);
        return true;
    }
}

dojo.inherits(webui.suntheme.widget.textField, dojo.widget.HtmlWidget);

//-->
