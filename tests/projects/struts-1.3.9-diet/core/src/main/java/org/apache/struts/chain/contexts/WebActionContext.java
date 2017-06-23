/*
 * $Id: WebActionContext.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.chain.contexts;

import org.apache.commons.chain.web.WebContext;
import org.apache.struts.Globals;
import org.apache.struts.config.ModuleConfig;

import java.util.Map;

/**
 * <p> Provide a Subclass of ActionContextBase which is understood to be
 * wrapping an instance of <code>org.apache.commons.chain.web.WebContext</code>.
 * </p>
 */
public class WebActionContext extends ActionContextBase {
    /**
     * Instantiate this composite by wrapping an instance of WebContext.
     *
     * @param context The WebContext to wrap
     */
    public WebActionContext(WebContext context) {
        super(context);
    }

    /**
     * Provide the wrapped WebContext for this composite.
     *
     * @return The wrapped WebContext
     */
    protected WebContext webContext() {
        return (WebContext) this.getBaseContext();
    }

    public void release() {
        super.release();
    }

    // -------------------------------
    // WebContext property wrappers
    // -------------------------------

    /**
     * <p> Return an immutable Map that maps header names to the first (or
     * only) header value (as a String). </p>
     *
     * @return A immutable Map of web request header names
     */
    public Map getHeader() {
        return webContext().getHeader();
    }

    /**
     * <p> Return an immutable Map that maps header names to the set of all
     * values specified in the request (as a String array). Header names must
     * be matched in a case-insensitive manner. </p>
     *
     * @return An immutable Map of web request header values
     */
    public Map getHeaderValues() {
        return webContext().getHeaderValues();
    }

    /**
     * <p> Return an immutable Map that maps context application
     * initialization parameters to their values. </p>
     *
     * @return An immutable Map of web context initialization parameters
     */
    public Map getInitParam() {
        return webContext().getInitParam();
    }

    /**
     * <p> Return a map whose keys are <code>String</code> request parameter
     * names and whose values are <code>String</code> values. </p> <p> For
     * parameters which were submitted with more than one value, only one
     * value will be returned, as if one called
     * <code>ServletRequest.getParameter(String)</code>
     * </p>
     *
     * @return A map of web request parameters
     */
    public Map getParam() {
        return webContext().getParam();
    }

    /**
     * <p> Return a map whose keys are <code>String</code> request parameter
     * names and whose values are <code>String[]</code> values. </p>
     *
     * @return A map of web request parameter values (as an array)
     */
    public Map getParamValues() {
        return webContext().getParamValues();
    }

    public Map getApplicationScope() {
        return webContext().getApplicationScope();
    }

    public Map getRequestScope() {
        return webContext().getRequestScope();
    }

    public Map getParameterMap() {
        return getParamValues();
    }

    public Map getSessionScope() {
        return webContext().getSessionScope();
    }

    // ISSUE: AbstractSelectModule set the precedent of doing this at the
    // "web context" level instead of the ServletWebContext level.
    // Consider whether that's how we want to do it universally for other
    // manipulations of the RequestScope or not...
    public void setModuleConfig(ModuleConfig moduleConfig) {
        super.setModuleConfig(moduleConfig);
        this.getRequestScope().put(Globals.MODULE_KEY, moduleConfig);
    }

    /**
     * @see org.apache.struts.chain.contexts.ActionContext#getModuleConfig()
     */
    public ModuleConfig getModuleConfig() {
        ModuleConfig mc = super.getModuleConfig();

        if (mc == null) {
            mc = (ModuleConfig) this.getRequestScope().get(Globals.MODULE_KEY);
        }

        return mc;
    }

    // ISSUE:  AbstractSelectModule set the precedent of doing this at the
    // "web context" level instead of the ServletWebContext level.  Consider
    // whether that's how we want to do it universally for other manipulations
    // of the RequestScope or not...
    public void setCancelled(Boolean cancelled) {
        super.setCancelled(cancelled);

        // historic semantics of "isCancelled" are to consider any non-null
        // value in the request under Globals.CANCEL_KEY as "yes, this was
        // cancelled."
        if ((cancelled != null) && cancelled.booleanValue()) {
            this.getRequestScope().put(Globals.CANCEL_KEY, cancelled);
        } else {
            this.getRequestScope().remove(Globals.CANCEL_KEY);
        }
    }

    public Boolean getCancelled() {
        Boolean cancelled = super.getCancelled();

        if (cancelled == null) {
            cancelled =
                (Boolean) this.getRequestScope().get(Globals.CANCEL_KEY);
        }

        return cancelled;
    }
}
