/*
 * $Id: AbstractExceptionHandler.java 471754 2006-11-06 14:55:09Z husted $
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
import org.apache.struts.config.ExceptionConfig;
import org.apache.struts.config.ForwardConfig;
import org.apache.struts.config.ModuleConfig;

/**
 * <p>Invoke the local or global exception handler configured for the
 * exception class that occurred.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-11-12 13:01:44 -0500 (Sat, 12 Nov 2005)
 *          $
 */
public abstract class AbstractExceptionHandler extends ActionCommandBase {
    // ------------------------------------------------------ Instance Variables

    /**
     * Provide a Commons logging instance for this class.
     */
    private static final Log LOG =
        LogFactory.getLog(AbstractExceptionHandler.class);

    // ---------------------------------------------------------- Public Methods

    /**
     * <p>Invoke the appropriate <code>Action</code> for this request, and
     * cache the returned <code>ActionForward</code>.</p>
     *
     * @param actionCtx The <code>Context</code> for the current request
     * @return <code>false</code> if a <code>ForwardConfig</code> is returned,
     *         else <code>true</code> to complete processing
     * @throws Exception if thrown by the Action class and not declared by an
     *                   Exception Handler
     */
    public boolean execute(ActionContext actionCtx)
        throws Exception {
        // Look up the exception that was thrown
        Exception exception = actionCtx.getException();

        if (exception == null) {
            LOG.warn("No Exception found in ActionContext");

            return (true);
        }

        // Look up the local or global exception handler configuration
        ExceptionConfig exceptionConfig = null;
        ActionConfig actionConfig = actionCtx.getActionConfig();
        ModuleConfig moduleConfig = actionCtx.getModuleConfig();

        if (actionConfig != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("See if actionConfig " + actionConfig
                    + " has an exceptionConfig for "
                    + exception.getClass().getName());
            }

            exceptionConfig = actionConfig.findException(exception.getClass());
        } else if (moduleConfig != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No action yet, see if moduleConfig " + moduleConfig
                    + " has an exceptionConfig "
                    + exception.getClass().getName());
            }

            exceptionConfig = moduleConfig.findException(exception.getClass());
        }

        // Handle the exception in the configured manner
        if (exceptionConfig == null) {
            LOG.warn("Unhandled exception", exception);
            throw exception;
        }

        ForwardConfig forwardConfig =
            handle(actionCtx, exception, exceptionConfig, actionConfig,
                moduleConfig);

        if (forwardConfig != null) {
            actionCtx.setForwardConfig(forwardConfig);

            return (false);
        } else {
            return (true);
        }
    }

    // ------------------------------------------------------- Protected Methods

    /**
     * <p>Perform the required handling of the specified exception.</p>
     *
     * @param context         The <code>Context</code> for this request
     * @param exception       The exception being handled
     * @param exceptionConfig The corresponding {@link ExceptionConfig}
     * @param actionConfig    The {@link ActionConfig} for this request
     * @param moduleConfig    The {@link ModuleConfig} for this request
     * @return the <code>ForwardConfig</code> to be processed next (if any),
     *         or <code>null</code> if processing has been completed
     * @throws Exception if there are any problems handling the exception
     */
    protected abstract ForwardConfig handle(ActionContext context,
        Exception exception, ExceptionConfig exceptionConfig,
        ActionConfig actionConfig, ModuleConfig moduleConfig)
        throws Exception;
}
