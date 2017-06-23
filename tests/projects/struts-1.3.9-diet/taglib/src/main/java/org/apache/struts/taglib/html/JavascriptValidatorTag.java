/*
 * $Id: JavascriptValidatorTag.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.commons.validator.Field;
import org.apache.commons.validator.Form;
import org.apache.commons.validator.ValidatorAction;
import org.apache.commons.validator.ValidatorResources;
import org.apache.commons.validator.Var;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.taglib.TagUtils;
import org.apache.struts.util.MessageResources;
import org.apache.struts.validator.Resources;
import org.apache.struts.validator.ValidatorPlugIn;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Custom tag that generates JavaScript for client side validation based on
 * the validation rules loaded by the <code>ValidatorPlugIn</code> defined in
 * the struts-config.xml file.
 *
 * @version $Rev: 471754 $ $Date: 2006-11-06 15:55:09 +0100 (Mon, 06 Nov 2006) $
 * @since Struts 1.1
 */
public class JavascriptValidatorTag extends BodyTagSupport {
    /**
     * A Comparator to use when sorting ValidatorAction objects.
     */
    private static final Comparator actionComparator =
        new Comparator() {
            public int compare(Object o1, Object o2) {
                ValidatorAction va1 = (ValidatorAction) o1;
                ValidatorAction va2 = (ValidatorAction) o2;

                if (((va1.getDepends() == null)
                    || (va1.getDepends().length() == 0))
                    && ((va2.getDepends() == null)
                    || (va2.getDepends().length() == 0))) {
                    return 0;
                } else if (((va1.getDepends() != null)
                    && (va1.getDepends().length() > 0))
                    && ((va2.getDepends() == null)
                    || (va2.getDepends().length() == 0))) {
                    return 1;
                } else if (((va1.getDepends() == null)
                    || (va1.getDepends().length() == 0))
                    && ((va2.getDepends() != null)
                    && (va2.getDepends().length() > 0))) {
                    return -1;
                } else {
                    return va1.getDependencyList().size()
                    - va2.getDependencyList().size();
                }
            }
        };

    /**
     * The start of the HTML comment hiding JavaScript from old browsers.
     *
     * @since Struts 1.2
     */
    protected static final String HTML_BEGIN_COMMENT = "\n<!-- Begin \n";

    /**
     * The end of the HTML comment hiding JavaScript from old browsers.
     *
     * @since Struts 1.2
     */
    protected static final String HTML_END_COMMENT = "//End --> \n";

    /**
     * The line ending string.
     */
    protected static String lineEnd = System.getProperty("line.separator");

    // ----------------------------------------------------------- Properties

    /**
     * The servlet context attribute key for our resources.
     */
    protected String bundle = Globals.MESSAGES_KEY;

    /**
     * The name of the form that corresponds with the action name in
     * struts-config.xml. Specifying a form name places a &lt;script&gt;
     * &lt;/script&gt; around the javascript.
     */
    protected String formName = null;

    /**
     * formName is used for both Javascript and non-javascript validations.
     * For the javascript validations, there is the possibility that we will
     * be rewriting the formName (if it is a ValidatorActionForm instead of
     * just a ValidatorForm) so we need another variable to hold the formName
     * just for javascript usage.
     */
    protected String jsFormName = null;

    /**
     * The current page number of a multi-part form. Only valid when the
     * formName attribute is set.
     */
    protected int page = 0;

    /**
     * This will be used as is for the JavaScript validation method name if it
     * has a value.  This is the method name of the main JavaScript method
     * that the form calls to perform validations.
     */
    protected String methodName = null;

    /**
     * Include language attribute in the &lt;script&gt; element.  This
     * property is ignored in XHTML mode.
     *
     * @since Struts 1.2
     */
    protected boolean scriptLanguage = true;

    /**
     * The static JavaScript methods will only be printed if this is set to
     * "true".
     */
    protected String staticJavascript = "true";

    /**
     * The dynamic JavaScript objects will only be generated if this is set to
     * "true".
     */
    protected String dynamicJavascript = "true";

    /**
     * The src attribute for html script element (used to include an external
     * script resource). The src attribute is only recognized when the
     * formName attribute is specified.
     */
    protected String src = null;

    /**
     * The JavaScript methods will enclosed with html comments if this is set
     * to "true".
     */
    protected String htmlComment = "true";

    /**
     * Hide JavaScript methods in a CDATA section for XHTML when "true".
     */
    protected String cdata = "true";

    /**
     * Gets the key (form name) that will be used to retrieve a set of
     * validation rules to be performed on the bean passed in for validation.
     */
    public String getFormName() {
        return formName;
    }

    /**
     * Sets the key (form name) that will be used to retrieve a set of
     * validation rules to be performed on the bean passed in for validation.
     * Specifying a form name places a &lt;script&gt; &lt;/script&gt; tag
     * around the javascript.
     */
    public void setFormName(String formName) {
        this.formName = formName;
    }

    /**
     * @return Returns the jsFormName.
     */
    public String getJsFormName() {
        return jsFormName;
    }

    /**
     * @param jsFormName The jsFormName to set.
     */
    public void setJsFormName(String jsFormName) {
        this.jsFormName = jsFormName;
    }

    /**
     * Gets the current page number of a multi-part form. Only field
     * validations with a matching page numer will be generated that match the
     * current page number. Only valid when the formName attribute is set.
     */
    public int getPage() {
        return page;
    }

    /**
     * Sets the current page number of a multi-part form. Only field
     * validations with a matching page numer will be generated that match the
     * current page number. Only valid when the formName attribute is set.
     */
    public void setPage(int page) {
        this.page = page;
    }

    /**
     * Gets the method name that will be used for the Javascript validation
     * method name if it has a value.  This overrides the auto-generated
     * method name based on the key (form name) passed in.
     */
    public String getMethod() {
        return methodName;
    }

    /**
     * Sets the method name that will be used for the Javascript validation
     * method name if it has a value.  This overrides the auto-generated
     * method name based on the key (form name) passed in.
     */
    public void setMethod(String methodName) {
        this.methodName = methodName;
    }

    /**
     * Gets whether or not to generate the static JavaScript.  If this is set
     * to 'true', which is the default, the static JavaScript will be
     * generated.
     */
    public String getStaticJavascript() {
        return staticJavascript;
    }

    /**
     * Sets whether or not to generate the static JavaScript.  If this is set
     * to 'true', which is the default, the static JavaScript will be
     * generated.
     */
    public void setStaticJavascript(String staticJavascript) {
        this.staticJavascript = staticJavascript;
    }

    /**
     * Gets whether or not to generate the dynamic JavaScript.  If this is set
     * to 'true', which is the default, the dynamic JavaScript will be
     * generated.
     */
    public String getDynamicJavascript() {
        return dynamicJavascript;
    }

    /**
     * Sets whether or not to generate the dynamic JavaScript.  If this is set
     * to 'true', which is the default, the dynamic JavaScript will be
     * generated.
     */
    public void setDynamicJavascript(String dynamicJavascript) {
        this.dynamicJavascript = dynamicJavascript;
    }

    /**
     * Gets whether or not to delimit the JavaScript with html comments.  If
     * this is set to 'true', which is the default, the htmlComment will be
     * surround the JavaScript.
     */
    public String getHtmlComment() {
        return htmlComment;
    }

    /**
     * Sets whether or not to delimit the JavaScript with html comments.  If
     * this is set to 'true', which is the default, the htmlComment will be
     * surround the JavaScript.
     */
    public void setHtmlComment(String htmlComment) {
        this.htmlComment = htmlComment;
    }

    /**
     * Gets the src attribute's value when defining the html script element.
     */
    public String getSrc() {
        return src;
    }

    /**
     * Sets the src attribute's value when defining the html script element.
     * The src attribute is only recognized when the formName attribute is
     * specified.
     */
    public void setSrc(String src) {
        this.src = src;
    }

    /**
     * Sets the servlet context attribute key for our resources.
     */
    public String getBundle() {
        return bundle;
    }

    /**
     * Gets the servlet context attribute key for our resources.
     */
    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    /**
     * Render the JavaScript for to perform validations based on the form
     * name.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        JspWriter writer = pageContext.getOut();

        try {
            writer.print(this.renderJavascript());
        } catch (IOException e) {
            throw new JspException(e.getMessage());
        }

        return EVAL_BODY_TAG;
    }

    /**
     * Returns fully rendered JavaScript.
     *
     * @since Struts 1.2
     */
    protected String renderJavascript()
        throws JspException {
        StringBuffer results = new StringBuffer();

        ModuleConfig config =
            TagUtils.getInstance().getModuleConfig(pageContext);
        ValidatorResources resources =
            (ValidatorResources) pageContext.getAttribute(
              ValidatorPlugIn.VALIDATOR_KEY
                + config.getPrefix(), PageContext.APPLICATION_SCOPE);

        if (resources == null) {
            throw new JspException(
                "ValidatorResources not found in application scope under key \""
                + ValidatorPlugIn.VALIDATOR_KEY + config.getPrefix() + "\"");
        }

        Locale locale =
            TagUtils.getInstance().getUserLocale(this.pageContext, null);

        Form form = null;
        if ("true".equalsIgnoreCase(dynamicJavascript)) {
            form = resources.getForm(locale, formName);
            if (form == null) {
                throw new JspException("No form found under '" + formName
                    + "' in locale '" + locale
                    + "'.  A form must be defined in the "
                    + "Commons Validator configuration when "
                    + "dynamicJavascript=\"true\" is set.");
            }
        }

        if (form != null) {
            if ("true".equalsIgnoreCase(dynamicJavascript)) {
                results.append(this.createDynamicJavascript(config, resources,
                        locale, form));
            } else if ("true".equalsIgnoreCase(staticJavascript)) {
                results.append(this.renderStartElement());

                if ("true".equalsIgnoreCase(htmlComment)) {
                    results.append(HTML_BEGIN_COMMENT);
                }
            }
        }

        if ("true".equalsIgnoreCase(staticJavascript)) {
            results.append(getJavascriptStaticMethods(resources));
        }

        if ((form != null)
            && ("true".equalsIgnoreCase(dynamicJavascript)
            || "true".equalsIgnoreCase(staticJavascript))) {
            results.append(getJavascriptEnd());
        }

        return results.toString();
    }

    /**
     * Generates the dynamic JavaScript for the form.
     *
     * @param config
     * @param resources
     * @param locale
     * @param form
     */
    private String createDynamicJavascript(ModuleConfig config,
        ValidatorResources resources, Locale locale, Form form)
        throws JspException {
        StringBuffer results = new StringBuffer();

        MessageResources messages =
            TagUtils.getInstance().retrieveMessageResources(pageContext,
                bundle, true);

        HttpServletRequest request =
            (HttpServletRequest) pageContext.getRequest();
        ServletContext application = pageContext.getServletContext();

        List actions = this.createActionList(resources, form);

        final String methods =
            this.createMethods(actions, this.stopOnError(config));

        String formName = form.getName();

        jsFormName = formName;

        if (jsFormName.charAt(0) == '/') {
            String mappingName =
                TagUtils.getInstance().getActionMappingName(jsFormName);
            ActionMapping mapping =
                (ActionMapping) config.findActionConfig(mappingName);

            if (mapping == null) {
                JspException e =
                    new JspException(messages.getMessage("formTag.mapping",
                            mappingName));

                pageContext.setAttribute(Globals.EXCEPTION_KEY, e,
                    PageContext.REQUEST_SCOPE);
                throw e;
            }

            jsFormName = mapping.getAttribute();
        }

        results.append(this.getJavascriptBegin(methods));

        for (Iterator i = actions.iterator(); i.hasNext();) {
            ValidatorAction va = (ValidatorAction) i.next();
            int jscriptVar = 0;
            String functionName = null;

            if ((va.getJsFunctionName() != null)
                && (va.getJsFunctionName().length() > 0)) {
                functionName = va.getJsFunctionName();
            } else {
                functionName = va.getName();
            }

            results.append("    function " + jsFormName + "_" + functionName
                + " () { \n");

            for (Iterator x = form.getFields().iterator(); x.hasNext();) {
                Field field = (Field) x.next();

                // Skip indexed fields for now until there is a good way to
                // handle error messages (and the length of the list (could
                // retrieve from scope?))
                if (field.isIndexed() || (field.getPage() != page)
                    || !field.isDependency(va.getName())) {
                    continue;
                }

                String message =
                    Resources.getMessage(application, request, messages,
                        locale, va, field);

                message = (message != null) ? message : "";

                // prefix variable with 'a' to make it a legal identifier
                results.append("     this.a" + jscriptVar++ + " = new Array(\""
                    + field.getKey() + "\", \"" + escapeQuotes(message)
                    + "\", ");

                results.append("new Function (\"varName\", \"");

                Map vars = field.getVars();

                // Loop through the field's variables.
                Iterator varsIterator = vars.keySet().iterator();

                while (varsIterator.hasNext()) {
                    String varName = (String) varsIterator.next();
                    Var var = (Var) vars.get(varName);
                    String varValue =
                        Resources.getVarValue(var, application, request, false);
                    String jsType = var.getJsType();

                    // skip requiredif variables field, fieldIndexed, fieldTest,
                    // fieldValue
                    if (varName.startsWith("field")) {
                        continue;
                    }

                    String varValueEscaped = escapeJavascript(varValue);

                    if (Var.JSTYPE_INT.equalsIgnoreCase(jsType)) {
                        results.append("this." + varName + "="
                            + varValueEscaped + "; ");
                    } else if (Var.JSTYPE_REGEXP.equalsIgnoreCase(jsType)) {
                        results.append("this." + varName + "=/"
                            + varValueEscaped + "/; ");
                    } else if (Var.JSTYPE_STRING.equalsIgnoreCase(jsType)) {
                        results.append("this." + varName + "='"
                            + varValueEscaped + "'; ");

                        // So everyone using the latest format doesn't need to
                        // change their xml files immediately.
                    } else if ("mask".equalsIgnoreCase(varName)) {
                        results.append("this." + varName + "=/"
                            + varValueEscaped + "/; ");
                    } else {
                        results.append("this." + varName + "='"
                            + varValueEscaped + "'; ");
                    }
                }

                results.append(" return this[varName];\"));\n");
            }

            results.append("    } \n\n");
        }

        return results.toString();
    }

    private String escapeQuotes(String in) {
        if ((in == null) || (in.indexOf("\"") == -1)) {
            return in;
        }

        StringBuffer buffer = new StringBuffer();
        StringTokenizer tokenizer = new StringTokenizer(in, "\"", true);

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();

            if (token.equals("\"")) {
                buffer.append("\\");
            }

            buffer.append(token);
        }

        return buffer.toString();
    }

    /**
     * <p>Backslash-escapes the following characters from the input string:
     * &quot;, &apos;, \, \r, \n.</p>
     *
     * <p>This method escapes characters that will result in an invalid
     * Javascript statement within the validator Javascript.</p>
     *
     * @param str The string to escape.
     * @return The string <code>s</code> with each instance of a double quote,
     *         single quote, backslash, carriage-return, or line feed escaped
     *         with a leading backslash.
     */
    private String escapeJavascript(String str) {
        if (str == null) {
            return null;
        }

        int length = str.length();

        if (length == 0) {
            return str;
        }

        // guess at how many chars we'll be adding...
        StringBuffer out = new StringBuffer(length + 4);

        // run through the string escaping sensitive chars
        for (int i = 0; i < length; i++) {
            char c = str.charAt(i);

            if ((c == '"') || (c == '\'') || (c == '\\') || (c == '\n')
                || (c == '\r')) {
                out.append('\\');
            }

            out.append(c);
        }

        return out.toString();
    }

    /**
     * Determines if validations should stop on an error.
     *
     * @param config The <code>ModuleConfig</code> used to lookup the
     *               stopOnError setting.
     * @return <code>true</code> if validations should stop on errors.
     */
    private boolean stopOnError(ModuleConfig config) {
        Object stopOnErrorObj =
            pageContext.getAttribute(ValidatorPlugIn.STOP_ON_ERROR_KEY + '.'
                + config.getPrefix(), PageContext.APPLICATION_SCOPE);

        boolean stopOnError = true;

        if (stopOnErrorObj instanceof Boolean) {
            stopOnError = ((Boolean) stopOnErrorObj).booleanValue();
        }

        return stopOnError;
    }

    /**
     * Creates the JavaScript methods list from the given actions.
     *
     * @param actions     A List of ValidatorAction objects.
     * @param stopOnError If true, behaves like released version of struts 1.1
     *                    and stops after first error. If false, evaluates all
     *                    validations.
     * @return JavaScript methods.
     */
    private String createMethods(List actions, boolean stopOnError) {
        StringBuffer methods = new StringBuffer();
        final String methodOperator = stopOnError ? " && " : " & ";

        Iterator iter = actions.iterator();

        while (iter.hasNext()) {
            ValidatorAction va = (ValidatorAction) iter.next();

            if (methods.length() > 0) {
                methods.append(methodOperator);
            }

            methods.append(va.getMethod()).append("(form)");
        }

        return methods.toString();
    }

    /**
     * Get List of actions for the given Form.
     *
     * @param resources
     * @param form
     * @return A sorted List of ValidatorAction objects.
     */
    private List createActionList(ValidatorResources resources, Form form) {
        List actionMethods = new ArrayList();

        Iterator iterator = form.getFields().iterator();

        while (iterator.hasNext()) {
            Field field = (Field) iterator.next();

            for (Iterator x = field.getDependencyList().iterator();
                x.hasNext();) {
                Object o = x.next();

                if ((o != null) && !actionMethods.contains(o)) {
                    actionMethods.add(o);
                }
            }
        }

        List actions = new ArrayList();

        // Create list of ValidatorActions based on actionMethods
        iterator = actionMethods.iterator();

        while (iterator.hasNext()) {
            String depends = (String) iterator.next();
            ValidatorAction va = resources.getValidatorAction(depends);

            // throw nicer NPE for easier debugging
            if (va == null) {
                throw new NullPointerException("Depends string \"" + depends
                    + "\" was not found in validator-rules.xml.");
            }

            if ((va.getJavascript() != null)
                && (va.getJavascript().length() > 0)) {
                actions.add(va);
            } else {
                iterator.remove();
            }
        }

        Collections.sort(actions, actionComparator);

        return actions;
    }

    /**
     * Release any acquired resources.
     */
    public void release() {
        super.release();
        bundle = Globals.MESSAGES_KEY;
        formName = null;
        jsFormName = null;
        page = 0;
        methodName = null;
        staticJavascript = "true";
        dynamicJavascript = "true";
        htmlComment = "true";
        cdata = "true";
        src = null;
    }

    /**
     * Returns the opening script element and some initial javascript.
     */
    protected String getJavascriptBegin(String methods) {
        StringBuffer sb = new StringBuffer();
        String name = jsFormName.replace('/', '_'); // remove any '/' characters

        name =
            jsFormName.substring(0, 1).toUpperCase()
            + jsFormName.substring(1, jsFormName.length());

        sb.append(this.renderStartElement());

        if (this.isXhtml() && "true".equalsIgnoreCase(this.cdata)) {
            sb.append("//<![CDATA[\r\n");
        }

        if (!this.isXhtml() && "true".equals(htmlComment)) {
            sb.append(HTML_BEGIN_COMMENT);
        }

        sb.append("\n    var bCancel = false; \n\n");

        if ((methodName == null) || (methodName.length() == 0)) {
            sb.append("    function validate" + name + "(form) { \n");
        } else {
            sb.append("    function " + methodName + "(form) { \n");
        }

        sb.append("        if (bCancel) { \n");
        sb.append("            return true; \n");
        sb.append("        } else { \n");

        // Always return true if there aren't any Javascript validation methods
        if ((methods == null) || (methods.length() == 0)) {
            sb.append("            return true; \n");
        } else {
            sb.append("            var formValidationResult; \n");
            sb.append("            formValidationResult = " + methods + "; \n");
            if (methods.indexOf("&&") >= 0) {
                sb.append("            return (formValidationResult); \n");
            } else {
                //Making Sure that Bitwise operator works:
                sb.append("            return (formValidationResult == 1); \n");
            }
        }
        sb.append("        } \n");
        sb.append("    } \n\n");

        return sb.toString();
    }

    protected String getJavascriptStaticMethods(ValidatorResources resources) {
        StringBuffer sb = new StringBuffer();

        sb.append("\n\n");

        Iterator actions = resources.getValidatorActions().values().iterator();

        while (actions.hasNext()) {
            ValidatorAction va = (ValidatorAction) actions.next();

            if (va != null) {
                String javascript = va.getJavascript();

                if ((javascript != null) && (javascript.length() > 0)) {
                    sb.append(javascript + "\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * Returns the closing script element.
     */
    protected String getJavascriptEnd() {
        StringBuffer sb = new StringBuffer();

        sb.append("\n");

        if (!this.isXhtml() && "true".equals(htmlComment)) {
            sb.append(HTML_END_COMMENT);
        }

        if (this.isXhtml() && "true".equalsIgnoreCase(this.cdata)) {
            sb.append("//]]>\r\n");
        }

        sb.append("</script>\n\n");

        return sb.toString();
    }

    /**
     * Constructs the beginning &lt;script&gt; element depending on XHTML
     * status.
     *
     * @since Struts 1.2
     */
    protected String renderStartElement() {
        StringBuffer start =
            new StringBuffer("<script type=\"text/javascript\"");

        // there is no language attribute in XHTML
        if (!this.isXhtml() && this.scriptLanguage) {
            start.append(" language=\"Javascript1.1\"");
        }

        if (this.src != null) {
            start.append(" src=\"" + src + "\"");
        }

        start.append("> \n");

        return start.toString();
    }

    /**
     * Returns true if this is an xhtml page.
     */
    private boolean isXhtml() {
        return TagUtils.getInstance().isXhtml(this.pageContext);
    }

    /**
     * Returns the cdata setting "true" or "false".
     *
     * @return String - "true" if JavaScript will be hidden in a CDATA
     *         section
     */
    public String getCdata() {
        return cdata;
    }

    /**
     * Sets the cdata status.
     *
     * @param cdata The cdata to set
     */
    public void setCdata(String cdata) {
        this.cdata = cdata;
    }

    /**
     * Gets whether or not the &lt;script&gt; element will include the
     * language attribute.
     *
     * @return true if language attribute will be included.
     * @since Struts 1.2
     */
    public boolean getScriptLanguage() {
        return this.scriptLanguage;
    }

    /**
     * Sets whether or not the &lt;script&gt; element will include the
     * language attribute.
     *
     * @since Struts 1.2
     */
    public void setScriptLanguage(boolean scriptLanguage) {
        this.scriptLanguage = scriptLanguage;
    }
}
