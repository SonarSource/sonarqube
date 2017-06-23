/*
 * $Id: PopulateActionForm.java 471754 2006-11-06 14:55:09Z husted $
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
import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.chain.commands.AbstractPopulateActionForm;
import org.apache.struts.chain.contexts.ActionContext;
import org.apache.struts.chain.contexts.ServletActionContext;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.util.RequestUtils;

/**
 * <p>Populate the form bean (if any) for this request.  Sets the multipart
 * class from the action config in the request attributes.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-11-12 13:01:44 -0500 (Sat, 12 Nov 2005)
 *          $
 */
public class PopulateActionForm extends AbstractPopulateActionForm {
    private static final Log log = LogFactory.getLog(PopulateActionForm.class);

    // ------------------------------------------------------- Protected Methods
    protected void populate(ActionContext context, ActionConfig actionConfig,
        ActionForm actionForm)
        throws Exception {
        ServletActionContext saContext = (ServletActionContext) context;

        RequestUtils.populate(actionForm, actionConfig.getPrefix(),
            actionConfig.getSuffix(), saContext.getRequest());
    }

    protected void reset(ActionContext context, ActionConfig actionConfig,
        ActionForm actionForm) {
        ServletActionContext saContext = (ServletActionContext) context;

        actionForm.reset((ActionMapping) actionConfig, saContext.getRequest());

        // Set the multipart class
        if (actionConfig.getMultipartClass() != null) {
            saContext.getRequestScope().put(Globals.MULTIPART_KEY,
                actionConfig.getMultipartClass());
        }
    }
}
