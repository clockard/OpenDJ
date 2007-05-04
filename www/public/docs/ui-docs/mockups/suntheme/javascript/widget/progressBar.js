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

dojo.provide("webui.suntheme.widget.progressBar");

dojo.require("dojo.widget.*");
dojo.require("webui.suntheme.*");
dojo.require("webui.suntheme.widget.*");

/**
 * This function will be invoked when creating a Dojo widget. Please see
 * webui.suntheme.widget.progressBar.setProps for a list of supported
 * properties.
 *
 * Note: This is considered a private API, do not use.
 */
webui.suntheme.widget.progressBar = function() {
    // Set defaults.
    this.percentChar = "%";
    this.progress = 0;
    this.type = webui.suntheme.widget.props.progressBar.determinate;
    this.widgetType = "progressBar";

    // Register widget.
    dojo.widget.Widget.call(this);

    this.fillInTemplate = function() {
        // Set ids.
        if (this.id) {
            this.barContainer.id = this.id + "_barContainer";
            this.bottomControlsContainer.id = this.id + "_bottomControlsContainer";
            this.bottomTextContainer.id = this.id + "_bottomTextContainer"; 
            this.failedStateContainer.id = this.id + "_failedStateContainer";
            this.failedLabelContainer.id = this.id + "_failedLabelContainer";
            this.hiddenFieldNode.id = this.id + "_" + "controlType";
            this.hiddenFieldNode.name = this.hiddenFieldNode.id;
            this.innerBarContainer.id = this.id + "_innerBarContainer";
            this.innerBarOverlayContainer.id = this.id + "_innerBarOverlayContainer";
            this.logContainer.id = this.id + "_logContainer";
            this.rightControlsContainer.id = this.id + "_rightControlsContainer";
            this.topTextContainer.id = this.id + "_topTextContainer"; 
        }

        // Set public functions
        this.domNode.cancel = function() { return dojo.widget.byId(this.id).cancel(); }
        this.domNode.getProps = function() { return dojo.widget.byId(this.id).getProps(); }
        this.domNode.isBottomControlVisible = function() { return dojo.widget.byId(this.id).isBottomControlVisible(); }
        this.domNode.isFailedStateMessageVisible = function() { return dojo.widget.byId(this.id).isFailedStateMessageVisible(); }
        this.domNode.isLogMsgVisible = function() { return dojo.widget.byId(this.id).isLogMsgVisible(); }
        this.domNode.isOperationTextVisible = function() { return dojo.widget.byId(this.id).isOperationTextVisible(); }
        this.domNode.isProgressBarContainerVisible = function() { return dojo.widget.byId(this.id).isProgressBarContainerVisible(); }
        this.domNode.isProgressBarVisible = function() { return dojo.widget.byId(this.id).isProgressBarVisible(); }
        this.domNode.isRightControlVisible = function() { return dojo.widget.byId(this.id).isRightControlVisible(); }
        this.domNode.isStatusTextVisible = function() { return dojo.widget.byId(this.id).isStatusTextVisible(); }
        this.domNode.pause = function() { return dojo.widget.byId(this.id).pause(); }
        this.domNode.refresh = function(execute) { return dojo.widget.byId(this.id).refresh(execute); }
        this.domNode.resume = function() { return dojo.widget.byId(this.id).resume(); }
        this.domNode.stop = function() { return dojo.widget.byId(this.id).stop(); }
        this.domNode.setOnCancel = function(func) { return dojo.widget.byId(this.id).setOnCancel(func); }
        this.domNode.setOnComplete = function(func) { return dojo.widget.byId(this.id).setOnComplete(func); }
        this.domNode.setOnFail = function(func) { return dojo.widget.byId(this.id).setOnFail(func); }
        this.domNode.setBottomControlVisible = function(show) { return dojo.widget.byId(this.id).setBottomControlVisible(show); }
        this.domNode.setFailedStateMessageVisible = function(show) { return dojo.widget.byId(this.id).setFailedStateMessageVisible(show); }
        this.domNode.setLogMsgVisible = function(show) { return dojo.widget.byId(this.id).setLogMsgVisible(show); }
        this.domNode.setOperationTextVisible = function(show) { return dojo.widget.byId(this.id).setOperationTextVisible(show); }
        this.domNode.setProgressBarContainerVisible = function(show) { return dojo.widget.byId(this.id).setProgressBarContainerVisible(show); }
        this.domNode.setProgressBarVisible = function(show) { return dojo.widget.byId(this.id).setProgressBarVisible(show); }
        this.domNode.setProps = function(props) { return dojo.widget.byId(this.id).setProps(props); }
        this.domNode.setRightControlVisible = function(show) { return dojo.widget.byId(this.id).setRightControlVisible(show); }
        this.domNode.setStatusTextVisible = function(show) { return dojo.widget.byId(this.id).setStatusTextVisible(show); }

        // Set private functions.
        this.cancel = webui.suntheme.widget.progressBar.cancel;
        this.getProps =  webui.suntheme.widget.progressBar.getProps;
        this.isBottomControlVisible = webui.suntheme.widget.progressBar.isBottomControlVisible;
        this.isFailedStateMessageVisible = webui.suntheme.widget.progressBar.isFailedStateMessageVisible;
        this.isLogMsgVisible = webui.suntheme.widget.progressBar.isLogMsgVisible;
        this.isOperationTextVisible = webui.suntheme.widget.progressBar.isOperationTextVisible;
        this.isProgressBarContainerVisible = webui.suntheme.widget.progressBar.isProgressBarContainerVisible;
        this.isProgressBarVisible = webui.suntheme.widget.progressBar.isProgressBarVisible;
        this.isRightControlVisible = webui.suntheme.widget.progressBar.isRightControlVisible;
        this.isStatusTextVisible = webui.suntheme.widget.progressBar.isStatusTextVisible;
        this.pause = webui.suntheme.widget.progressBar.pause;
        this.refresh = webui.suntheme.widget.progressBar.refresh.processEvent;
        this.resume = webui.suntheme.widget.progressBar.resume;
        this.stop = webui.suntheme.widget.progressBar.stop;
        this.setOnCancel = webui.suntheme.widget.progressBar.setOnCancel;
        this.setOnComplete = webui.suntheme.widget.progressBar.setOnComplete;
        this.setOnFail = webui.suntheme.widget.progressBar.setOnFail;
        this.setBottomControlVisible = webui.suntheme.widget.progressBar.setBottomControlVisible;
        this.setFailedStateMessageVisible = webui.suntheme.widget.progressBar.setFailedStateMessageVisible;
        this.setLogMsgVisible = webui.suntheme.widget.progressBar.setLogMsgVisible;
        this.setOperationTextVisible = webui.suntheme.widget.progressBar.setOperationTextVisible;
        this.setProgress = webui.suntheme.widget.progressBar.setProgress;
        this.setProgressBarContainerVisible = webui.suntheme.widget.progressBar.setProgressBarContainerVisible;
        this.setProgressBarVisible = webui.suntheme.widget.progressBar.setProgressBarVisible;
        this.setProps =  webui.suntheme.widget.progressBar.setProps;
        this.sleep = webui.suntheme.widget.progressBar.sleep;
        this.setRightControlVisible = webui.suntheme.widget.progressBar.setRightControlVisible;
        this.setStatusTextVisible = webui.suntheme.widget.progressBar.setStatusTextVisible;
        this.updateProgress = webui.suntheme.widget.progressBar.progress.processEvent;

        // Initialize properties.
        webui.suntheme.widget.common.initProps(this);

        // Obtain progress.
        this.updateProgress();
        return true;
   };

}

/**
 * This function handles cancel button event.
 */
webui.suntheme.widget.progressBar.cancel = function() {
    clearTimeout(this.timeoutId);

    this.hiddenFieldNode.value = webui.suntheme.widget.props.progressBar.canceled;
    if (this.type == webui.suntheme.widget.props.progressBar.determinate) {
        this.innerBarContainer.style.width = "0%";
    }
    this.updateProgress();
}

/**
 * This function is used to get widget properties. Please see
 * webui.suntheme.widget.progressBar.setProps for a list of supported
 * properties.
 */
webui.suntheme.widget.progressBar.getProps = function() {
    var props = {};

    // Set properties.
    if (this.barHeight) { props.barHeight = this.barHeight; }
    if (this.barWidth) { props.barWidth = this.barWidth; }
    if (this.bottomText) { props.bottomText = this.bottomText; }
    if (this.busyImage != null) { props.busyImage = this.busyImage; }
    if (this.failedStateText != null) { props.failedStateText = this.failedStateText; }
    if (this.id) { props.id = this.id; }
    if (this.log != null) { props.log = this.log; }
    if (this.logId) { props.logId = this.logId; }
    if (this.logMessage) { props.logMessage = this.logMessage; }
    if (this.overlayAnimation != null) { props.overlayAnimation = this.overlayAnimation; }
    if (this.percentChar) { props.percentChar = this.percentChar; }
    if (this.progress != null) { props.progress = this.progress; }
    if (this.progressImageUrl) { props.progressImageUrl = this.progressImageUrl; }
    if (this.progressControlBottom != null) { props.progressControlBottom = this.progressControlBottom; }
    if (this.progressControlRight != null) { props.progressControlRight = this.progressControlRight; }
    if (this.refreshRate) { props.refreshRate = this.refreshRate; }
    if (this.taskState != null) { props.taskState = this.taskState; }
    if (this.toolTip) { props.toolTip = this.toolTip; }
    if (this.topText) { props.topText = this.topText; }
    if (this.type) { props.type = this.type; }

    // Add DOM node properties.
    Object.extend(props, webui.suntheme.widget.common.getCommonProps(this));
    Object.extend(props, webui.suntheme.widget.common.getCoreProps(this));

    return props;
}

/**
 * This method displays the Bottom Controls if it was hidden.
 *
 * @return true if successful; otherwise, false.
 */
webui.suntheme.widget.progressBar.isBottomControlVisible = function() {
    return webui.suntheme.common.isVisibleElement(this.bottomControlsContainer);
}

/**
 * This method displays the failed state message and icon if it was hidden.
 *
 * @return true if successful; otherwise, false.
 */
webui.suntheme.widget.progressBar.isFailedStateMessageVisible = function() {
    return webui.suntheme.common.isVisibleElement(this.failedStateContainer);
}

/**
 * This method displays the log message if it was hidden.
 *
 * @return true if successful; otherwise, false.
 */
webui.suntheme.widget.progressBar.isLogMsgVisible = function() {
    return webui.suntheme.common.isVisibleElement(this.logContainer);
}

/**
 * This method displays the operation text if it was hidden.
 *
 * @return true if successful; otherwise, false.
 */
webui.suntheme.widget.progressBar.isOperationTextVisible = function() {
    return webui.suntheme.common.isVisibleElement(this.topTextContainer);
}

/**
 * This method displays the ProgressBar Container if it was hidden.
 *
 * @return true if successful; otherwise, false.
 */
webui.suntheme.widget.progressBar.isProgressBarContainerVisible = function() {
    return webui.suntheme.common.isVisibleElement(this.barContainer);
}

/**
 * This method displays the ProgressBar if it was hidden.
 *
 * @return true if successful; otherwise, false.
 */
webui.suntheme.widget.progressBar.isProgressBarVisible = function() {
    return webui.suntheme.common.isVisibleElement(this); 
}

/**
 * This method displays the Right Controls if it was hidden.
 *
 * @return true if successful; otherwise, false.
 */
webui.suntheme.widget.progressBar.isRightControlVisible = function() {
    return webui.suntheme.common.isVisibleElement(this.rightControlsContainer);
}

/**
 * This method displays the status text if it was hidden.
 *
 * @return true if successful; otherwise, false.
 */
webui.suntheme.widget.progressBar.isStatusTextVisible = function() {
    return webui.suntheme.common.isVisibleElement(this.bottomTextContainer);
}

/**
 * This function handles pause button event.
 */
webui.suntheme.widget.progressBar.pause = function() {
    clearTimeout(this.timeoutId);

    this.hiddenFieldNode.value = webui.suntheme.widget.props.progressBar.paused;
    if (this.type == webui.suntheme.widget.props.progressBar.indeterminate) {
        this.innerBarContainer.className =
            webui.suntheme.widget.props.progressBar.indeterminatePausedClassName;
    }
    this.updateProgress();
}

/**
 * This closure is used to publish progress events.
 */
webui.suntheme.widget.progressBar.progress = {
    /**
     * Event topics for custom AJAX implementations to listen for.
     */
    beginEventTopic: "webui_suntheme_widget_progressBar_progress_begin",
    endEventTopic: "webui_suntheme_widget_progressBar_progress_end",

    /**
     * Process progress event.
     */
    processEvent: function() {
        // Publish event.
        if (this.refreshRate > 0) {
            webui.suntheme.widget.progressBar.progress.publishBeginEvent({
                id: this.id
            });
        }

        // Create a call back function to periodically publish progress events.
        this.timeoutId = setTimeout(
            webui.suntheme.widget.progressBar.progress.createCallback(this.id),
            this.refreshRate);
    },

    /**
     * Helper function to to periodically obtain progress.
     *
     * @param id The client id used to invoke the callback.
     */
    createCallback: function(id) {
        if (id != null) {
            // New literals are created every time this function
            // is called, and it's saved by closure magic.
            return function() {
                var widget = dojo.widget.byId(id);
                if (widget == null) {
                    return null;
                } else {
                    widget.updateProgress();
                }
            };
        }
    },

    /**
     * Publish an event for custom AJAX implementations to listen for.
     */
    publishBeginEvent: function(id) {
        dojo.event.topic.publish(webui.suntheme.widget.progressBar.progress.beginEventTopic, id);
        return true;
    },
    
    /**
     * Publish an event for custom AJAX implementations to listen for.
     *
     * @param props Key-Value pairs of properties of the widget.
     */
    publishEndEvent: function(props) {
        dojo.event.topic.publish(webui.suntheme.widget.progressBar.progress.endEventTopic, props);
        return true;
    }
}

/**
 * This closure is used to process refresh events.
 */
webui.suntheme.widget.progressBar.refresh = {
    /**
     * Event topics for custom AJAX implementations to listen for.
     */
    beginEventTopic: "webui_suntheme_widget_progressBar_refresh_begin",
    endEventTopic: "webui_suntheme_widget_progressBar_refresh_end",
 
    /**
     * Process refresh event.
     *
     * @param execute Comma separated string containing a list of client ids 
     * against which the execute portion of the request processing lifecycle
     * must be run.
     */
    processEvent: function(execute) {
        // Publish event.
        webui.suntheme.widget.progressBar.refresh.publishBeginEvent({
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
        dojo.event.topic.publish(webui.suntheme.widget.progressBar.refresh.beginEventTopic, props);
        return true;
    },

    /**
     * Publish an event for custom AJAX implementations to listen for.
     *
     * @param props Key-Value pairs of properties of the widget.
     */
    publishEndEvent: function(props) {
        dojo.event.topic.publish(webui.suntheme.widget.progressBar.refresh.endEventTopic, props);
        return true;
    }
}

/**
 * This function handles resume button event.
 */
webui.suntheme.widget.progressBar.resume = function() {
    clearTimeout(this.timeoutId);

    this.hiddenFieldNode.value = webui.suntheme.widget.props.progressBar.resumed;
    if (this.type == webui.suntheme.widget.props.progressBar.indeterminate) {
        this.innerBarContainer.className =
            webui.suntheme.widget.props.progressBar.indeterminateClassName;
    }
    this.updateProgress();
}

/**
 * This method hides the Bottom Control.
 *
 * @param show true to show the element, false to hide the element.
 * @return true if successful; otherwise, false.
 */
webui.suntheme.widget.progressBar.setBottomControlVisible = function(show) {
    if (show == null) {
        return false;
    }
    webui.suntheme.common.setVisibleElement(this.bottomControlsContainer, show);
    return true;
}

/**
 * This method hides the failed state message and icon area.
 *
 * @param show true to show the element, false to hide the element.
 * @return true if successful; otherwise, false.
 */
webui.suntheme.widget.progressBar.setFailedStateMessageVisible = function(show) {
    if (show == null) {
        return false;
    }
    webui.suntheme.common.setVisibleElement(this.failedStateContainer, show);
    return true;
}

/**
 * This method hides the log message area.
 *
 * @param show true to show the element, false to hide the element.
 * @return true if successful; otherwise, false.
 */
webui.suntheme.widget.progressBar.setLogMsgVisible = function(show) {
    if (show == null) {
        return false;
    }
    webui.suntheme.common.setVisibleElement(this.logContainer, show);
    return true;
}

/**
 * This function invokes developer define function for cancel event.
 * 
 * @param func The JavaScript function.
 */
webui.suntheme.widget.progressBar.setOnCancel = function(func) {
    if (func) {
        this.funcCanceled = func;
    }
}

/**
 * This function invokes developer define function for complete event.
 * 
 * @param func The JavaScript function.
 */
webui.suntheme.widget.progressBar.setOnComplete = function(func) {
    if (func) {
        this.funcComplete = func;
    }
}

/**
 * This function invokes developer define function for failed event.
 * 
 * @param func The JavaScript function.
 */
webui.suntheme.widget.progressBar.setOnFail = function(func) {
    if (func) {
        this.funcFailed = func;
    }
}

/**
 * This method hides the operation text.
 *
 * @param show true to show the element, false to hide the element.
 * @return true if successful; otherwise, false.
 */
webui.suntheme.widget.progressBar.setOperationTextVisible = function(show) {
    if (show == null) {
        return false;
    }
    webui.suntheme.common.setVisibleElement(this.topTextContainer, show);
    return true;
}

/**
 * This function is used to set progress with the following Object
 * literals.
 *
 * <ul>
 *  <li>failedStateText</li>
 *  <li>logMessage</li>
 *  <li>progress</li>
 *  <li>status</li>
 *  <li>taskState</li>
 *  <li>topText</li>
 *  <li>type</li>
 * </ul>
 *
 * @param props Key-Value pairs of properties.
 */
webui.suntheme.widget.progressBar.setProgress = function(props) {
    if (props == null) {
        return false;
    }

    // Adjust max value.
    if (props.progress > 99 
            || props.taskState == webui.suntheme.widget.props.progressBar.completed) {
        props.progress = 100;
    }

    // Save properties for later updates.
    webui.suntheme.widget.common.extend(this, props);    

    // Set status.
    if (props.status) {
        webui.suntheme.widget.common.addFragment(this.bottomTextContainer, props.status);
    }

    // If top text doesnt get change, dont update.
    if (props.topText) {
        if (props.topText != this.topText) {
            webui.suntheme.widget.common.addFragment(this.topTextContainer, props.topText);
        }
    }

    // Update log messages.
    if (this.type == webui.suntheme.widget.props.progressBar.determinate) { 
        this.innerBarContainer.style.width = props.progress + '%';

        if (props.logMessage) {
            var field = webui.suntheme.field.getInputElement(this.logId)
            if (field != null) {
                field.value = (field.value)
                   ? field.value + props.logMessage + "\n"
                   : props.logMessage + "\n";
            }
        }

        // Add overlay text.
        if (this.overlayAnimation == true) {
            webui.suntheme.widget.common.addFragment(this.innerBarOverlayContainer,
                props.progress + "%");
        }
    } 

    // Failed state.
    if (props.taskState == webui.suntheme.widget.props.progressBar.failed) {
        clearTimeout(this.timeoutId);
        this.sleep(1000);
        this.setProgressBarContainerVisible(false);
        this.setBottomControlVisible(false);
        this.setRightControlVisible(false);
        this.setLogMsgVisible(false);

        if (props.failedStateText != null) {
            var text = props.failedStateText + " " + props.progress + this.percentChar;
            webui.suntheme.widget.common.addFragment(this.failedLabelContainer, text);
            webui.suntheme.common.setVisibleElement(this.failedLabelContainer, true);
            webui.suntheme.common.setVisibleElement(this.failedStateContainer, true);
        }
        if (this.funcFailed != null) {
            (this.funcFailed)();
        }
        return;
    }

    // Cancel state.
    if (props.taskState == webui.suntheme.widget.props.progressBar.canceled) {
        clearTimeout(this.timeoutId);
        this.sleep(1000);
        this.setOperationTextVisible(false);
        this.setStatusTextVisible(false);
        this.setProgressBarContainerVisible(false);
        this.setBottomControlVisible(false);
        this.setRightControlVisible(false);
        this.setLogMsgVisible(false);

        if (this.type == webui.suntheme.widget.props.progressBar.determinate) {
            this.innerBarContainer.style.width = "0%";
        }
        if (this.funcCanceled != null) {
           (this.funcCanceled)(); 
        }
        return;    
    }

    // paused state
    if (props.taskState == webui.suntheme.widget.props.progressBar.paused) {
        clearTimeout(this.timeoutId);
        return;
    }

    // stopped state
    if (props.taskState == webui.suntheme.widget.props.progressBar.stopped) {
        clearTimeout(this.timeoutId);
        return;
    }

    if (props.progress > 99 
            || props.taskState == webui.suntheme.widget.props.progressBar.completed) {
        clearTimeout(this.timeoutId);
        if (this.type == webui.suntheme.widget.props.progressBar.indeterminate) {
            this.innerBarContainer.className =
                webui.suntheme.widget.props.progressBar.indeterminatePausedClassName;
        }
        if (this.type == webui.suntheme.widget.props.progressBar.busy) {
            this.setProgressBarContainerVisible(false);
        }
        if (this.funcComplete != null) {
           (this.funcComplete)(); 
        }
    }

    // Set progress for A11Y.
    if (props.progress) {
        if (this.bottomTextContainer.setAttributeNS) {
            this.bottomTextContainer.setAttributeNS("http://www.w3.org/2005/07/aaa",
                "valuenow", props.progress);
        }
    }
}

/**
 * This method hides the ProgressBar Container.
 *
 * @param show true to show the element, false to hide the element.
 * @return true if successful; otherwise, false.
 */
webui.suntheme.widget.progressBar.setProgressBarContainerVisible = function(show) {
    if (show == null) {
        return false;
    }

    // FIX: Remove "display:block" from barContainer class and
    // use webui.suntheme.common.setVisibleElement.

    if (show == false) {
        this.barContainer.style.display = "none";
    } else {
        this.barContainer.style.display = '';
    }
    return true; 
}

/**
 * This method hides the ProgressBar.
 *
 * @param show true to show the element, false to hide the element.
 * @return true if successful; otherwise, false.
 */
webui.suntheme.widget.progressBar.setProgressBarVisible = function(show) {
    if (show == null) {
        return false;
    }
    webui.suntheme.common.setVisibleElement(this, show);
    return true; 
}

/**
 * This function is used to set widget properties with the
 * following Object literals.
 *
 * <ul>
 *  <li>barHeight</li>
 *  <li>barWidth</li>
 *  <li>bottomText</li>
 *  <li>busyImage</li>
 *  <li>failedStateText</li>
 *  <li>id</li>
 *  <li>log</li>
 *  <li>logId</li>
 *  <li>logMessage</li>
 *  <li>overlayAnimation</li>
 *  <li>percentChar</li>
 *  <li>progress</li>
 *  <li>progressImageUrl</li>
 *  <li>progressControlBottom</li>
 *  <li>progressControlRight</li>
 *  <li>refreshRate</li>
 *  <li>taskState</li>
 *  <li>toolTip</li>
 *  <li>topText</li>
 *  <li>type</li>
 *  <li>visible</li>
 * </ul>
 *
 * @param props Key-Value pairs of properties.
 */
webui.suntheme.widget.progressBar.setProps = function(props) {
    // Save properties for later updates.
    if (props != null) {
        webui.suntheme.widget.common.extend(this, props);
    } else {
        props = this.getProps(); // Widget is being initialized.
    }

    // Set DOM node properties.
    webui.suntheme.widget.common.setCoreProps(this.domNode, props);
    webui.suntheme.widget.common.setCommonProps(this.domNode, props);

    // Set tool tip.
    if (props.toolTip) {
        this.barContainer.title = props.toolTip;
    }

    // Add top text.
    if (props.topText) {
        webui.suntheme.widget.common.addFragment(this.topTextContainer, props.topText); 
        webui.suntheme.common.setVisibleElement(this.topTextContainer, true);
    }

    // Add bottom text.
    if (props.bottomText) {
        webui.suntheme.widget.common.addFragment(this.bottomTextContainer, props.bottomText);
        webui.suntheme.common.setVisibleElement(this.bottomTextContainer, true);
    }

    if (props.type == webui.suntheme.widget.props.progressBar.determinate 
            || props.type == webui.suntheme.widget.props.progressBar.indeterminate) {
        // Set style class.
        this.barContainer.className =
            webui.suntheme.widget.props.progressBar.barContainerClassName;

        // Set height.
        if (props.barHeight != null && props.barHeight > 0) {
            this.barContainer.style.height = props.barHeight + "px;"; 
            this.innerBarContainer.style.height = props.barHeight + "px;";
        }

        // Set width.
        if (props.barWidth != null && props.barWidth > 0) {
            this.barContainer.style.width = props.barWidth + "px;";
        }

        // Add right controls.
        if (props.progressControlRight != null) {
            webui.suntheme.widget.common.addFragment(this.rightControlsContainer,
                props.progressControlRight);
            webui.suntheme.common.setVisibleElement(this.rightControlsContainer, true);
        }

        // Add bottom controls.
        if (props.progressControlBottom != null) {
            webui.suntheme.widget.common.addFragment(this.bottomControlsContainer,
                props.progressControlBottom);
            webui.suntheme.common.setVisibleElement(this.bottomControlsContainer, true);
        }
    }

    if (props.type == webui.suntheme.widget.props.progressBar.determinate) {
        // Set style class.
        this.innerBarContainer.className =
            webui.suntheme.widget.props.progressBar.determinateClassName;

        // Set width.
        this.innerBarContainer.style.width = this.progress + '%';

        // Add overlay.
        if (props.overlayAnimation == true) {
            webui.suntheme.widget.common.addFragment(this.innerBarOverlayContainer,
                this.progress + "%");
            webui.suntheme.common.setVisibleElement(this.innerBarOverlayContainer, true);
        }

        // Add log.
        if (props.log != null && props.overlayAnimation == false) { 
            webui.suntheme.widget.common.addFragment(this.logContainer, props.log);
            webui.suntheme.common.setVisibleElement(this.logContainer, true);
        }  
    } else if (props.type == webui.suntheme.widget.props.progressBar.indeterminate) {
        // Set style class.
        this.barContainer.className = 
            webui.suntheme.widget.props.progressBar.barContainerClassName;
        this.innerBarContainer.className = 
            webui.suntheme.widget.props.progressBar.indeterminateClassName;
    } else if (props.type == webui.suntheme.widget.props.progressBar.busy) {
        // Add busy image.
        if (props.busyImage) {
            webui.suntheme.widget.common.addFragment(this.busyImageContainer, props.busyImage);
            webui.suntheme.common.setVisibleElement(this.busyImageContainer, true);
        }
    }

    // Set developer specified image.
    if (props.progressImageUrl != null ) {
        this.innerBarContainer.style.backgroundImage = 'url(' + props.progressImageUrl + ')';
    }

    // Set A11Y properties.
    if (props.progress != null) {
        if (this.bottomTextContainer.setAttributeNS) {
            this.bottomTextContainer.setAttributeNS(
                "http://www.w3.org/2005/07/aaa", "valuenow", this.progress);
        }
    }
    return true;
}

/**
 * This method hides the Right Control.
 *
 * @param show true to show the element, false to hide the element.
 * @return true if successful; otherwise, false.
 */
webui.suntheme.widget.progressBar.setRightControlVisible = function(show) {
    if (show == null) {
        return false;
    }
    webui.suntheme.common.setVisibleElement(this.rightControlsContainer, show);
    return true;
}

/**
 * This method hides the status text.
 *
 * @param show true to show the element, false to hide the element.
 * @return true if successful; otherwise, false.
 */
webui.suntheme.widget.progressBar.setStatusTextVisible = function(show) {
    if (show == null) {
        return false;
    }
    webui.suntheme.common.setVisibleElement(this.bottomTextContainer, show);
    return true;
}

/**
 * This function sleeps for specified milli seconds.
 */
webui.suntheme.widget.progressBar.sleep = function(delay) {
    var start = new Date();
    var exitTime = start.getTime() + delay;

    while(true) {
        start = new Date();
        if (start.getTime() > exitTime) {
            return;
        }
    }
}

/**
 * This function handles stop button event.
 */
webui.suntheme.widget.progressBar.stop = function() {
    clearTimeout(this.timeoutId);

    this.hiddenFieldNode.value = webui.suntheme.widget.props.progressBar.stopped;
    if (this.type == webui.suntheme.widget.props.progressBar.indeterminate) {
        this.innerBarIdContainer.className =
            webui.suntheme.widget.props.progressBar.indeterminatePaused;
    }
    this.updateProgress();
}

dojo.inherits(webui.suntheme.widget.progressBar, dojo.widget.HtmlWidget);

//-->
