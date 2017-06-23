/*
 * $Id: ImgTag.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.struts.config.ModuleConfig;
import org.apache.struts.taglib.TagUtils;
import org.apache.struts.util.MessageResources;
import org.apache.struts.util.ModuleUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;

import java.util.Iterator;
import java.util.Map;

/**
 * <p>Generate an IMG tag to the specified image URI. </p>
 *
 * <p>TODO:</p>
 *
 * <ul>
 *
 * <li>Make the <strong>alt</strong> and <strong>src</strong> settable from
 * properties (for i18n)</li>
 *
 * </ul>
 *
 * @version $Rev: 471754 $
 */
public class ImgTag extends BaseHandlerTag {
    /**
     * The message resources for this package.
     */
    protected static MessageResources messages =
        MessageResources.getMessageResources(Constants.Package
            + ".LocalStrings");

    // ------------------------------------------------------------- Properties

    /**
     * The property to specify where to align the image.
     */
    protected String align = null;

    /**
     * The border size around the image.
     */
    protected String border = null;

    /**
     * The image height.
     */
    protected String height = null;

    /**
     * The horizontal spacing around the image.
     */
    protected String hspace = null;

    /**
     * The image name for named images.
     */
    protected String imageName = null;

    /**
     * Server-side image map declaration.
     */
    protected String ismap = null;

    /**
     * The JSP bean name for query parameters.
     */
    protected String name = null;

    /**
     * The module-relative path, starting with a slash character, of the image
     * to be displayed by this rendered tag.
     */
    protected String page = null;

    /**
     * The message resources key under which we should look up the
     * <code>page</code> attribute for this generated tag, if any.
     */
    protected String pageKey = null;

    /**
     * The module-relative action (beginning with a slash) which will be used
     * as the source for this image.
     */
    protected String action = null;

    /**
     * The module prefix (beginning with a slash) which will be used to find
     * the action for this link.
     */
    protected String module = null;

    /**
     * In situations where an image is dynamically generated (such as to
     * create a chart graph), this specifies the single-parameter request
     * parameter name to generate.
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
     * The image source URI.
     */
    protected String src = null;

    /**
     * The message resources key under which we should look up the
     * <code>src</code> attribute for this generated tag, if any.
     */
    protected String srcKey = null;

    /**
     * Client-side image map declaration.
     */
    protected String usemap = null;

    /**
     * The vertical spacing around the image.
     */
    protected String vspace = null;

    /**
     * The image width.
     */
    protected String width = null;
    protected boolean useLocalEncoding = false;

    // ----------------------------------------------------- Constructor
    public ImgTag() {
        super();
        doDisabled = false;
    }

    public String getAlign() {
        return (this.align);
    }

    public void setAlign(String align) {
        this.align = align;
    }

    public String getBorder() {
        return (this.border);
    }

    public void setBorder(String border) {
        this.border = border;
    }

    public String getHeight() {
        return (this.height);
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getHspace() {
        return (this.hspace);
    }

    public void setHspace(String hspace) {
        this.hspace = hspace;
    }

    public String getImageName() {
        return (this.imageName);
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getIsmap() {
        return (this.ismap);
    }

    public void setIsmap(String ismap) {
        this.ismap = ismap;
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

    public String getPageKey() {
        return (this.pageKey);
    }

    public void setPageKey(String pageKey) {
        this.pageKey = pageKey;
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

    public String getSrc() {
        return (this.src);
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getSrcKey() {
        return (this.srcKey);
    }

    public void setSrcKey(String srcKey) {
        this.srcKey = srcKey;
    }

    public String getUsemap() {
        return (this.usemap);
    }

    public void setUsemap(String usemap) {
        this.usemap = usemap;
    }

    public String getVspace() {
        return (this.vspace);
    }

    public void setVspace(String vspace) {
        this.vspace = vspace;
    }

    public String getWidth() {
        return (this.width);
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public boolean isUseLocalEncoding() {
        return useLocalEncoding;
    }

    public void setUseLocalEncoding(boolean b) {
        useLocalEncoding = b;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Render the beginning of the IMG tag.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doStartTag() throws JspException {
        // Evaluate the body of this tag
        return (EVAL_BODY_TAG);
    }

    /**
     * Render the end of the IMG tag.
     *
     * @throws JspException if a JSP exception has occurred
     */
    public int doEndTag() throws JspException {
        // Generate the name definition or image element
        HttpServletResponse response =
            (HttpServletResponse) pageContext.getResponse();
        StringBuffer results = new StringBuffer("<img");
        String tmp = src();
        String srcurl = url(tmp);

        if (srcurl != null) {
            prepareAttribute(results, "src", response.encodeURL(srcurl));
        }

        prepareAttribute(results, "name", getImageName());
        prepareAttribute(results, "height", getHeight());
        prepareAttribute(results, "width", getWidth());
        prepareAttribute(results, "align", getAlign());
        prepareAttribute(results, "border", getBorder());
        prepareAttribute(results, "hspace", getHspace());
        prepareAttribute(results, "vspace", getVspace());
        prepareAttribute(results, "ismap", getIsmap());
        prepareAttribute(results, "usemap", getUsemap());
        results.append(prepareStyles());
        results.append(prepareEventHandlers());
        prepareOtherAttributes(results);
        results.append(getElementClose());

        TagUtils.getInstance().write(pageContext, results.toString());

        return (EVAL_PAGE);
    }

    /**
     * Release any acquired resources.
     */
    public void release() {
        super.release();

        border = null;
        height = null;
        hspace = null;
        imageName = null;
        ismap = null;
        name = null;
        page = null;
        pageKey = null;
        action = null;
        paramId = null;
        paramName = null;
        paramProperty = null;
        paramScope = null;
        property = null;
        scope = null;
        src = null;
        srcKey = null;
        usemap = null;
        vspace = null;
        width = null;
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Convenience method to throw a "imgTag.src" exception.
     *
     * @throws JspException
     */
    private void throwImgTagSrcException()
        throws JspException {
        JspException e = new JspException(messages.getMessage("imgTag.src"));

        TagUtils.getInstance().saveException(pageContext, e);
        throw e;
    }

    /**
     * Convenience method to test whether this is the default module.
     *
     * @param config Our Moduleconfig
     * @return True if this is the default module or contextRelative is set
     */
    private boolean srcDefaultReference(ModuleConfig config) {
        return (config == null);
    }

    /**
     * Return the base source URL that will be rendered in the
     * <code>src</code> property for this generated element, or
     * <code>null</code> if there is no such URL.
     *
     * @throws JspException if an error occurs
     */
    protected String src() throws JspException {
        // Deal with a direct context-relative page that has been specified
        if (this.page != null) {
            if ((this.src != null) || (this.srcKey != null)
                || (this.pageKey != null)) {
                throwImgTagSrcException();
            }

            ModuleConfig config =
                ModuleUtils.getInstance().getModuleConfig(this.module,
                    (HttpServletRequest) pageContext.getRequest(),
                    pageContext.getServletContext());

            HttpServletRequest request =
                (HttpServletRequest) pageContext.getRequest();
            String pageValue = this.page;

            if (!srcDefaultReference(config)) {
                pageValue =
                    TagUtils.getInstance().pageURL(request, this.page, config);
            }

            return (request.getContextPath() + pageValue);
        }

        // Deal with an indirect context-relative page that has been specified
        if (this.pageKey != null) {
            if ((this.src != null) || (this.srcKey != null)) {
                throwImgTagSrcException();
            }

            ModuleConfig config =
                ModuleUtils.getInstance().getModuleConfig(this.module,
                    (HttpServletRequest) pageContext.getRequest(),
                    pageContext.getServletContext());

            HttpServletRequest request =
                (HttpServletRequest) pageContext.getRequest();
            String pageValue =
                TagUtils.getInstance().message(pageContext, getBundle(),
                    getLocale(), this.pageKey);

            if (!srcDefaultReference(config)) {
                pageValue =
                    TagUtils.getInstance().pageURL(request, pageValue, config);
            }

            return (request.getContextPath() + pageValue);
        }

        if (this.action != null) {
            if ((this.src != null) || (this.srcKey != null)) {
                throwImgTagSrcException();
            }

            return TagUtils.getInstance().getActionMappingURL(action, module,
                pageContext, false);
        }

        // Deal with an absolute source that has been specified
        if (this.src != null) {
            if (this.srcKey != null) {
                throwImgTagSrcException();
            }

            return (this.src);
        }

        // Deal with an indirect source that has been specified
        if (this.srcKey == null) {
            throwImgTagSrcException();
        }

        return TagUtils.getInstance().message(pageContext, getBundle(),
            getLocale(), this.srcKey);
    }

    /**
     * Return the specified src URL, modified as necessary with optional
     * request parameters.
     *
     * @param url The URL to be modified (or null if this url will not be
     *            used)
     * @throws JspException if an error occurs preparing the URL
     */
    protected String url(String url)
        throws JspException {
        if (url == null) {
            return (url);
        }

        String charEncoding = "UTF-8";

        if (useLocalEncoding) {
            charEncoding = pageContext.getResponse().getCharacterEncoding();
        }

        // Start with an unadorned URL as specified
        StringBuffer src = new StringBuffer(url);

        // Append a single-parameter name and value, if requested
        if ((paramId != null) && (paramName != null)) {
            if (src.toString().indexOf('?') < 0) {
                src.append('?');
            } else {
                src.append("&amp;");
            }

            src.append(paramId);
            src.append('=');

            Object value =
                TagUtils.getInstance().lookup(pageContext, paramName,
                    paramProperty, paramScope);

            if (value != null) {
                src.append(TagUtils.getInstance().encodeURL(value.toString(),
                        charEncoding));
            }
        }

        // Just return the URL if there is no bean to look up
        if ((property != null) && (name == null)) {
            JspException e =
                new JspException(messages.getMessage("getter.name"));

            TagUtils.getInstance().saveException(pageContext, e);
            throw e;
        }

        if (name == null) {
            return (src.toString());
        }

        // Look up the map we will be using
        Object mapObject =
            TagUtils.getInstance().lookup(pageContext, name, property, scope);
        Map map = null;

        try {
            map = (Map) mapObject;
        } catch (ClassCastException e) {
            TagUtils.getInstance().saveException(pageContext, e);
            throw new JspException(messages.getMessage("imgTag.type"));
        }

        // Append the required query parameters
        boolean question = (src.toString().indexOf("?") >= 0);
        Iterator keys = map.keySet().iterator();

        while (keys.hasNext()) {
            String key = (String) keys.next();
            Object value = map.get(key);

            if (value == null) {
                if (question) {
                    src.append("&amp;");
                } else {
                    src.append('?');
                    question = true;
                }

                src.append(key);
                src.append('=');

                // Interpret null as "no value specified"
            } else if (value instanceof String[]) {
                String[] values = (String[]) value;

                for (int i = 0; i < values.length; i++) {
                    if (question) {
                        src.append("&amp;");
                    } else {
                        src.append('?');
                        question = true;
                    }

                    src.append(key);
                    src.append('=');
                    src.append(TagUtils.getInstance().encodeURL(values[i],
                            charEncoding));
                }
            } else {
                if (question) {
                    src.append("&amp;");
                } else {
                    src.append('?');
                    question = true;
                }

                src.append(key);
                src.append('=');
                src.append(TagUtils.getInstance().encodeURL(value.toString(),
                        charEncoding));
            }
        }

        // Return the final result
        return (src.toString());
    }
}
