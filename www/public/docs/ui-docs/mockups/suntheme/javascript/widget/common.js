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

dojo.provide("webui.suntheme.widget.common");

dojo.require("dojo.widget.*");
dojo.require("webui.suntheme.*");

/**
 * Define webui.suntheme.widget.common name space.
 */
webui.suntheme.widget.common = {
    /**
     * Helper function to add a widget or string to the given
     * parent node. If props is an object containing a _widgetType
     * value, a widget will be added to the given parent node per the
     * specified position. If props contains a _modules property,
     * the given resources shall be retrieved before creating the
     * widget. If props is a string, it will be added as the contents
     * of the given parent node.
     *
     * Note: The position argument can be null for strings. However,
     * if fragments must be added to the same DOM node, use "last", "first", 
     * etc. -- arguments are passed though to Dojo's createWidget function.
     *
     * @param parentNode The parent node used to add widget.
     * @param props Key-Value pairs of properties.
     * @param position The position (e.g., first, last, etc.) to add widget.
     */
    addFragment: function(parentNode, props, position) {
        if (props == null || parentNode == null) {
            return false;
        }

        // Add fragment.
        if (typeof props == 'string') {
            // Strip script fragments, set innerHTML property, and
            // eval scripts using a timeout.
            //
            // Note that using Dojo's ContentPane widget would have
            // been more preferable than creating a new dependency
            // based on Prototype. However, as of version .4.1, Dojo
            // still does not use a timeout to eval JavaScript; thus,
            // IE generates errors with innerHTML. For example: 
            //
            // var pane = dojo.widget.createWidget("ContentPane", { 
            //     executeScripts: true,
            //     scriptSeparation: false
            // }, parentNode, position);
            // pane.setContent(props);
            //
            // "The problem has to do with the browser's poor
            // threading model. Basically, once some JavaScript code
            // is running, all other threads of execution are on hold
            // until the running thread is finished with it's
            // task. This includes whatever thread of execution that
            // updates the DOM model when new content is inserted via
            // innerHTML. For example if you do:
            //
            // foo.innerHTML = '<span id="bar">Bar</span>';
            // var bar = document.getElementById('bar');
            //
            // This code will sometimes fail because although you
            // added the content via innerHTML the DOM may not have
            // been updated and therefore the "bar" element does not
            // exist in the DOM yet. To work around this you can do:
            //
            // foo.innerHTML = '<span id="bar">Bar</span>';
            // setTimeout(function() {var bar = document.getElementById('bar');},10);
            //
            // This will work because in 10 milliseconds whatever
            // event handler is running will have finished, the DOM
            // will update, then the callback will execute and have no
            // problem accessing the DOM element."
            //
            // The full discussion on this topic can be found at:
            //
            // http://www.ruby-forum.com/topic/73990
            //

            // Append html if postion is given.
            var html = props.stripScripts();
            if (position && parentNode.innerHTML) {
                html = parentNode.innerHTML + html;
            }

            parentNode.innerHTML = html;
            setTimeout(function() {props.evalScripts()}, 10);
        } else {
            if (props._widgetType == null) {
                return false;
            } else {
                // Retrieve required modules.
                if (props._modules) {
                    for (var i = 0; i < props._modules.length; i++) {
                        webui.suntheme.widget.common.require(props._modules[i]);
                    }
                }

                // Create widget.
                dojo.widget.createWidget(props._widgetType, props, parentNode,
                    (position) ? position : null); // Replace existing node, if null.
            }
        }
        return true;
    },

    /**
     * Helper function to create a widget. This function assumes that
     * an HTML element is used as a place holder for the document
     * fragment (i.e., widget).
     *
     * Note: If props does not contain id and _widgetType properties,
     * a widget shall not be created. If props contains a _modules
     * property, the given resources shall be retrieved before creating
     * the widget.
     *
     * @param props Key-Value pairs of properties.
     */
    createWidget: function(props) {
        var newNode = null;
        if (props == null || props.id == null || props._widgetType == null) {
            return newNode;
        }
        var oldNode = document.getElementById(props.id);
        if (oldNode != null) {
            // Retrieve required modules.
            if (props._modules) {
                for (var i = 0; i < props._modules.length; i++) {
                    webui.suntheme.widget.common.require(props._modules[i]);
                }
            }

            // Create widget.
            newNode = dojo.widget.createWidget(props._widgetType, props)
            if (newNode != null) {
                // Replace existing child, if any.
                oldNode.parentNode.replaceChild(newNode.domNode, oldNode);
            }
        }
        return newNode; // Return widget object for calling widget.destroy().
    },

    /**
     * This function is used to extend the given object with Key-Value pairs of
     * properties. If a property is an object containing Key-Value pairs itself,
     * this function is called recursively to preserve data which is not
     * explicitly extended.
     *
     * Note: If all top level properties must be replaced, use Prototype's 
     * Object.extend function.
     *
     * @param obj The object to extend.
     * @param props Key-Value pairs of properties.
     */
    extend: function(obj, props) {
        if (obj == null || props == null) {
            return false;
        }
        for (var property in props) {
            if (obj[property] && typeof obj[property] == "object") {
                webui.suntheme.widget.common.extend(obj[property], props[property]);
            } else {
                obj[property] = props[property];
            }
        }
        return true;
    },

    /**
     * This function is used to get common properties for the given obj. Please
     * see webui.suntheme.widget.common.setCommonProps for a list of supported
     * properties.
     *
     * @param obj The object containing properties.
     */
    getCommonProps: function(obj) {
        var props = {};
        if (obj == null) {
            return props;
        }

        // Set properties.
        if (obj.accesskey) { props.accesskey = obj.accesskey; }
        if (obj.dir) { props.dir = obj.dir; }
        if (obj.lang) { props.lang = obj.lang; }
        if (obj.tabIndex) { props.tabIndex = obj.tabIndex; }
        if (obj.title) { props.title = obj.title; }

        return props;
    },

    /**
     * This function is used to get core properties for the given obj. Please
     * see webui.suntheme.widget.common.setCoreProps for a list of supported
     * properties.
     *
     * @param obj The object containing properties.
     */
    getCoreProps: function(obj) {
        var props = {};
        if (obj == null) {
            return props;
        }

        // Set properties.
        if (obj.className) { props.className = obj.className; }
        if (obj.id) { props.id = obj.id; }
        if (obj.style) { props.style = obj.style; }
        if (obj.visible != null) { props.visible = obj.visible; }

        return props;
    },

    /**
     * This function is used to get JavaScript properties for the given obj.
     * Please see webui.suntheme.widget.common.setJavaScriptProps for a list of
     * supported properties.
     *
     * @param obj The object containing properties.
     */
    getJavaScriptProps: function(obj) {
        var props = {};
        if (obj == null) {
            return props;
        }

        // Set properties.
        if (obj.onBlur) { props.onBlur = obj.onBlur; }
        if (obj.onChange) { props.onChange = obj.onChange; }
        if (obj.onClick) { props.onClick = obj.onClick; }
        if (obj.onDblClick) { props.onDblClick = obj.onDblClick; }
        if (obj.onFocus) { props.onFocus = obj.onFocus; }
        if (obj.onKeyDown) { props.onKeyDown = obj.onKeyDown; }
        if (obj.onKeyPress) { props.onKeyPress = obj.onKeyPress; }
        if (obj.onKeyUp) { props.onKeyUp = obj.onKeyUp; }
        if (obj.onMouseDown) { props.onMouseDown = obj.onMouseDown; }
        if (obj.onMouseOut) { props.onMouseOut = obj.onMouseOut; }
        if (obj.onMouseOver) { props.onMouseOver = obj.onMouseOver; }
        if (obj.onMouseUp) { props.onMouseUp = obj.onMouseUp; }
        if (obj.onMouseMove) { props.onMouseMove = obj.onMouseMove; }
        if (obj.onSelect) { props.onSelect = obj.onSelect; }

        return props;
    },

    /**
     * Helper function to initialize widget.
     *
     * Note: There is a timing issue on IE 6 regarding when DOM nodes can be 
     * initialized. For example, even though a checkbox widget has set its 
     * checked property to true, the browser will not display the checkbox as
     * selected. If initialization is delayed slightly, the browser will display
     * the HTML element with the correct state. Therefore, this function will
     * call the setProps() function of the given widget using a timeout. When
     * IE 6 is not detected, the setProps() function will be called directly.
     * In both cases, an "initialized" flag is set. This can be used by the
     * getProps() function in order to retrieve user input after the widget has
     * been initialized -- see webui.suntheme.widget.checkbox.getProps.
     *
     * @param widget The widget object to initialize.
     */
    initProps: function(widget) {
        if (widget == null) {
            return false;
        }
        if (webui.suntheme.common.browser.is_ie6 == true) {
            setTimeout(
                // New literals are created every time this function
                // is called, and it's saved by closure magic.
                function() {
                    var obj = dojo.widget.byId(widget.id);
                    if (obj == null) {
                        return false;
                    }
                    obj.setProps(); // No args during initialization.
                    obj.initialized = true; // Set for getProps() function.
                }, 0); // (n) milliseconds delay.
        } else {
            widget.setProps(); // No args during initialization.
            widget.initialized = true; // Set for getProps() function.
        }
        return true;
    },

    /**
     * Helper function to remove child nodes from given DOM node.
     *
     * Note: Child nodes can be cleared using the innerHTML property. However,
     * IE fails when innerHTML is set via the widget's fillInTemplate function.
     * In this scenario, child nodes will be removed via the standard Node APIs.
     *
     * @param domNode The DOM node to remove child nodes.
     */
    removeChildNodes: function(domNode) {
        if (domNode == null || domNode.hasChildNodes() == false) {
            return false;
        }

        try {
            domNode.innerHTML = "";
        } catch (error) {
            var childNodes = domNode.childNodes;
            for(var i = 0; i < childNodes.length; i++) {
                domNode.removeChild(childNodes[i]);
            }
        }
        return true;
    },

    /**
     * Helper function to obtain a module resource.
     *
     * @param module The module resource to retrieve.
     */
    require: function(module) {
        if (module == null) {
            return false;
        }

        // Warning: Do not use dojo.require() here.
        //
        // Dojo appears to parse JavaScript for dojo.require()
        // statments when djConfig.debugAtAllCosts is true. At
        // the time, "modules" is undefined and an error is thrown.
        dojo.require.apply(dojo, [module]);
        return true;
    },

    /**
     * This function is used to set common properties for the given DOM node
     * with the following Object literals.
     *
     * <ul>
     *  <li>accesskey</li>
     *  <li>dir</li>
     *  <li>lang</li>
     *  <li>tabIndex</li>
     *  <li>title</li>
     * </ul>
     *
     * @param domNode The DOM node to assign properties to.
     * @param props Key-Value pairs of properties.
     */
    setCommonProps: function(domNode, props) {
        if (domNode == null || props == null) {
            return false;
        }
        if (props.accesskey) { 
            domNode.accesskey = props.accesskey;
        }
        if (props.dir) {
            domNode.dir = props.dir;
        }
        if (props.lang) {
            domNode.lang = props.lang;
        }
        if (props.tabIndex > -1 && props.tabIndex < 32767) {
            domNode.tabindex = props.tabIndex;
        }
        if (props.title) {
            domNode.title = props.title;
        }
        return true;
    },

    /**
     * This function is used to set core properties for the given DOM
     * node with the following Object literals. These properties are typically
     * set on the outermost element.
     *
     * <ul>
     *  <li>className</li>
     *  <li>id</li>
     *  <li>style</li>
     *  <li>visible</li>
     * </ul>
     *
     * Note: The className is typically provided by a web app developer. If 
     * the widget has a default className, it should be added to the DOM node
     * prior to calling this function. For example, the "myCSS" className would
     * be appended to the existing "Tblsun4" className (e.g., "Tbl_sun4 myCSS").
     *
     * @param domNode The DOM node to assign properties to.
     * @param props Key-Value pairs of properties.
     */
    setCoreProps: function(domNode, props) {
        if (domNode == null || props == null) {
            return false;
        }
        if (props.className) {
            domNode.className = props.className;
        }
        if (props.id) { 
            domNode.id = props.id;
        }
        if (props.style) { 
            domNode.style.cssText = props.style;
        }
        if (props.visible != null) {
            webui.suntheme.common.setVisibleElement(domNode, 
                new Boolean(props.visible).valueOf());
        }
        return true;
    },

    /**
     * This function is used to set JavaScript properties for the given DOM
     * node with the following Object literals.
     *
     * <ul>
     *  <li>onBlur</li>
     *  <li>onChange</li>
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
     *  <li>onSelect</li>
     * </ul>
     *
     * @param domNode The DOM node to assign properties to.
     * @param props Key-Value pairs of properties.
     */
    setJavaScriptProps: function(domNode, props) {
        if (domNode == null || props == null) {
            return false;
        }

        // Note: JSON strings are not recognized as JavaScript. In order for
        // events to work properly, an anonymous function must be created.
        if (props.onBlur) { 
            domNode.onblur = (typeof props.onBlur == 'string')
                ? new Function("event", props.onBlur)
                : props.onBlur;
        }
        if (props.onClick) {
            domNode.onclick = (typeof props.onClick == 'string')
                ? new Function("event", props.onClick)
                : props.onClick;
        }
        if (props.onChange) {
            domNode.onchange = (typeof props.onChange == 'string')
                ? new Function("event", props.onChange)
                : props.onChange;
        }
        if (props.onDblClick) {
            domNode.ondblclick = (typeof props.onDblClick == 'string')
                ? new Function("event", props.onDblClick)
                : props.onDblClick;
        }
        if (props.onFocus) {
            domNode.onfocus = (typeof props.onFocus == 'string')
                ? new Function("event", props.onFocus)
                : props.onFocus;
        }
        if (props.onKeyDown) {
            domNode.onkeydown = (typeof props.onKeyDown == 'string')
                ? new Function("event", props.onKeyDown)
                : props.onKeyDown;
        }
        if (props.onKeyPress) {
            domNode.onkeypress = (typeof props.onKeyPress == 'string')
                ? new Function("event", props.onKeyPress)
                : props.onKeyPress;
        }
        if (props.onKeyUp) {
            domNode.onkeyup = (typeof props.onKeyUp == 'string')
                ? new Function("event", props.onKeyUp)
                : props.onKeyUp;
        }
        if (props.onMouseDown) {
            domNode.onmousedown = (typeof props.onMouseDown == 'string')
                ? new Function("event", props.onMouseDown)
                : props.onMouseDown;
        }
        if (props.onMouseOut) {
            domNode.onmouseout = (typeof props.onMouseOut == 'string')
                ? new Function("event", props.onMouseOut)
                : props.onMouseOut;
        }
        if (props.onMouseOver) {
            domNode.onmouseover = (typeof props.onMouseOver == 'string')
                ? new Function("event", props.onMouseOver)
                : props.onMouseOver;
        }
        if (props.onMouseUp) {
            domNode.onmouseup = (typeof props.onMouseUp == 'string')
                ? new Function("event", props.onMouseUp)
                : props.onMouseUp;
        }
        if (props.onMouseMove) {
            domNode.onmousemove = (typeof props.onMouseMove == 'string')
                ? new Function("event", props.onMouseMove)
                : props.onMouseMove;
        }
        if (props.onSelect) {
            domNode.onselect = (typeof props.onSelect == 'string')
                ? new Function("event", props.onSelect)
                : props.onSelect;
        }
        return true;
    }
}

//-->
