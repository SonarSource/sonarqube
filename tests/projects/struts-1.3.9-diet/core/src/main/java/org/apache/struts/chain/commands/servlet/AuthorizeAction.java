/*
 * $Id: AuthorizeAction.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.struts.action.ActionServlet;
import org.apache.struts.chain.commands.AbstractAuthorizeAction;
import org.apache.struts.chain.contexts.ActionContext;
import org.apache.struts.chain.contexts.ServletActionContext;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.util.MessageResources;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>Determine if the action is authorized for the given roles.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-11-12 13:01:44 -0500 (Sat, 12 Nov 2005)
 *          $
 */
public class AuthorizeAction extends AbstractAuthorizeAction {
    // ------------------------------------------------------- Protected Methods
    protected boolean isAuthorized(ActionContext context, String[] roles,
        ActionConfig mapping)
        throws Exception {
        // Identify the HTTP request object
        ServletActionContext servletActionContext =
            (ServletActionContext) context;
        HttpServletRequest request = servletActionContext.getRequest();

        // Check the current user against the list of required roles
        for (int i = 0; i < roles.length; i++) {
            if (request.isUserInRole(roles[i])) {
                return (true);
            }
        }

        // Default to unauthorized
        return (false);
    }

    protected String getErrorMessage(ActionContext context,
        ActionConfig actionConfig) {
        ServletActionContext servletActionContext =
            (ServletActionContext) context;

        // Retrieve internal message resources
        ActionServlet servlet = servletActionContext.getActionServlet();
        MessageResources resources = servlet.getInternal();

        return resources.getMessage("notAuthorized", actionConfig.getPath());
    }
}
