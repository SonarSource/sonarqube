/*
 * $Id: ModuleUtils.java 471754 2006-11-06 14:55:09Z husted $
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
package org.apache.struts.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.Globals;
import org.apache.struts.action.RequestProcessor;
import org.apache.struts.config.MessageResourcesConfig;
import org.apache.struts.config.ModuleConfig;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * General purpose utility methods related to module processing.
 *
 * @version $Rev: 471754 $
 * @since Struts 1.2
 */
public class ModuleUtils {
    /**
     * The Singleton instance.
     */
    private static final ModuleUtils instance = new ModuleUtils();

    /**
     * Commons logging instance.
     */
    private static final Log log = LogFactory.getLog(ModuleUtils.class);

    /**
     * Constructor for ModuleUtils.
     */
    protected ModuleUtils() {
        super();
    }

    /**
     * Returns the Singleton instance of TagUtils.
     */
    public static ModuleUtils getInstance() {
        return instance;
    }

    /**
     * Return the current ModuleConfig object stored in request, if it exists,
     * null otherwise. This method can be used by plugin to retrieve the
     * current module config object. If no moduleConfig is found, this means
     * that the request haven't hit the server throught the struts servlet.
     * The appropriate module config can be set and found with <code>{@link
     * ModuleUtils#selectModule(HttpServletRequest, ServletContext)} </code>.
     *
     * @param request The servlet request we are processing
     * @return the ModuleConfig object from request, or null if none is set in
     *         the request.
     */
    public ModuleConfig getModuleConfig(HttpServletRequest request) {
        return (ModuleConfig) request.getAttribute(Globals.MODULE_KEY);
    }

    /**
     * Return the desired ModuleConfig object stored in context, if it exists,
     * null otherwise.
     *
     * @param prefix  The module prefix of the desired module
     * @param context The ServletContext for this web application
     * @return the ModuleConfig object specified, or null if not found in the
     *         context.
     */
    public ModuleConfig getModuleConfig(String prefix, ServletContext context) {
        if ((prefix == null) || "/".equals(prefix)) {
            return (ModuleConfig) context.getAttribute(Globals.MODULE_KEY);
        } else {
            return (ModuleConfig) context.getAttribute(Globals.MODULE_KEY
                + prefix);
        }
    }

    /**
     * Return the desired ModuleConfig object stored in context, if it exists,
     * otherwise return the current ModuleConfig
     *
     * @param prefix  The module prefix of the desired module
     * @param request The servlet request we are processing
     * @param context The ServletContext for this web application
     * @return the ModuleConfig object specified, or null if not found in the
     *         context.
     */
    public ModuleConfig getModuleConfig(String prefix,
        HttpServletRequest request, ServletContext context) {
        ModuleConfig moduleConfig = null;

        if (prefix != null) {
            //lookup module stored with the given prefix.
            moduleConfig = this.getModuleConfig(prefix, context);
        } else {
            //return the current module if no prefix was supplied.
            moduleConfig = this.getModuleConfig(request, context);
        }

        return moduleConfig;
    }

    /**
     * Return the ModuleConfig object is it exists, null otherwise.
     *
     * @param request The servlet request we are processing
     * @param context The ServletContext for this web application
     * @return the ModuleConfig object
     */
    public ModuleConfig getModuleConfig(HttpServletRequest request,
        ServletContext context) {
        ModuleConfig moduleConfig = this.getModuleConfig(request);

        if (moduleConfig == null) {
            moduleConfig = this.getModuleConfig("", context);
            request.setAttribute(Globals.MODULE_KEY, moduleConfig);
        }

        return moduleConfig;
    }

    /**
     * Get the module name to which the specified request belong.
     *
     * @param request The servlet request we are processing
     * @param context The ServletContext for this web application
     * @return The module prefix or ""
     */
    public String getModuleName(HttpServletRequest request,
        ServletContext context) {
        // Acquire the path used to compute the module
        String matchPath =
            (String) request.getAttribute(RequestProcessor.INCLUDE_SERVLET_PATH);

        if (matchPath == null) {
            matchPath = request.getServletPath();
        }

        return this.getModuleName(matchPath, context);
    }

    /**
     * Get the module name to which the specified uri belong.
     *
     * @param matchPath The uri from which we want the module name.
     * @param context   The ServletContext for this web application
     * @return The module prefix or ""
     */
    public String getModuleName(String matchPath, ServletContext context) {
        if (log.isDebugEnabled()) {
            log.debug("Get module name for path " + matchPath);
        }

        String prefix = ""; // Initialize prefix before we try lookup
        String[] prefixes = getModulePrefixes(context);

        // Get all other possible prefixes
        int lastSlash = 0; // Initialize before loop

        while (prefix.equals("")
            && ((lastSlash = matchPath.lastIndexOf("/")) > 0)) {
            // We may be in a non-default module.  Try to get it's prefix.
            matchPath = matchPath.substring(0, lastSlash);

            // Match against the list of module prefixes
            for (int i = 0; i < prefixes.length; i++) {
                if (matchPath.equals(prefixes[i])) {
                    prefix = prefixes[i];

                    break;
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Module name found: "
                + (prefix.equals("") ? "default" : prefix));
        }

        return prefix;
    }

    /**
     * Return the list of module prefixes that are defined for this web
     * application.  <strong>NOTE</strong> - the "" prefix for the default
     * module is not included in this list.
     *
     * @param context The ServletContext for this web application.
     * @return An array of module prefixes.
     */
    public String[] getModulePrefixes(ServletContext context) {
        return (String[]) context.getAttribute(Globals.MODULE_PREFIXES_KEY);
    }

    /**
     * Select the module to which the specified request belongs, and add
     * corresponding request attributes to this request.
     *
     * @param request The servlet request we are processing
     * @param context The ServletContext for this web application
     */
    public void selectModule(HttpServletRequest request, ServletContext context) {
        // Compute module name
        String prefix = getModuleName(request, context);

        // Expose the resources for this module
        this.selectModule(prefix, request, context);
    }

    /**
     * Select the module to which the specified request belongs, and add
     * corresponding request attributes to this request.
     *
     * @param prefix  The module prefix of the desired module
     * @param request The servlet request we are processing
     * @param context The ServletContext for this web application
     */
    public void selectModule(String prefix, HttpServletRequest request,
        ServletContext context) {
        // Expose the resources for this module
        ModuleConfig config = getModuleConfig(prefix, context);

        if (config != null) {
            request.setAttribute(Globals.MODULE_KEY, config);

            MessageResourcesConfig[] mrConfig =
                config.findMessageResourcesConfigs();

            for (int i = 0; i < mrConfig.length; i++) {
                String key = mrConfig[i].getKey();
                MessageResources resources =
                    (MessageResources) context.getAttribute(key + prefix);

                if (resources != null) {
                    request.setAttribute(key, resources);
                } else {
                    request.removeAttribute(key);
                }
            }
        } else {
            request.removeAttribute(Globals.MODULE_KEY);
        }
    }
}
