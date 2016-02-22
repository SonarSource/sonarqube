/*
 * $Id: AbstractPerformForward.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.chain.commands;

import org.apache.struts.chain.contexts.ActionContext;
import org.apache.struts.config.ForwardConfig;

/**
 * <p>Perform forwarding or redirection based on the specified
 * <code>ForwardConfig</code> (if any).</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-06-04 10:58:46 -0400 (Sat, 04 Jun 2005)
 *          $
 */
public abstract class AbstractPerformForward extends ActionCommandBase {
    // ---------------------------------------------------------- Public Methods

    /**
     * <p>Perform forwarding or redirection based on the specified
     * <code>ActionForward</code> (if any).</p>
     *
     * @param actionCtx The <code>Context</code> for the current request
     * @return <code>true</code> so that processing completes
     * @throws Exception if thrown by the <code>Action</code>
     */
    public boolean execute(ActionContext actionCtx)
        throws Exception {
        // Is there a ForwardConfig to be performed?
        ForwardConfig forwardConfig = actionCtx.getForwardConfig();

        if (forwardConfig == null) {
            return (false);
        }

        // Perform the appropriate processing on this ActionForward
        perform(actionCtx, forwardConfig);

        return (true);
    }

    // ------------------------------------------------------- Protected Methods

    /**
     * <p>Perform the appropriate processing on the specified
     * <code>ForwardConfig</code>.</p>
     *
     * @param context       The context for this request
     * @param forwardConfig The forward to be performed
     * @throws Exception if thrown by the <code>Action</code>
     */
    protected abstract void perform(ActionContext context,
        ForwardConfig forwardConfig)
        throws Exception;
}
