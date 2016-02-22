/*
 * $Id: FormTag.java 479633 2006-11-27 14:25:35Z pbenedict $
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
package org.apache.struts.taglib.html;

import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.config.ActionConfig;
import org.apache.struts.config.FormBeanConfig;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.taglib.TagUtils;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.RequestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import java.io.IOException;

/**
 * Custom tag that represents an input form, associated with a bean whose
 * properties correspond to the various fields of the form.
 *
 * @version $Rev: 479633 $ $Date: 2006-11-27 15:25:35 +0100 (Mon, 27 Nov 2006) $
 */
public class FormTag extends TagSupport {
    /**
     * The line ending string.
     */
    protected static String lineEnd = System.getProperty("line.separator");

    /**
     * The message resources for this package.
     */
    protected static MessageResources messages =
        MessageResources.getMessageResources(Constants.Package
            + ".LocalStrings");

    // ----------------------------------------------------- Instance Variables

    /**
     * The action URL to which this form should be submitted, if any.
     */
    protected String action = null;

    /**
     * A postback action URL to which this form should be submitted, if any.
     */
    private String postbackAction = null;

    /**
     * The module configuration for our module.
     */
    protected ModuleConfig moduleConfig = null;

    /**
     * The content encoding to be used on a POST submit.
     */
    protected String enctype = null;

    /**
     * The name of the field to receive focus, if any.
     */
    protected String focus = null;

    /**
     * The index in the focus field array to receive focus.  This only applies
     * if the field given in the focus attribute is actually an array of
     * fields.  This allows a specific field in a radio button array to
     * receive focus while still allowing indexed field names like
     * "myRadioButtonField[1]" to be passed in the focus attribute.
     *
     * @since Struts 1.1
     */
    protected String focusIndex = null;

    /**
     * The ActionMapping defining where we will be submitting this form
     */
    protected ActionMapping mapping = null;

    /**
     * The request method used when submitting this form.
     */
    protected String method = null;

    /**
     * The onReset event script.
     */
    protected String onreset = null;

    /**
     * The onSubmit event script.
     */
    protected String onsubmit = null;

    /**
     * Include language attribute in the focus script's &lt;script&gt;
     * element.  This property is ignored in XHTML mode.
     *
     * @since Struts 1.2
     */
    protected boolean scriptLanguage = true;

    /**
     * The ActionServlet instance we are associated with (so that we can
     * initialize the <code>servlet</code> property on any form bean that we
     * create).
     */
    protected ActionServlet servlet = null;

    /**
     * The style attribute associated with this tag.
     */
    protected String style = null;

    /**
     * The style class associated with this tag.
     */
    protected String styleClass = null;

    /**
     * The identifier associated with this tag.
     */
    protected String styleId = null;

    /**
     * The window target.
     */
    protected String target = null;

    /**
     * The name of the form bean to (create and) use. This is either the same
     * as the 'name' attribute, if that was specified, or is obtained from the
     * associated <code>ActionMapping</code> otherwise.
     */
    protected String beanName = null;

    /**
     * The scope of the form bean to (create and) use. This is either the same
     * as the 'scope' attribute, if that was specified, or is obtained from
     * the associated <code>ActionMapping</code> otherwise.
     */
    protected String beanScope = null;

    /**
     * The type of the form bean to (create and) use. This is either the same
     * as the 'type' attribute, if that was specified, or is obtained from the
     * associated <code>ActionMapping</code> otherwise.
     */
    protected String beanType = null;

    /**
     * The list of character encodings for input data that the server should
     * accept.
     */
    protected String acceptCharset = null;

    /**
     * Controls whether child controls should be 'disabled'.
     */
    private boolean disabled = false;

    /**
     * Controls whether child controls should be 'readonly'.
     */
    protected boolean readonly = false;

    /**
     * The language code of this element.
     */
    private String lang = null;
    
    /**
     * The direction for weak/neutral text of this element.
     */
    private String dir = null;

    // ------------------------------------------------------------- Properties

    /**
     * Return the name of the form bean corresponding to this tag. There is no
     * corresponding setter method; this method exists so that the nested tag
     * classes can obtain the actual bean name derived from other attributes
     * of the tag.
     */
    public String getBeanName() {
        return beanName;
    }

    /**
     * Return the action URL to which this form should be submitted.
     */
    public String getAction() {
        return (this.action);
    }

    /**
     * Set the action URL to which this form should be submitted.
     *
     * @param action The new action URL
     */
    public void setAction(String action) {
        this.action = action;
    }

    /**
     * Return the content encoding used when submitting this form.
     */
    public String getEnctype() {
        return (this.enctype);
    }

    /**
     * Set the content encoding used when submitting this form.
     *
     * @param enctype The new content encoding
     */
    public void setEnctype(String enctype) {
        this.enctype = enctype;
    }

    /**
     * Return the focus field name for this form.
     */
    public String getFocus() {
        return (this.focus);
    }

    /**
     * Set the focus field name for this form.
     *
     * @param focus The new focus field name
     */
    public void setFocus(String focus) {
        this.focus = focus;
    }

    /**
     * Return the request method used when submitting this form.
     */
    public String getMethod() {
        return (this.method);
    }

    /**
     * Set the request method used when submitting this form.
     *
     * @param method The new request method
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Return the onReset event script.
     */
    public String getOnreset() {
        return (this.onreset);
    }

    /**
     * Set the onReset event script.
     *
     * @param onReset The new event script
     */
    public void setOnreset(String onReset) {
        this.onreset = onReset;
    }

    /**
     * Return the onSubmit event script.
     */
    public String getOnsubmit() {
        return (this.onsubmit);
    }

    /**
     * Set the onSubmit event script.
     *
     * @param onSubmit The new event script
     */
    public void setOnsubmit(String onSubmit) {
        this.onsubmit = onSubmit;
    }

    /**
     * Return the style attribute for this tag.
     */
    public String getStyle() {
        return (this.style);
    }

    /**
     * Set the style attribute for this tag.
     *
     * @param style The new style attribute
     */
    public void setStyle(String style) {
        this.style = style;
    }

    /**
     * Return the style class for this tag.
     */
    public String getStyleClass() {
        return (this.styleClass);
    }

    /**
     * Set the style class for this tag.
     *
     * @param styleClass The new style class
     */
    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }

    /**
     * Return the style identifier for this tag.
     */
    public String getStyleId() {
        return (this.styleId);
    }

    /**
     * Set the style identifier for this tag.
     *
     * @param styleId The new style identifier
     */
    public void setStyleId(String styleId) {
        this.styleId = styleId;
    }

    /**
     * Return the window target.
     */
    public String getTarget() {
        return (this.target);
    }

    /**
     * Set the window target.
     *
     * @param target The new window target
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Return the list of character encodings accepted.
     */
    public String getAcceptCharset() {
        return acceptCharset;
    }

    /**
     * Set the list of character encodings accepted.
     *
     * @param acceptCharset The list of character encodings
     */
    public void setAcceptCharset(String acceptCharset) {
        this.acceptCharset = acceptCharset;
    }

    /**
     * Sets the disabled event handler.
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * Returns the disabled event handler.
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Sets the readonly event handler.
     */
    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    /**
     * Returns the readonly event handler.
     */
    public boolean isReadonly() {
        return readonly;
    }

    /**
     * Returns the language code of this element.
     * 
     * @since Struts 1.3.6
     */
    public String getLang() {
        return this.lang;
    }

    /**
     * Sets the language code of this element.
     * 
     * @since Struts 1.3.6
     */
    public void setLang(String lang) {
        this.lang = lang;
    }

    /**
     * Returns the direction for weak/neutral text this element.
     * 
     * @since Struts 1.3.6
     */
    public String getDir() {
        return this.dir;
    }

    /**
     * Sets the direction for weak/neutral text of this element.
     * 
     * @since Struts 1.3.6
     */
    public void setDir(String dir) {
        this.dir = dir;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Render the beginning of this form.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {

        postbackAction = null;

        // Look up the form bean name, scope, and type if necessary
        this.lookup();

        // Create an appropriate "form" element based on our parameters
        StringBuffer results = new StringBuffer();

        results.append(this.renderFormStartElement());

        results.append(this.renderToken());

        TagUtils.getInstance().write(pageContext, results.toString());

        // Store this tag itself as a page attribute
        pageContext.setAttribute(Constants.FORM_KEY, this,
            PageContext.REQUEST_SCOPE);

        this.initFormBean();

        return (EVAL_BODY_INCLUDE);
    }

    /**
     * Locate or create the bean associated with our form.
     *
     * @throws JspException
     * @since Struts 1.1
     */
    protected void initFormBean()
        throws JspException {
        int scope = PageContext.SESSION_SCOPE;

        if ("request".equalsIgnoreCase(beanScope)) {
            scope = PageContext.REQUEST_SCOPE;
        }

        Object bean = pageContext.getAttribute(beanName, scope);

        if (bean == null) {
            // New and improved - use the values from the action mapping
            bean =
                RequestUtils.createActionForm((HttpServletRequest) pageContext
                    .getRequest(), mapping, moduleConfig, servlet);

            if (bean instanceof ActionForm) {
                ((ActionForm) bean).reset(mapping,
                    (HttpServletRequest) pageContext.getRequest());
            }

            if (bean == null) {
                throw new JspException(messages.getMessage("formTag.create",
                        beanType));
            }

            pageContext.setAttribute(beanName, bean, scope);
        }

        pageContext.setAttribute(Constants.BEAN_KEY, bean,
            PageContext.REQUEST_SCOPE);
    }

    /**
     * Generates the opening <code>&lt;form&gt;</code> element with
     * appropriate attributes.
     *
     * @since Struts 1.1
     */
    protected String renderFormStartElement()
        throws JspException {
        StringBuffer results = new StringBuffer("<form");

        // render attributes
        renderName(results);

        renderAttribute(results, "method",
            (getMethod() == null) ? "post" : getMethod());
        renderAction(results);
        renderAttribute(results, "accept-charset", getAcceptCharset());
        renderAttribute(results, "class", getStyleClass());
        renderAttribute(results, "dir", getDir());
        renderAttribute(results, "enctype", getEnctype());
        renderAttribute(results, "lang", getLang());
        renderAttribute(results, "onreset", getOnreset());
        renderAttribute(results, "onsubmit", getOnsubmit());
        renderAttribute(results, "style", getStyle());
        renderAttribute(results, "target", getTarget());

        // Hook for additional attributes
        renderOtherAttributes(results);

        results.append(">");

        return results.toString();
    }

    /**
     * Renders the name of the form.  If XHTML is set to true, the name will
     * be rendered as an 'id' attribute, otherwise as a 'name' attribute.
     */
    protected void renderName(StringBuffer results)
        throws JspException {
        if (this.isXhtml()) {
            if (getStyleId() == null) {
                renderAttribute(results, "id", beanName);
            } else {
                throw new JspException(messages.getMessage("formTag.ignoredId"));
            }
        } else {
            renderAttribute(results, "name", beanName);
            renderAttribute(results, "id", getStyleId());
        }
    }

    /**
     * Renders the action attribute
     */
    protected void renderAction(StringBuffer results) {
        String calcAction = (this.action == null ? postbackAction : this.action);
        HttpServletResponse response =
            (HttpServletResponse) this.pageContext.getResponse();

        results.append(" action=\"");
        results.append(response.encodeURL(
                TagUtils.getInstance().getActionMappingURL(calcAction,
                    this.pageContext)));

        results.append("\"");
    }

    /**
     * 'Hook' to enable this tag to be extended and additional attributes
     * added.
     */
    protected void renderOtherAttributes(StringBuffer results) {
    }

    /**
     * Generates a hidden input field with token information, if any. The
     * field is added within a div element for HTML 4.01 Strict compliance.
     *
     * @return A hidden input field containing the token.
     * @since Struts 1.1
     */
    protected String renderToken() {
        StringBuffer results = new StringBuffer();
        HttpSession session = pageContext.getSession();

        if (session != null) {
            String token =
                (String) session.getAttribute(Globals.TRANSACTION_TOKEN_KEY);

            if (token != null) {
                results.append("<div><input type=\"hidden\" name=\"");
                results.append(Constants.TOKEN_KEY);
                results.append("\" value=\"");
                results.append(token);

                if (this.isXhtml()) {
                    results.append("\" />");
                } else {
                    results.append("\">");
                }

                results.append("</div>");
            }
        }

        return results.toString();
    }

    /**
     * Renders attribute="value" if not null
     */
    protected void renderAttribute(StringBuffer results, String attribute,
        String value) {
        if (value != null) {
            results.append(" ");
            results.append(attribute);
            results.append("=\"");
            results.append(value);
            results.append("\"");
        }
    }

    /**
     * Render the end of this form.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doEndTag() throws JspException {
        // Remove the page scope attributes we created
        pageContext.removeAttribute(Constants.BEAN_KEY,
            PageContext.REQUEST_SCOPE);
        pageContext.removeAttribute(Constants.FORM_KEY,
            PageContext.REQUEST_SCOPE);

        // Render a tag representing the end of our current form
        StringBuffer results = new StringBuffer("</form>");

        // Render JavaScript to set the input focus if required
        if (this.focus != null) {
            results.append(this.renderFocusJavascript());
        }

        // Print this value to our output writer
        JspWriter writer = pageContext.getOut();

        try {
            writer.print(results.toString());
        } catch (IOException e) {
            throw new JspException(messages.getMessage("common.io", e.toString()));
        }

        postbackAction = null;

        // Continue processing this page
        return (EVAL_PAGE);
    }

    /**
     * Generates javascript to set the initial focus to the form element given
     * in the tag's "focus" attribute.
     *
     * @since Struts 1.1
     */
    protected String renderFocusJavascript() {
        StringBuffer results = new StringBuffer();

        results.append(lineEnd);
        results.append("<script type=\"text/javascript\"");

        if (!this.isXhtml() && this.scriptLanguage) {
            results.append(" language=\"JavaScript\"");
        }

        results.append(">");
        results.append(lineEnd);

        // xhtml script content shouldn't use the browser hiding trick
        if (!this.isXhtml()) {
            results.append("  <!--");
            results.append(lineEnd);
        }

        // Construct the control name that will receive focus.
        // This does not include any index.
        StringBuffer focusControl = new StringBuffer("document.forms[\"");

        focusControl.append(beanName);
        focusControl.append("\"].elements[\"");
        focusControl.append(this.focus);
        focusControl.append("\"]");

        results.append("  var focusControl = ");
        results.append(focusControl.toString());
        results.append(";");
        results.append(lineEnd);
        results.append(lineEnd);

        results.append("  if (focusControl.type != \"hidden\" && ");
        results.append("!focusControl.disabled && ");
        results.append("focusControl.style.display != \"none\") {");
        results.append(lineEnd);

        // Construct the index if needed and insert into focus statement
        String index = "";

        if (this.focusIndex != null) {
            StringBuffer sb = new StringBuffer("[");

            sb.append(this.focusIndex);
            sb.append("]");
            index = sb.toString();
        }

        results.append("     focusControl");
        results.append(index);
        results.append(".focus();");
        results.append(lineEnd);

        results.append("  }");
        results.append(lineEnd);

        if (!this.isXhtml()) {
            results.append("  // -->");
            results.append(lineEnd);
        }

        results.append("</script>");
        results.append(lineEnd);

        return results.toString();
    }

    /**
     * Release any acquired resources.
     */
    public void release() {
        super.release();
        action = null;
        moduleConfig = null;
        enctype = null;
        dir = null;
        disabled = false;
        focus = null;
        focusIndex = null;
        lang = null;
        mapping = null;
        method = null;
        onreset = null;
        onsubmit = null;
        readonly = false;
        servlet = null;
        style = null;
        styleClass = null;
        styleId = null;
        target = null;
        acceptCharset = null;
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Look up values for the <code>name</code>, <code>scope</code>, and
     * <code>type</code> properties if necessary.
     *
     * @throws JspException if a required value cannot be looked up
     */
    protected void lookup() throws JspException {

        // Look up the module configuration information we need
        moduleConfig = TagUtils.getInstance().getModuleConfig(pageContext);

        if (moduleConfig == null) {
            JspException e =
                new JspException(messages.getMessage("formTag.collections"));

            pageContext.setAttribute(Globals.EXCEPTION_KEY, e,
                PageContext.REQUEST_SCOPE);
            throw e;
        }

        String calcAction = this.action;

        // If the action is not specified, use the original request uri
        if (this.action == null) {
            HttpServletRequest request =
                (HttpServletRequest) pageContext.getRequest();
            postbackAction =
                (String) request.getAttribute(Globals.ORIGINAL_URI_KEY);

            String prefix = moduleConfig.getPrefix();
            if (postbackAction != null && prefix.length() > 0 && postbackAction.startsWith(prefix)) {
                postbackAction = postbackAction.substring(prefix.length());
            }
            calcAction = postbackAction;
        } else {
            // Translate the action if it is an actionId
            ActionConfig actionConfig = moduleConfig.findActionConfigId(this.action);
            if (actionConfig != null) {
                this.action = actionConfig.getPath();
                calcAction = this.action;
            }
        }

        servlet =
            (ActionServlet) pageContext.getServletContext().getAttribute(Globals.ACTION_SERVLET_KEY);

        // Look up the action mapping we will be submitting to
        String mappingName =
            TagUtils.getInstance().getActionMappingName(calcAction);

        mapping = (ActionMapping) moduleConfig.findActionConfig(mappingName);

        if (mapping == null) {
            JspException e =
                new JspException(messages.getMessage("formTag.mapping",
                        mappingName));

            pageContext.setAttribute(Globals.EXCEPTION_KEY, e,
                PageContext.REQUEST_SCOPE);
            throw e;
        }

        // Look up the form bean definition
        FormBeanConfig formBeanConfig =
            moduleConfig.findFormBeanConfig(mapping.getName());

        if (formBeanConfig == null) {
            JspException e = null;

            if (mapping.getName() == null) {
                e = new JspException(messages.getMessage("formTag.name", calcAction));
            } else {
                e = new JspException(messages.getMessage("formTag.formBean",
                            mapping.getName(), calcAction));
            }

            pageContext.setAttribute(Globals.EXCEPTION_KEY, e,
                PageContext.REQUEST_SCOPE);
            throw e;
        }

        // Calculate the required values
        beanName = mapping.getAttribute();
        beanScope = mapping.getScope();
        beanType = formBeanConfig.getType();
    }

    /**
     * Returns true if this tag should render as xhtml.
     */
    private boolean isXhtml() {
        return TagUtils.getInstance().isXhtml(this.pageContext);
    }

    /**
     * Returns the focusIndex.
     *
     * @return String
     */
    public String getFocusIndex() {
        return focusIndex;
    }

    /**
     * Sets the focusIndex.
     *
     * @param focusIndex The focusIndex to set
     */
    public void setFocusIndex(String focusIndex) {
        this.focusIndex = focusIndex;
    }

    /**
     * Gets whether or not the focus script's &lt;script&gt; element will
     * include the language attribute.
     *
     * @return true if language attribute will be included.
     * @since Struts 1.2
     */
    public boolean getScriptLanguage() {
        return this.scriptLanguage;
    }

    /**
     * Sets whether or not the focus script's &lt;script&gt; element will
     * include the language attribute.
     *
     * @since Struts 1.2
     */
    public void setScriptLanguage(boolean scriptLanguage) {
        this.scriptLanguage = scriptLanguage;
    }
}
