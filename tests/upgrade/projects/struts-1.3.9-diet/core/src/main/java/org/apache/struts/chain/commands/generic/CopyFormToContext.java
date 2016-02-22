/*
 * $Id: CopyFormToContext.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.chain.commands.generic;

import org.apache.struts.action.ActionForm;
import org.apache.struts.chain.commands.ActionCommandBase;
import org.apache.struts.chain.contexts.ActionContext;
import org.apache.struts.chain.contexts.ActionContextBase;
import org.apache.struts.config.ActionConfig;

/**
 * <p>Subclass this command and configure it as part of a per-forward chain to
 * perform any necessary pre-population or other preparation for a form before
 * control is dispatched to the view layer.</p>
 *
 * @version $Id: CopyFormToContext.java 471754 2006-11-06 14:55:09Z husted $
 */
public class CopyFormToContext extends ActionCommandBase {
    // ------------------------------------------------------ Instance Variables

    /**
     * <p>The name of a form bean as configured in a struts-config.xml file
     * for this module.  </p>
     *
     * <p> Either actionPath or both this and scope are required configuration
     * properties.</p>
     */
    private String formName = null;

    /**
     * <p>The name of a scope, such as "request" or "session" in which the
     * form to be prepared will be placed for reference by the view and other
     * parts of Struts.</p>
     *
     * <p>Either <code>actionPath</code> or both this and
     * <code>formName</code> are required configuration properties.</p>
     */
    private String scope = null;

    /**
     * <p>The path of an <code>&lt;action&gt;</code> mapping as configured in
     * a <code>struts-config.xml</code> file for this module.  This action
     * will be looked up, and its <code>name</code> and <code>scope</code>
     * values will be used as if those values were configured directly in this
     * instance's <code>formName</code> and <code>scope</code>
     * properties.</p>
     *
     * <p>Either <code>this</code> or both <code>scope</code> and
     * <code>formName</code> are required configuration properties.</p>
     */
    private String actionPath = null;

    /**
     * The context key under which the form which was looked up will be
     * stored. Defaults to "actionForm" but may be overridden in cases where
     * the "request" ActionForm must be preserved.
     */
    private String toKey = ActionContextBase.ACTION_FORM_KEY;

    // ------------------------------------------------------ Properties

    /**
     * <p>Return ActionPath property.</p>
     *
     * @return ActionPath property
     */
    public String getActionPath() {
        return this.actionPath;
    }

    /**
     * <p>Set ActionPath property.</p>
     *
     * @param actionPath New valuefor ActionPath
     */
    public void setActionPath(String actionPath) {
        this.actionPath = actionPath;
    }

    /**
     * <p>Return FormName property.</p>
     *
     * @return FormName property
     */
    public String getFormName() {
        return this.formName;
    }

    /**
     * <p>Set FormName property.</p>
     *
     * @param formName New valuefor FormName
     */
    public void setFormName(String formName) {
        this.formName = formName;
    }

    /**
     * <p>Return Scope property.</p>
     *
     * @return Scope property
     */
    public String getScope() {
        return this.scope;
    }

    /**
     * <p>Set Scope property.</p>
     *
     * @param scope New valuefor Scope
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * <p>Return ToKey property.</p>
     *
     * @return ToKey property
     */
    public String getToKey() {
        return this.toKey;
    }

    /**
     * <p>Set ToKey property.</p>
     *
     * @param toKey New valuefor FormName
     */
    public void setToKey(String toKey) {
        this.toKey = toKey;
    }

    // ------------------------------------------------------

    /**
     * <p>Look up an ActionForm instance based on the configured properties of
     * this command and copy it into the <code>Context</code>.  After this
     * command successfully executes, an ActionForm instance will exist in the
     * specified scope and will be available, for example for backing fields
     * in an HTML form.  It will also be in the <code>ActionContext</code>
     * available for another command to do prepopulation of values or other
     * preparation.</p>
     *
     * @param actionContext Our ActionContext
     * @return TRUE if processing should halt
     * @throws Exception on any error
     */
    public boolean execute(ActionContext actionContext)
        throws Exception {
        ActionForm form = findOrCreateForm(actionContext);

        if (isEmpty(getToKey())) {
            throw new IllegalStateException("Property 'toKey' must be defined.");
        }

        actionContext.put(getToKey(), form);

        return false;
    }

    /**
     * <p>Based on the properties of this command and the given
     * <code>ActionContext</code>, find or create an ActionForm instance for
     * preparation.</p>
     *
     * @param context ActionContextBase class that we are processing
     * @return ActionForm instance
     * @throws IllegalArgumentException On ActionConfig not found
     * @throws IllegalStateException    On undefined scope and formbean
     * @throws IllegalAccessException   On failed instantiation
     * @throws InstantiationException   If ActionContext is not subsclass of
     *                                  ActionContextBase
     */
    protected ActionForm findOrCreateForm(ActionContext context)
        throws IllegalAccessException, InstantiationException {
        String effectiveFormName;
        String effectiveScope;

        if (!(isEmpty(this.getActionPath()))) {
            ActionConfig actionConfig =
                context.getModuleConfig().findActionConfig(this.getActionPath());

            if (actionConfig == null) {
                throw new IllegalArgumentException(
                    "No ActionConfig found for path " + this.getActionPath());
            }

            effectiveFormName = actionConfig.getName();
            effectiveScope = actionConfig.getScope();
        } else {
            effectiveFormName = this.getFormName();
            effectiveScope = this.getScope();
        }

        if (isEmpty(effectiveScope) || isEmpty(effectiveFormName)) {
            throw new IllegalStateException("Both scope [" + effectiveScope
                + "] and formName [" + effectiveFormName + "] must be defined.");
        }

        return findOrCreateForm(context, effectiveFormName, effectiveScope);
    }

    /**
     * <p>Actually find or create an instance of ActionForm configured under
     * the form-bean-name <code>effectiveFormName</code>, looking in in the
     * <code>ActionContext's</code> scope as identified by
     * <code>effectiveScope</code>. If a form is created, it will also be
     * stored in that scope.</p>
     *
     * <p><b>NOTE:</b> This specific method depends on the instance of
     * <code>ActionContext</code> which is passed being a subclass of
     * <code>ActionContextBase</code>, which implements the utility method
     * <code>findOrCreateActionForm</code>. </p>
     *
     * @param ctx               The ActionContext we are processing
     * @param effectiveFormName the target form name
     * @param effectiveScope    The target scope
     * @return ActionForm instnace, storing in scope if created
     * @throws InstantiationException   If ActionContext is not subsclass of
     *                                  ActionContextBase
     * @throws InstantiationException   If object cannot be created
     * @throws IllegalArgumentException On form not found in/ scope
     * @throws IllegalAccessException   On failed instantiation
     * @throws IllegalStateException    If ActionContext is not a subclass of
     *                                  ActionBase
     */
    protected ActionForm findOrCreateForm(ActionContext ctx,
        String effectiveFormName, String effectiveScope)
        throws IllegalAccessException, InstantiationException {
        ActionContextBase context;

        try {
            context = (ActionContextBase) ctx;
        } catch (ClassCastException e) {
            throw new IllegalStateException("ActionContext [" + ctx + "]"
                + " must be subclass of ActionContextBase");
        }

        ActionForm form =
            context.findOrCreateActionForm(effectiveFormName, effectiveScope);

        if (form == null) {
            throw new IllegalArgumentException("No form found under scope ["
                + effectiveScope + "] and formName [" + effectiveFormName + "]");
        }

        return form;
    }

    /**
     * <p>Convenience method to test for an empty string.</p>
     *
     * @param test String to test
     * @return TRUE if test is null or zero-length
     */
    private boolean isEmpty(String test) {
        return (test == null) || (test.trim().length() == 0);
    }
}
