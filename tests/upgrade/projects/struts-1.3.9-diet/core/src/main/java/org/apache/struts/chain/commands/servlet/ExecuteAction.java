/*
 * $Id: ExecuteAction.java 471754 2006-11-06 14:55:09Z husted $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.struts.chain.commands.servlet;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.chain.commands.AbstractExecuteAction;
import org.apache.struts.chain.contexts.ActionContext;
import org.apache.struts.chain.contexts.ServletActionContext;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.config.ForwardConfig;

/**
 * <p>Invoke the appropriate <code>Action</code> for this request, and cache
 * the returned <code>ActionForward</code>.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-05-07 12:11:38 -0400 (Sat, 07 May 2005)
 *          $
 */
public class ExecuteAction extends AbstractExecuteAction {
    // ------------------------------------------------------- Protected Methods

    /**
     * <p>Execute the specified <code>Action</code>, and return the resulting
     * <code>ActionForward</code>.</p>
     *
     * @param context      The <code>Context</code> for this request
     * @param action       The <code>Action</code> to be executed
     * @param actionConfig The <code>ActionConfig</code> defining this action
     * @param actionForm   The <code>ActionForm</code> (if any) for this
     *                     action
     * @throws Exception if thrown by the <code>Action</code>
     */
    protected ForwardConfig execute(ActionContext context, Action action,
        ActionConfig actionConfig, ActionForm actionForm)
        throws Exception {
        ServletActionContext saContext = (ServletActionContext) context;

        return (action.execute((ActionMapping) actionConfig, actionForm,
            saContext.getRequest(), saContext.getResponse()));
    }
}
