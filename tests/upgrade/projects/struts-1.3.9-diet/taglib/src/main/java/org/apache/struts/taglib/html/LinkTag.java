/*
 * $Id: LinkTag.java 519563 2007-03-18 05:55:07Z pbenedict $
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

import org.apache.struts.taglib.TagUtils;
import org.apache.struts.util.MessageResources;

import javax.servlet.jsp.JspException;

import java.net.MalformedURLException;

import java.util.HashMap;
import java.util.Map;

/**
 * Generate a URL-encoded hyperlink to the specified URI.
 *
 * @version $Rev: 519563 $ $Date: 2005-04-06 02:37:00 -0400 (Wed, 06 Apr 2005)
 *          $
 */
public class LinkTag extends BaseHandlerTag {
    /**
     * The message resources for this package.
     */
    protected static MessageResources messages =
        MessageResources.getMessageResources(Constants.Package
            + ".LocalStrings");

    // ----------------------------------------------------- Instance Variables

    /**
     * The body content of this tag (if any).
     */
    protected String text = null;

    // ------------------------------------------------------------- Properties

    /**
     * The anchor to be added to the end of the generated hyperlink.
     */
    protected String anchor = null;

    /**
     * <p>The logical forward name from which to retrieve the hyperlink
     * URI.</p> <p>Usage note: If a forward config is used in a hyperlink, and
     * a module is specified, the path must lead to another action and not
     * directly to a page. This is in keeping with rule that in a modular
     * application all links must be to an action rather than a page. </p>
     */
    protected String forward = null;

    /**
     * The hyperlink URI.
     */
    protected String href = null;

    /**
     * The link name for named links.
     */
    protected String linkName = null;

    /**
     * The JSP bean name for query parameters.
     */
    protected String name = null;

    /**
     * The module-relative page URL (beginning with a slash) to which this
     * hyperlink will be rendered.
     */
    protected String page = null;

    /**
     * The module-relative action (beginning with a slash) which will be
     * called by this link
     */
    protected String action = null;

    /**
     * The module prefix (beginning with a slash) which will be used to find
     * the action for this link.
     */
    protected String module = null;

    /**
     * The single-parameter request parameter name to generate.
     */
    protected String paramId = null;

    /**
     * The single-parameter JSP bean name.
     */
    protected String paramName = null;

    /**
     * The single-parameter JSP bean property.
     */
    protected String paramProperty = null;

    /**
     * The single-parameter JSP bean scope.
     */
    protected String paramScope = null;

    /**
     * The JSP bean property name for query parameters.
     */
    protected String property = null;

    /**
     * The scope of the bean specified by the name property, if any.
     */
    protected String scope = null;

    /**
     * The window target.
     */
    protected String target = null;

    /**
     * Include transaction token (if any) in the hyperlink?
     */
    protected boolean transaction = false;

    /**
     * Additional parameters included programatically.
     */
    protected Map parameters = new HashMap();

    /**
     * Name of parameter to generate to hold index number
     */
    protected String indexId = null;
    protected boolean useLocalEncoding = false;

    // ----------------------------------------------------- Constructor
    public LinkTag() {
        super();
        doDisabled = false;
    }

    public String getAnchor() {
        return (this.anchor);
    }

    public void setAnchor(String anchor) {
        this.anchor = anchor;
    }

    public String getForward() {
        return (this.forward);
    }

    public void setForward(String forward) {
        this.forward = forward;
    }

    public String getHref() {
        return (this.href);
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getLinkName() {
        return (this.linkName);
    }

    public void setLinkName(String linkName) {
        this.linkName = linkName;
    }

    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPage() {
        return (this.page);
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getAction() {
        return (this.action);
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getModule() {
        return (this.module);
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getParamId() {
        return (this.paramId);
    }

    public void setParamId(String paramId) {
        this.paramId = paramId;
    }

    public String getParamName() {
        return (this.paramName);
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    public String getParamProperty() {
        return (this.paramProperty);
    }

    public void setParamProperty(String paramProperty) {
        this.paramProperty = paramProperty;
    }

    public String getParamScope() {
        return (this.paramScope);
    }

    public void setParamScope(String paramScope) {
        this.paramScope = paramScope;
    }

    public String getProperty() {
        return (this.property);
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getScope() {
        return (this.scope);
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getTarget() {
        return (this.target);
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public boolean getTransaction() {
        return (this.transaction);
    }

    public void setTransaction(boolean transaction) {
        this.transaction = transaction;
    }

    public String getIndexId() {
        return (this.indexId);
    }

    public void setIndexId(String indexId) {
        this.indexId = indexId;
    }

    public boolean isUseLocalEncoding() {
        return useLocalEncoding;
    }

    public void setUseLocalEncoding(boolean b) {
        useLocalEncoding = b;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Render the beginning of the hyperlink. <p> Support for indexed property
     * since Struts 1.1
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        this.text = null;
        this.parameters.clear();

        // Evaluate the body of this tag
        return (EVAL_BODY_TAG);
    }

    /**
     * Save the associated label from the body content.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doAfterBody() throws JspException {
        if (bodyContent != null) {
            String value = bodyContent.getString().trim();

            if (value.length() > 0) {
                text = value;
            }
        }

        return (SKIP_BODY);
    }

    /**
     * Render the end of the hyperlink.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doEndTag() throws JspException {
        // Generate the opening anchor element
        StringBuffer results = new StringBuffer("<a");

        // Special case for name anchors
        prepareAttribute(results, "name", getLinkName());

        // * @since Struts 1.1
        if ((getLinkName() == null) || (getForward() != null)
            || (getHref() != null) || (getPage() != null)
            || (getAction() != null)) {
            prepareAttribute(results, "href", calculateURL());
        }

        prepareAttribute(results, "target", getTarget());
        prepareAttribute(results, "accesskey", getAccesskey());
        prepareAttribute(results, "tabindex", getTabindex());
        results.append(prepareStyles());
        results.append(prepareEventHandlers());
        prepareOtherAttributes(results);
        results.append(">");

        // Prepare the textual content and ending element of this hyperlink
        if (text != null) {
            results.append(text);
        }
        results.append("</a>");
        TagUtils.getInstance().write(pageContext, results.toString());

        return (EVAL_PAGE);
    }

    /**
     * Release any acquired resources.
     */
    public void release() {
        super.release();
        anchor = null;
        forward = null;
        href = null;
        linkName = null;
        name = null;
        page = null;
        action = null;
        module = null;
        paramId = null;
        paramName = null;
        paramProperty = null;
        paramScope = null;
        parameters.clear();
        property = null;
        scope = null;
        target = null;
        text = null;
        transaction = false;
        indexId = null;
        useLocalEncoding = false;
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Return the complete URL to which this hyperlink will direct the user.
     * Support for indexed property since Struts 1.1
     *
     * @throws JspException if an exception is thrown calculating the value
     */
    protected String calculateURL()
        throws JspException {
        // Identify the parameters we will add to the completed URL
        Map params =
            TagUtils.getInstance().computeParameters(pageContext, paramId,
                paramName, paramProperty, paramScope, name, property, scope,
                transaction);

        // Add parameters collected from the tag's inner body
        if (!this.parameters.isEmpty()) {
            if (params == null) {
                params = new HashMap();
            }
            params.putAll(this.parameters);
        }

        // if "indexed=true", add "index=x" parameter to query string
        // * @since Struts 1.1
        if (indexed) {
            int indexValue = getIndexValue();

            //calculate index, and add as a parameter
            if (params == null) {
                params = new HashMap(); //create new HashMap if no other params
            }

            if (indexId != null) {
                params.put(indexId, Integer.toString(indexValue));
            } else {
                params.put("index", Integer.toString(indexValue));
            }
        }

        String url = null;

        try {
            url = TagUtils.getInstance().computeURLWithCharEncoding(pageContext,
                    forward, href, page, action, module, params, anchor, false,
                    useLocalEncoding);
        } catch (MalformedURLException e) {
            TagUtils.getInstance().saveException(pageContext, e);
            throw new JspException(messages.getMessage("rewrite.url",
                    e.toString()));
        }

        return (url);
    }

    /**
     * <p>Adds a parameter to this link.</p>
     *
     * @param paramName the parameter name
     * @param paramValue the parameter value
     * @since Struts 1.3.6
     */
    public void addParameter(String paramName, Object paramValue) {
        this.parameters.put(paramName, paramValue);
    }
}
