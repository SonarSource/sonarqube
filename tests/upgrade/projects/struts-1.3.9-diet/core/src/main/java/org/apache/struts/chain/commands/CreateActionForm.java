/*
 * $Id: CreateActionForm.java 471754 2006-11-06 14:55:09Z husted $
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
import org.apache.struts.action.ActionForm;
import org.apache.struts.chain.contexts.ActionContext;
import org.apache.struts.chain.contexts.ServletActionContext;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.config.FormBeanConfig;

import java.util.Map;

/**
 * <p>Create (if necessary) and cache a form bean for this request.</p>
 *
 * @version $Id: CreateActionForm.java 471754 2006-11-06 14:55:09Z husted $
 */
public class CreateActionForm extends ActionCommandBase {
    // ------------------------------------------------------ Instance Variables

    /**
     * <p> Provide Commons Logging instance for this class. </p>
     */
    private static final Log LOG = LogFactory.getLog(CreateActionForm.class);

    // ---------------------------------------------------------- Public Methods

    /**
     * <p>Create (if necessary) and cache a form bean for this request.</p>
     *
     * @param actionCtx The <code>Context</code> for the current request
     * @return <code>false</code> so that processing continues
     * @throws Exception on any error
     */
    public boolean execute(ActionContext actionCtx)
        throws Exception {
        // Is there a form bean associated with this ActionConfig?
        ActionConfig actionConfig = actionCtx.getActionConfig();
        String name = actionConfig.getName();

        if (name == null) {
            actionCtx.setActionForm(null);

            return (false);
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("Look up form-bean " + name);
        }

        // Look up the corresponding FormBeanConfig (if any)
        FormBeanConfig formBeanConfig =
            actionConfig.getModuleConfig().findFormBeanConfig(name);

        if (formBeanConfig == null) {
            LOG.warn("No FormBeanConfig found in module "
                + actionConfig.getModuleConfig().getPrefix() + " under name "
                + name);
            actionCtx.setActionForm(null);

            return (false);
        }

        Map scope = actionCtx.getScope(actionConfig.getScope());

        ActionForm instance;

        instance = (ActionForm) scope.get(actionConfig.getAttribute());

        // Can we recycle the existing instance (if any)?
        if (!formBeanConfig.canReuse(instance)) {
            instance = formBeanConfig.createActionForm(actionCtx);
        }

        // TODO: Remove ServletActionContext when ActionForm no longer
        //  directly depends on ActionServlet
        if (actionCtx instanceof ServletActionContext) {
            // The servlet property of ActionForm is transient, so
            // ActionForms which are restored from a serialized state
            // need to have their servlet restored.
            ServletActionContext sac = (ServletActionContext) actionCtx;

            instance.setServlet(sac.getActionServlet());
        }

        actionCtx.setActionForm(instance);

        scope.put(actionConfig.getAttribute(), instance);

        return (false);
    }
}
