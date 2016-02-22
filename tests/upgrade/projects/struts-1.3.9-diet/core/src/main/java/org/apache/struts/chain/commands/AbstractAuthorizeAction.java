/*
 * $Id: AbstractAuthorizeAction.java 481115 2006-12-01 00:16:41Z germuska $
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

/**
 * <p>Determine whether the requested action is authorized for the current
 * user. If not, abort chain processing and perferably, return an error
 * message of some kind.</p>
 *
 * @version $Rev: 481115 $ $Date: 2005-11-12 13:01:44 -0500 (Sat, 12 Nov 2005)
 *          $
 */
public abstract class AbstractAuthorizeAction extends ActionCommandBase {
    // ------------------------------------------------------ Instance Variables

    /**
     * Provide a Commons logging instance for this class.
     */
    private static final Log LOG =
        LogFactory.getLog(AbstractAuthorizeAction.class);

    // ---------------------------------------------------------- Public Methods

    /**
     * <p>Determine whether the requested action is authorized for the current
     * user.  If not, abort chain processing and perferably, return an error
     * message of some kind.</p>
     *
     * @param actionCtx The <code>Context</code> for the current request
     * @return <code>false</code> if the user is authorized for the selected
     *         action, else <code>true</code> to abort processing.
     * @throws UnauthorizedActionException if authorization fails 
     * or if an error is encountered in the course of performing the authorization.
     */
    public boolean execute(ActionContext actionCtx)
        throws Exception {
        // Retrieve ActionConfig
        ActionConfig actionConfig = actionCtx.getActionConfig();

        // Is this action protected by role requirements?
        if (!isAuthorizationRequired(actionConfig)) {
            return (false);
        }

        boolean throwEx;

        try {
            throwEx =
                !(isAuthorized(actionCtx, actionConfig.getRoleNames(),
                    actionConfig));
        } catch (UnauthorizedActionException ex) {
        	throw ex;
        } catch (Exception ex) {
            throwEx = true;
            LOG.error("Unable to complete authorization process", ex);
        }

        if (throwEx) {
            // The current user is not authorized for this action
            throw new UnauthorizedActionException(getErrorMessage(actionCtx,
                    actionConfig));
        } else {
            return (false);
        }
    }

    /**
     * <p>Must authorization rules be consulted?  The base implementation
     * returns <code>true</code> if the given <code>ActionConfig</code> has
     * one or more roles defined.</p>
     *
     * @param actionConfig the current ActionConfig object
     * @return true if the <code>isAuthorized</code> method should be
     *         consulted.
     */
    protected boolean isAuthorizationRequired(ActionConfig actionConfig) {
        String[] roles = actionConfig.getRoleNames();

        return (roles != null) && (roles.length > 0);
    }

    // ------------------------------------------------------- Protected Methods

    /**
     * <p>Determine if the action is authorized for the given roles.</p>
     *
     * @param context      The <code>Context</code> for the current request
     * @param roles        An array of valid roles for this request
     * @param actionConfig The current action mapping
     * @return <code>true</code> if the request is authorized, else
     *         <code>false</code>
     * @throws UnauthorizedActionException If the logic determines that the request is not authorized 
     * but does not wish to rely upon the default mechanism reporting the error.
     * @throws Exception If the action cannot be tested for authorization
     */
    protected abstract boolean isAuthorized(ActionContext context,
        String[] roles, ActionConfig actionConfig)
        throws Exception;

    /**
     * <p> Retrieve error message from context. </p>
     *
     * @param context      The <code>Context</code> for the current request
     * @param actionConfig The current action mapping
     * @return error message
     */
    protected abstract String getErrorMessage(ActionContext context,
        ActionConfig actionConfig);
}
