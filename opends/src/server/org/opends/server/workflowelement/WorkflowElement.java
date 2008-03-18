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
 *      Copyright 2008 Sun Microsystems, Inc.
 */
package org.opends.server.workflowelement;

import static org.opends.server.util.Validator.ensureNotNull;
import static org.opends.messages.ConfigMessages.*;

import java.util.List;
import java.util.TreeMap;

import org.opends.messages.Message;
import org.opends.server.admin.std.server.WorkflowElementCfg;
import org.opends.server.config.ConfigException;
import org.opends.server.core.SearchOperationBasis;
import org.opends.server.types.Control;
import org.opends.server.types.DN;
import org.opends.server.types.Entry;
import org.opends.server.types.Operation;
import org.opends.server.types.SearchResultReference;
import org.opends.server.types.CanceledOperationException;


/**
 * This class defines the super class for all the workflow elements. A workflow
 * element is a task in a workflow. A workflow element can wrap a physical
 * repository such as a local backend, a remote LDAP server or a local ldif
 * file. A workflow element can also be used to route operations. This is the
 * case for load balancing and distribution. And workflow element can be used
 * in a virtual environment to transform data (DN and attribute renaming,
 * attribute value renaming...).
 *
 * @param  <T>  The type of configuration handled by this workflow elelemnt.
 */
public abstract class WorkflowElement
       <T extends WorkflowElementCfg>
{
  // Indicates whether the workflow element encapsulates a private local
  // backend.
  private boolean isPrivate = false;


  // The workflow element identifier.
  private String workflowElementID = null;


  // The set of workflow elements registered with the server.
  // The workflow element identifier is used as a key in the map.
  private static TreeMap<String, WorkflowElement> registeredWorkflowElements =
    new TreeMap<String, WorkflowElement>();


  // A lock to protect access to the registered workflow elements.
  private static Object registeredWorkflowElementsLock = new Object();


  // The original operation basis which has invoked the workflow.
  // This original operation basis is only useful for the search
  // operation, for the returned entry and returned reference to be
  // processed before they are sent back to the client application.
  private Operation originalOperationBasis = null;


  // The parent of the workflow element (null if the workflow element is
  // the root of the processing tree).
  private WorkflowElement<?> parent = null;


  /**
   * Creates a new instance of the workflow element.
   */
  public WorkflowElement()
  {
    // There is nothing to do in the constructor.
  }


  /**
   * Initializes the instance of the workflow element.
   *
   * @param workflowElementID  the workflow element identifier as defined
   *                           in the configuration.
   */
  public void initialize(String workflowElementID)
  {
    this.workflowElementID = workflowElementID;
  }


  /**
   * Set the original operation basis which has invoked the workflow.
   *
   * @param operation  the operation basis which has invoked the workflow
   */
  protected void setOriginalOperationBasis(Operation operation)
  {
    this.originalOperationBasis = operation;
  }


  /**
   * Set the parent of the current workflow element.
   *
   * @param parent  the parent of the workflow element
   */
  protected void setParent(WorkflowElement<?> parent)
  {
    this.parent = parent;
  }


  /**
   * Indicates whether the provided configuration is acceptable for
   * this workflow elelement.
   *
   * @param  configuration        The workflow element configuration for
   *                              which to make the determination.
   * @param  unacceptableReasons  A list that may be used to hold the
   *                              reasons that the provided
   *                              configuration is not acceptable.
   *
   * @return  {@code true} if the provided configuration is acceptable
   *          for this workflow element, or {@code false} if not.
   */
  public boolean isConfigurationAcceptable(
      WorkflowElementCfg configuration,
      List<String> unacceptableReasons)
  {
    // This default implementation does not perform any special
    // validation.  It should be overridden by workflow element
    // implementations that wish to perform more detailed validation.
    return true;
  }


  /**
   * Performs any finalization that might be required when this
   * workflow element is unloaded.  No action is taken in the default
   * implementation.
   */
  public void finalizeWorkflowElement()
  {
    // No action is required by default.
  }


  /**
   * Executes the workflow element for an operation.
   *
   * @param operation the operation to execute
   *
   * @throws CanceledOperationException if this operation should be
   * cancelled
   */
  public abstract void execute(Operation operation)
      throws CanceledOperationException;


  /**
   * Indicates whether the workflow element encapsulates a private
   * local backend.
   *
   * @return <code>true</code> if the workflow element encapsulates a private
   *         local backend, <code>false</code> otherwise
   */
  public boolean isPrivate()
  {
    return isPrivate;
  }


  /**
   * Specifies whether the workflow element encapsulates a private local
   * backend.
   *
   * @param  isPrivate  Indicates whether the workflow element encapsulates a
   *                    private local backend.
   */
  protected void setPrivate(boolean isPrivate)
  {
    this.isPrivate = isPrivate;
  }


  /**
   * Provides the workflow element identifier.
   *
   * @return the worflow element identifier
   */
  public String getWorkflowElementID()
  {
    return workflowElementID;
  }


  /**
   * Registers the workflow element (this) with the server.
   *
   * @throws  ConfigException  If the workflow element ID for the provided
   *                           workflow element conflicts with the workflow
   *                           element ID of an existing workflow element.
   */
  public void register()
      throws ConfigException
  {
    ensureNotNull(workflowElementID);

    synchronized (registeredWorkflowElementsLock)
    {
      // the workflow element must not be already registered
      if (registeredWorkflowElements.containsKey(workflowElementID))
      {
        Message message = ERR_CONFIG_WORKFLOW_ELEMENT_ALREADY_REGISTERED.get(
            workflowElementID);
        throw new ConfigException(message);
      }

      TreeMap<String, WorkflowElement> newWorkflowElements =
        new TreeMap<String, WorkflowElement>(registeredWorkflowElements);
      newWorkflowElements.put(workflowElementID, this);
      registeredWorkflowElements = newWorkflowElements;
    }
  }


  /**
   * Deregisters the workflow element (this) with the server.
   */
  public void deregister()
  {
    ensureNotNull(workflowElementID);

    synchronized (registeredWorkflowElementsLock)
    {
      TreeMap<String, WorkflowElement> newWorkflowElements =
        new TreeMap<String, WorkflowElement>(registeredWorkflowElements);
      newWorkflowElements.remove(workflowElementID);
      registeredWorkflowElements = newWorkflowElements;
    }
  }


  /**
   * Gets a workflow element that was registered with the server.
   *
   * @param workflowElementID  the ID of the workflow element to get
   * @return the requested workflow element
   */
  public static WorkflowElement getWorkflowElement(
      String workflowElementID)
  {
    return registeredWorkflowElements.get(workflowElementID);
  }


  /**
   * Resets all the registered workflows.
   */
  public static void resetConfig()
  {
    synchronized (registeredWorkflowElementsLock)
    {
      registeredWorkflowElements = new TreeMap<String, WorkflowElement>();
    }
  }


  /**
   * Used as a callback for workflow elements to indicate that the provided
   * entry matches the search criteria and that additional processing should
   * be performed to potentially send it back to the client.
   *
   * @param  entry     The entry that matches the search criteria and should be
   *                   sent to the client.
   * @param  controls  The set of controls to include with the entry (may be
   *                   <CODE>null</CODE> if none are needed).
   *
   * @return  <CODE>true</CODE> if the caller should continue processing the
   *          search request and sending additional entries and references, or
   *          <CODE>false</CODE> if not for some reason (e.g., the size limit
   *          has been reached or the search has been abandoned).
   */
  public boolean returnEntry(
      Entry entry,
      List<Control> controls)
  {
    boolean result;

    // If the workflow element has a parent then send the entry
    // to the parent, otherwise send the entry to the operation
    // basis. The operation basis will be in charge of sending
    // the entry to the client application.
    if (parent == null)
    {
      SearchOperationBasis searchOperationBasis =
        (SearchOperationBasis) originalOperationBasis;
      result = searchOperationBasis.returnEntry(entry, controls);
    }
    else
    {
      result = parent.returnEntry(entry, controls);
    }

    return result;
  }


  /**
   * Used as a callback for workflow elements to indicate that the provided
   * search reference was encountered during processing and that additional
   * processing should be performed to potentially send it back to the client.
   *
   * @param  reference  The search reference to send to the client.
   * @param  dn         The DN related to the specified search reference.
   *
   * @return  <CODE>true</CODE> if the caller should continue processing the
   *          search request and sending additional entries and references , or
   *          <CODE>false</CODE> if not for some reason (e.g., the size limit
   *          has been reached or the search has been abandoned).
   */
  public boolean returnReference(
      DN dn,
      SearchResultReference reference)
  {
    boolean result;

    // If the workflow element has a parent then send the reference
    // to the parent, otherwise send the reference to the operation
    // basis. The operation basis will be in charge of sending
    // the reference to the client application.
    if (parent == null)
    {
      SearchOperationBasis searchOperationBasis =
        (SearchOperationBasis) originalOperationBasis;
      result = searchOperationBasis.returnReference(dn, reference);
    }
    else
    {
      result = parent.returnReference(dn, reference);
    }

    return result;
  }

}

