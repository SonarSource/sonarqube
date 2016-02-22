/*
 * $Id: CreateAction.java 510851 2007-02-23 07:05:18Z pbenedict $
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.chain.Constants;
import org.apache.struts.chain.commands.util.ClassUtils;
import org.apache.struts.chain.contexts.ActionContext;
import org.apache.struts.chain.contexts.ServletActionContext;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.config.ModuleConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Concrete implementation of <code>AbstractCreateAction</code> for use in
 * a Servlet API chain.  Expects that the ActionContext passed into it can
 * safely be cast to <code>ServletActionContext</code>.</p>
 */
public class CreateAction
    extends org.apache.struts.chain.commands.AbstractCreateAction {
    // ------------------------------------------------------ Instance Variables
    private static final Log log = LogFactory.getLog(CreateAction.class);

    /* :TODO The Action class' dependency on having its "servlet" property set
     * requires this API-dependent subclass of AbstractCreateAction.
     */
    protected synchronized Action getAction(ActionContext context, String type,
        ActionConfig actionConfig)
        throws Exception {
        ModuleConfig moduleConfig = actionConfig.getModuleConfig();
        String actionsKey = Constants.ACTIONS_KEY + moduleConfig.getPrefix();
        Map actions = (Map) context.getApplicationScope().get(actionsKey);

        if (actions == null) {
            actions = new HashMap();
            context.getApplicationScope().put(actionsKey, actions);
        }

        Action action = null;

        synchronized (actions) {
            action = (Action) actions.get(type);

            if (action == null) {
                action = createAction(context, type);
                actions.put(type, action);
            }
        }

        if (action.getServlet() == null) {
            ServletActionContext saContext = (ServletActionContext) context;
            ActionServlet actionServlet = saContext.getActionServlet();

            action.setServlet(actionServlet);
        }

        return (action);
    }

    
    /**
     * <p>Invoked by <code>getAction</code> when the <code>Action</code> 
     * actually has to be created. If the instance is already created and 
     * cached, this method will not be called. </p>
     * 
     * @param context      The <code>Context</code> for this request
     * @param type         Name of class to instantiate
     * @return Instantiated Action class
     * @throws Exception if there are any problems instantiating the Action
     *                   class.
     * @since Struts 1.3.7
     */
    protected Action createAction(ActionContext context, String type) throws Exception {
        log.info("Initialize action of type: " + type);
        return (Action) ClassUtils.getApplicationInstance(type);
    }
}
