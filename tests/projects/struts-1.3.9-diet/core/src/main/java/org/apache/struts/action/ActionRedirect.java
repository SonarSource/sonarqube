/*
 * $Id: ActionRedirect.java 513602 2007-03-02 02:50:23Z pbenedict $
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
package org.apache.struts.action;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.config.ForwardConfig;
import org.apache.struts.util.ResponseUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <p> A subclass of {@link ActionForward} which is designed for use in
 * redirecting requests, with support for adding parameters at runtime. <br/>
 * An {@link ForwardConfig} (or subclass) can be passed to the constructor to
 * copy its configuration: </p> <p>
 * <pre>
 * public ActionForward execute(ActionMapping mapping,
 *                              ActionForm form,
 *                              HttpServletRequest request,
 *                              HttpServletResponse response)
 *         throws Exception {
 *     ActionRedirect redirect =
 *             new ActionRedirect(mapping.findForward("doRedirect"));
 *     redirect.addParameter("param1","value1");
 *     redirect.addParameter("param2","2");
 *     redirect.addParameter("param3","3.0");
 *     return redirect;
 * }
 * </pre>
 * </p>
 *
 * @version $Rev: 513602 $ $Date: 2007-03-02 03:50:23 +0100 (Fri, 02 Mar 2007) $
 */
public class ActionRedirect extends ActionForward {
    // ----------------------------------------------------- Manifest constants

    /**
     * <p>Default allocation size for string buffers.</p>
     */
    private static final int DEFAULT_BUFFER_SIZE = 256;

    // ----------------------------------------------------- Static variables

    /**
     * <p>Commons logging instance.</p>
     */
    protected static final Log LOG = LogFactory.getLog(ActionRedirect.class);

    // ----------------------------------------------------- Instance variables

    /**
     * <p>Holds the redirect parameters. Each entry is either a String or a
     * String[] depending on whether it has one or more entries.</p>
     */
    protected Map parameterValues = null;

    /**
     * <p>Holds the anchor value.</p>
     */
    protected String anchorValue = null;

    // ----------------------------------------------------- Constructors

    /**
     * <p>Construct a new instance with redirect set to true and initialize
     * parameter lists.</p>
     */
    public ActionRedirect() {
        setRedirect(true);
        initializeParameters();
    }

    /**
     * <p>Construct a new instance with the specified path and initialize
     * parameter lists.</p>
     *
     * @param path Path for this instance
     */
    public ActionRedirect(String path) {
        super(path);
        setRedirect(true);
        initializeParameters();
    }

    /**
     * <p>Construct a new instance with the specified values and initialize
     * parameter lists.</p>
     *
     * @param name   Name of this instance
     * @param path   Path for this instance
     * @param module Module prefix, if any
     */
    public ActionRedirect(String name, String path, String module) {
        super(name, path, true);
        setModule(module);
        initializeParameters();
    }

    /**
     * <p>Construct a new instance with a {@link ForwardConfig} object to copy
     * name, path, contextRelative, and arbitrary property values from.</p>
     *
     * @param baseConfig the {@link ForwardConfig} to copy configuration
     *                   values from
     */
    public ActionRedirect(ForwardConfig baseConfig) {
        setName(baseConfig.getName());
        setPath(baseConfig.getPath());
        setModule(baseConfig.getModule());
        setRedirect(true);
        inheritProperties(baseConfig);
        initializeParameters();
    }

    // ----------------------------------------------------- Private methods

    /**
     * <p>Initializes the internal objects used to hold parameter values.</p>
     */
    private void initializeParameters() {
        parameterValues = new HashMap();
    }

    // ----------------------------------------------------- Public methods

    /**
     * <p>Adds the object's toString() to the list of parameters if it's not
     * null, or an empty string with the given fieldName if it is.</p>
     *
     * @param fieldName the name to use for the parameter
     * @param valueObj  the value for this parameter
     * @return The ActionRedirect instance this method is called on
     */
    public ActionRedirect addParameter(String fieldName, Object valueObj) {
        String value = (valueObj != null) ? valueObj.toString() : "";

        if (parameterValues == null) {
            initializeParameters();
        }

        //try {
        value = ResponseUtils.encodeURL(value);

        //} catch (UnsupportedEncodingException uce) {
        // this shouldn't happen since UTF-8 is the W3C Recommendation
        //     String errorMsg = "UTF-8 Character Encoding not supported";
        //     LOG.error(errorMsg, uce);
        //     throw new RuntimeException(errorMsg, uce);
        // }
        Object currentValue = parameterValues.get(fieldName);

        if (currentValue == null) {
            // there's no value for this param yet; add it to the map
            parameterValues.put(fieldName, value);
        } else if (currentValue instanceof String) {
            // there's already a value; let's use an array for these parameters
            String[] newValue = new String[2];

            newValue[0] = (String) currentValue;
            newValue[1] = value;
            parameterValues.put(fieldName, newValue);
        } else if (currentValue instanceof String[]) {
            // add the value to the list of existing values
            List newValues =
                new ArrayList(Arrays.asList((Object[]) currentValue));

            newValues.add(value);
            parameterValues.put(fieldName,
                newValues.toArray(new String[newValues.size()]));
        }
        return this;
    }

    /**
     * <p>Adds an anchor to the path.  Technically, the anchor value is
     * just stored for later and will be added to the path in getPath().
     * Note that this is a considerably simpler method than the
     * addParmaeter method because aside from encoding the value, there
     * isn't really anything to do.  Passing in null is fine because that
     * is the value that will be checked for later to determine whether
     * to append an anchor to the path or not.</p>
     *
     * @param anchorValue The anchor to append to the path
     * @return The ActionRefirect instance this method is called on
     */
    public ActionRedirect setAnchor(String anchorValue) {
        this.anchorValue = ResponseUtils.encodeURL(anchorValue);
        return this;
    }

    /**
     * <p>Get the original path without the parameters added at runtime.</p>
     *
     * @return the original path as configured.
     */
    public String getOriginalPath() {
        return super.getPath();
    }

    /**
     * <p>Get the path for this object, including any parameters that may have
     * been added at runtime.</p>
     *
     * @return The path for this object.
     */
    public String getPath() {
        // get the original path and the parameter string that was formed
        String originalPath = getOriginalPath();
        String parameterString = getParameterString();
        String anchorString = getAnchorString();

        StringBuffer result = new StringBuffer(originalPath);

        if ((parameterString != null) && (parameterString.length() > 0)) {
            // the parameter separator we're going to use
            String paramSeparator = "?";

            // true if we need to use a parameter separator after originalPath
            boolean needsParamSeparator = true;

            // does the original path already have a "?"?
            int paramStartIndex = originalPath.indexOf("?");

            if (paramStartIndex > 0) {
                // did the path end with "?"?
                needsParamSeparator = (paramStartIndex != (originalPath.length()
                    - 1));

                if (needsParamSeparator) {
                    paramSeparator = "&";
                }
            }

            if (needsParamSeparator) {
                result.append(paramSeparator);
            }

            result.append(parameterString);
        }

        // append anchor string (or blank if none was set)
        result.append(anchorString);


        return result.toString();
    }

    /**
     * <p>Forms the string containing the parameters
     *  passed onto this object thru calls to addParameter().</p>
     *
     * @return a string which can be appended to the URLs.  The
     *    return string includes a leading hash
     *    mark (#).
     */
    public String getAnchorString() {
        String retVal = "";
        if (anchorValue != null) {
            retVal = "#" + anchorValue;
        }
        return retVal;
    }

    /**
     * <p>Forms the string containing the parameters passed onto this object
     * thru calls to addParameter().</p>
     *
     * @return a string which can be appended to the URLs.  The return string
     *         does not include a leading question mark (?).
     */
    public String getParameterString() {
        StringBuffer strParam = new StringBuffer(DEFAULT_BUFFER_SIZE);

        // loop through all parameters
        Iterator iterator = parameterValues.keySet().iterator();

        while (iterator.hasNext()) {
            // get the parameter name
            String paramName = (String) iterator.next();

            // get the value for this parameter
            Object value = parameterValues.get(paramName);

            if (value instanceof String) {
                // just one value for this param
                strParam.append(paramName).append("=").append(value);
            } else if (value instanceof String[]) {
                // loop through all values for this param
                String[] values = (String[]) value;

                for (int i = 0; i < values.length; i++) {
                    strParam.append(paramName).append("=").append(values[i]);

                    if (i < (values.length - 1)) {
                        strParam.append("&");
                    }
                }
            }

            if (iterator.hasNext()) {
                strParam.append("&");
            }
        }

        return strParam.toString();
    }

    // ----------------------------------------------------- toString()

    /**
     * <p>Return a string description of this object.</p>
     *
     * @return a string containing the original path for this object and the
     *         parameters it currently holds
     */
    public String toString() {
        StringBuffer result = new StringBuffer(DEFAULT_BUFFER_SIZE);

        result.append("ActionRedirect [");
        result.append("originalPath=").append(getOriginalPath()).append(";");
        result.append("parameterString=").append(getParameterString()).append("]");
        result.append("anchorString=").append(getAnchorString()).append("]");

        return result.toString();
    }
}
