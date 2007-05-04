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

dojo.provide("webui.suntheme.widget.radioButton");

dojo.require("dojo.widget.*");
dojo.require("webui.suntheme.*");
dojo.require("webui.suntheme.widget.*");

/**
 * This function will be invoked when creating a Dojo widget. Please see
 * webui.suntheme.widget.radioButton.setProps for a list of supported
 * properties.
 *
 * Note: This is considered a private API, do not use.
 */
webui.suntheme.widget.radioButton = function() {    
    // Set defaults
    this.widgetType = "radioButton";    
    
    // Register widget
    dojo.widget.Widget.call(this);

    /**
     * This function is used to generate a template based widget.
     */
    this.fillInTemplate = function() {
        // Set ids
        if (this.id) {              
            this.radioButtonNode.id = this.id + "_rb";
            this.imageContainer.id = this.id + "_imageContainer";
            this.labelContainer.id = this.id + "_labelContainer";

            // If null, use HTML input id.
            if (this.name == null) { this.name = this.radioButtonNode.id; }
        } 
    
        // Set public functions
        this.domNode.getProps = function() { return dojo.widget.byId(this.id).getProps(); }
        this.domNode.setProps = function(props) { return dojo.widget.byId(this.id).setProps(props); }
        this.domNode.getInputElement = function() { return dojo.widget.byId(this.id).getInputElement(); }
        this.domNode.refresh = function(execute) { return dojo.widget.byId(this.id).refresh(execute); }

        // Set private functions 
        this.getClassName = webui.suntheme.widget.radioButton.getClassName;      
        this.getProps = webui.suntheme.widget.radioButton.getProps;
        this.setProps = webui.suntheme.widget.radioButton.setProps;    
        this.getInputElement = webui.suntheme.widget.radioButton.getInputElement;
        this.refresh = webui.suntheme.widget.radioButton.refresh.processEvent;

        // Initialize properties.
        return webui.suntheme.widget.common.initProps(this);   
    }    
}

/**
 * Helper function to obtain widget class names.
 */
webui.suntheme.widget.radioButton.getClassName = function() {
    // Set style class for the span element.
    var className = webui.suntheme.widget.props.radioButton.spanClassName;
    if (this.disabled == true) {
        className = webui.suntheme.widget.props.radioButton.spanDisabledClassName;
    }
    return (this.className)
        ? className + " " + this.className
        : className;
}

/**
  * To get the HTML Input element.   
  *
  * @return HTML Input element. 
  */
 webui.suntheme.widget.radioButton.getInputElement = function() {
     return this.radioButtonNode;
 }

/**
 * This function is used to get widget properties. Please see
 * webui.suntheme.widget.radioButton.setProps for a list of supported
 * properties.
 */
webui.suntheme.widget.radioButton.getProps = function() {
    var props = {};

    // Set properties.  
    if (this.align) { props.align = this.align; }
    if (this.disabled != null) { props.disabled = this.disabled; }   
    if (this.image) { props.image = this.image; }
    if (this.label) { props.label = this.label; }
    if (this.name) { props.name = this.name; }        
    if (this.readOnly != null) { props.readOnly = this.readOnly; }
    if (this.value) { props.value = this.value; }

    // After widget has been initialized, get user's input.
    if (this.initialized == true && this.radioButtonNode.checked != null) {
        props.checked = this.radioButtonNode.checked;
    } else if (this.checked != null) {
        props.checked = this.checked;
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
 *  <li>align</li>
 *  <li>checked</li>
 *  <li>className</li>
 *  <li>dir</li>
 *  <li>disabled</li>
 *  <li>id</li>
 *  <li>image</li>
 *  <li>label</li>
 *  <li>lang</li>
 *  <li>name</li>
 *  <li>onBlur</li>
 *  <li>onChange</li>
 *  <li>onClick</li> 
 *  <li>onFocus</li> 
 *  <li>onKeyDown</li>
 *  <li>onKeyPress</li>
 *  <li>onKeyUp</li>
 *  <li>onMouseDown</li>
 *  <li>onMouseMove</li>
 *  <li>onMouseOut</li>
 *  <li>onMouseOver</li>
 *  <li>onMouseUp</li> 
 *  <li>onSelect</li>
 *  <li>readOnly</li>    
 *  <li>style</li>
 *  <li>tabIndex</li>
 *  <li>title</li>
 *  <li>value</li>  
 *  <li>visible</li>  
 * </ul>
 *
 * @param props Key-Value pairs of properties.
 */
webui.suntheme.widget.radioButton.setProps = function(props) {
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
    webui.suntheme.widget.common.setCommonProps(this.radioButtonNode, props);
    webui.suntheme.widget.common.setJavaScriptProps(this.radioButtonNode, props);        

    if (props.value) {
        this.radioButtonNode.value = props.value;
    }
    if (props.readOnly != null) {
        this.radioButtonNode.readonly = new Boolean(props.readOnly).valueOf();
    }
    if (props.checked != null) { 
        this.radioButtonNode.checked = new Boolean(props.checked).valueOf();
    }
    if (props.disabled != null) { 
        this.radioButtonNode.disabled = new Boolean(props.disabled).valueOf();
    }
    if (props.name) { 
        this.radioButtonNode.name = props.name;
    }

    // Set image properties.
    if (props.image || props.disabled != null && this.image) {
        // Ensure property exists so we can call setProps just once.
        if (props.image == null) {
            props.image = {};
        }
        
        // Setting style class.
        if (props.disabled == true) {
            props.image.className = webui.suntheme.widget.props.radioButton.imageDisabledClassName;
        } else {
            props.image.className = webui.suntheme.widget.props.radioButton.imageClassName; 
        }
        
        // Update widget/add fragment.
        var imageWidget = dojo.widget.byId(this.image.id);        
        if (imageWidget) {
            imageWidget.setProps(props.image);        
        } else {        
            webui.suntheme.widget.common.addFragment(this.imageContainer, props.image);
        }
    }  
  
    // Set label propertiess.        
    if (props.label || props.disabled != null && this.label) {     
        // Ensure property exists so we can call setProps just once.
        if (props.label == null) {
            props.label = {};
        }
        
        // Setting style class.
        if (props.disabled == true) {            
            props.label.className = webui.suntheme.widget.props.radioButton.labelDisabledClassName; 
        } else {
            props.label.className = webui.suntheme.widget.props.radioButton.labelClassName;
        }

        // update widget/add fragment.
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
  * This closure is used to process refresh events.
  */
 webui.suntheme.widget.radioButton.refresh = {
     /**
      * Event topics for custom AJAX implementations to listen for.
      */
     beginEventTopic: "webui_suntheme_widget_radioButton_refresh_begin",
     endEventTopic: "webui_suntheme_widget_radioButton_refresh_end",
 
    /**
     * Process refresh event.
     *
     * @param execute Comma separated string containing a list of client ids 
      * against which the execute portion of the request processing lifecycle
      * must be run.
      */
     processEvent: function(execute) {
         // Publish event.
         webui.suntheme.widget.radioButton.refresh.publishBeginEvent({
             id: this.id,
             execute: execute
         });
         return true;
     },
 
     /**
      * Publish an event for custom AJAX implementations to listen for.
      *
      * @param props Key-Value pairs of properties of the widget.
      */
     publishBeginEvent: function(props) {
         dojo.event.topic.publish(webui.suntheme.widget.radioButton.refresh.beginEventTopic, props);
         return true;
     },
 
     /**
      * Publish an event for custom AJAX implementations to listen for.
      *
      * @param props Key-Value pairs of properties of the widget.
      */
     publishEndEvent: function(props) {
         dojo.event.topic.publish(webui.suntheme.widget.radioButton.refresh.endEventTopic, props);
         return true;
     }
 }

dojo.inherits(webui.suntheme.widget.radioButton, dojo.widget.HtmlWidget);

//-->
