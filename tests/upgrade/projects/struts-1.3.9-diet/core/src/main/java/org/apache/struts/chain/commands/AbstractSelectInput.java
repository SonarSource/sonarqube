/*
 * $Id: AbstractSelectInput.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.chain.contexts.ActionContext;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.config.ForwardConfig;
import org.apache.struts.config.ModuleConfig;

/**
 * <p>Select and cache a <code>ForwardConfig</code> that returns us to the
 * input page for the current action, if any.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-06-04 10:58:46 -0400 (Sat, 04 Jun 2005)
 *          $
 */
public abstract class AbstractSelectInput extends ActionCommandBase {
    // ------------------------------------------------------ Instance Variables

    /**
     * <p> Provide Commons Logging instance for this class. </p>
     */
    private static final Log LOG = LogFactory.getLog(AbstractSelectInput.class);

    // ---------------------------------------------------------- Public Methods

    /**
     * <p>Select and cache a <code>ForwardConfig</code> for the input page for
     * the current request.</p>
     *
     * @param actionCtx The <code>Context</code> for the current request
     * @return <code>false</code> so that processing continues
     * @throws Exception if thrown by the Action class
     */
    public boolean execute(ActionContext actionCtx)
        throws Exception {
        // Skip processing if the current request is valid
        Boolean valid = actionCtx.getFormValid();

        if ((valid != null) && valid.booleanValue()) {
            return (false);
        }

        // Acquire configuration objects that we need
        ActionConfig actionConfig = actionCtx.getActionConfig();
        ModuleConfig moduleConfig = actionConfig.getModuleConfig();

        // Cache an ForwardConfig back to our input page
        ForwardConfig forwardConfig;
        String input = actionConfig.getInput();

        if (moduleConfig.getControllerConfig().getInputForward()) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Finding ForwardConfig for '" + input + "'");
            }

            forwardConfig = actionConfig.findForwardConfig(input);

            if (forwardConfig == null) {
                forwardConfig = moduleConfig.findForwardConfig(input);
            }
        } else {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Delegating to forward() for '" + input + "'");
            }

            forwardConfig = forward(actionCtx, moduleConfig, input);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Forwarding back to " + forwardConfig);
        }

        actionCtx.setForwardConfig(forwardConfig);

        return (false);
    }

    // ------------------------------------------------------- Protected Methods

    /**
     * <p>Create and return a <code>ForwardConfig</code> representing the
     * specified module-relative destination.</p>
     *
     * @param context      The context for this request
     * @param moduleConfig The <code>ModuleConfig</code> for this request
     * @param uri          The module-relative URI to be the destination
     * @return ForwardConfig representing destination
     */
    protected abstract ForwardConfig forward(ActionContext context,
        ModuleConfig moduleConfig, String uri);
}
