/*
 * $Id: AbstractSelectModule.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.struts.Globals;
import org.apache.struts.chain.contexts.ActionContext;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.util.MessageResources;

/**
 * <p>Cache the <code>ModuleConfig</code> and <code>MessageResources</code>
 * instances for the sub-application module to be used for processing this
 * request.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-11-12 13:01:44 -0500 (Sat, 12 Nov 2005)
 *          $
 */
public abstract class AbstractSelectModule extends ActionCommandBase {
    // ------------------------------------------------------ Instance Variables
    // ---------------------------------------------------------- Public Methods

    /**
     * <p>Cache the <code>ModuleConfig</code> and <code>MessageResources</code>
     * instances for the sub-application module to be used for processing this
     * request.</p>
     *
     * @param actionCtx The <code>Context</code> for the current request
     * @return <code>false</code> so that processing continues
     * @throws IllegalArgumentException if no valid ModuleConfig or
     *                                  MessageResources can be identified for
     *                                  this request
     * @throws Exception                if thrown by the Action class
     */
    public boolean execute(ActionContext actionCtx)
        throws Exception {
        String prefix = getPrefix(actionCtx);

        // Cache the corresponding ModuleConfig and MessageResources instances
        ModuleConfig moduleConfig =
            (ModuleConfig) actionCtx.getApplicationScope().get(Globals.MODULE_KEY
                + prefix);

        if (moduleConfig == null) {
            throw new IllegalArgumentException("No module config for prefix '"
                + prefix + "'");
        }

        actionCtx.setModuleConfig(moduleConfig);

        String key = Globals.MESSAGES_KEY + prefix;
        MessageResources messageResources =
            (MessageResources) actionCtx.getApplicationScope().get(key);

        if (messageResources == null) {
            throw new IllegalArgumentException(
                "No message resources found in application scope under " + key);
        }

        actionCtx.setMessageResources(messageResources);

        return (false);
    }

    // ------------------------------------------------------- Protected Methods

    /**
     * <p>Calculate and return the module prefix for the module to be selected
     * for this request.</p>
     *
     * @param context The <code>Context</code> for this request
     * @return Module prefix to be used with this request
     * @throws IllegalArgumentException if no valid ModuleConfig or
     *                                  MessageResources can be identified for
     *                                  this request
     */
    protected abstract String getPrefix(ActionContext context);
}
