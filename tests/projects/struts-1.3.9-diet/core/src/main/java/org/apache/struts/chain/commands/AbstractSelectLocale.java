/*
 * $Id: AbstractSelectLocale.java 471754 2006-11-06 14:55:09Z husted $
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
import org.apache.struts.config.ModuleConfig;

import java.util.Locale;

/**
 * <p>Select the <code>Locale</code> to be used for this request.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-11-12 13:01:44 -0500 (Sat, 12 Nov 2005)
 *          $
 */
public abstract class AbstractSelectLocale extends ActionCommandBase {
    // ------------------------------------------------------ Instance Variables

    /**
     * <p> Provide Commons Logging instance for this class. </p>
     */
    private static final Log LOG =
        LogFactory.getLog(AbstractSelectLocale.class);

    // ---------------------------------------------------------- Public Methods

    /**
     * <p>Select the <code>Locale</code> to be used for this request.</p>
     *
     * @param actionCtx The <code>Context</code> for the current request
     * @return <code>false</code> so that processing continues
     * @throws Exception if thrown by the Action class
     */
    public boolean execute(ActionContext actionCtx)
        throws Exception {
        // Are we configured to select Locale automatically?
        LOG.trace("retrieve config...");

        ModuleConfig moduleConfig = actionCtx.getModuleConfig();

        if (!moduleConfig.getControllerConfig().getLocale()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("module is not configured for a specific locale; "
                    + "nothing to do");
            }

            return (false);
        }

        // Retrieve and cache appropriate Locale for this request
        Locale locale = getLocale(actionCtx);

        if (LOG.isDebugEnabled()) {
            LOG.debug("set context locale to " + locale);
        }

        actionCtx.setLocale(locale);

        return (false);
    }

    // ------------------------------------------------------- Protected Methods

    /**
     * <p>Return the <code>Locale</code> to be used for this request.</p>
     *
     * @param context The <code>Context</code> for this request
     * @return The Locale to be used for this request
     */
    protected abstract Locale getLocale(ActionContext context);
}
