/*
 * $Id: ActionConfigMatcher.java 530002 2007-04-18 12:35:44Z pbenedict $
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
package org.apache.struts.config;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.apache.struts.util.WildcardHelper;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * <p> Matches paths against pre-compiled wildcard expressions pulled from
 * action configs. It uses the wildcard matcher from the Apache Cocoon
 * project. Patterns will be matched in the order they exist in the Struts
 * config file. The last match wins, so more specific patterns should be
 * defined after less specific patterns.
 *
 * @since Struts 1.2
 */
public class ActionConfigMatcher implements Serializable {
    /**
     * <p> The logging instance </p>
     */
    private static final Log log = LogFactory.getLog(ActionConfigMatcher.class);

    /**
     * <p> Handles all wildcard pattern matching. </p>
     */
    private static final WildcardHelper wildcard = new WildcardHelper();

    /**
     * <p> The compiled paths and their associated ActionConfig's </p>
     */
    private List compiledPaths;

    /**
     * <p> Finds and precompiles the wildcard patterns from the ActionConfig
     * "path" attributes. ActionConfig's will be evaluated in the order they
     * exist in the Struts config file. Only paths that actually contain a
     * wildcard will be compiled. </p>
     *
     * @param configs An array of ActionConfig's to process
     */
    public ActionConfigMatcher(ActionConfig[] configs) {
        compiledPaths = new ArrayList();

        int[] pattern;
        String path;

        for (int x = 0; x < configs.length; x++) {
            path = configs[x].getPath();

            if ((path != null) && (path.indexOf('*') > -1)) {
                if ((path.length() > 0) && (path.charAt(0) == '/')) {
                    path = path.substring(1);
                }

                if (log.isDebugEnabled()) {
                    log.debug("Compiling action config path '" + path + "'");
                }

                pattern = wildcard.compilePattern(path);
                compiledPaths.add(new Mapping(pattern, configs[x]));
            }
        }
    }

    /**
     * <p> Matches the path against the compiled wildcard patterns. </p>
     *
     * @param path The portion of the request URI for selecting a config.
     * @return The action config if matched, else null
     */
    public ActionConfig match(String path) {
        ActionConfig config = null;

        if (compiledPaths.size() > 0) {
            if (log.isDebugEnabled()) {
                log.debug("Attempting to match '" + path
                    + "' to a wildcard pattern");
            }

            if ((path.length() > 0) && (path.charAt(0) == '/')) {
                path = path.substring(1);
            }

            Mapping m;
            HashMap vars = new HashMap();

            for (Iterator i = compiledPaths.iterator(); i.hasNext();) {
                m = (Mapping) i.next();

                if (wildcard.match(vars, path, m.getPattern())) {
                    if (log.isDebugEnabled()) {
                        log.debug("Path matches pattern '"
                            + m.getActionConfig().getPath() + "'");
                    }

                    config =
                        convertActionConfig(path,
                            (ActionConfig) m.getActionConfig(), vars);
                }
            }
        }

        return config;
    }

    /**
     * <p> Clones the ActionConfig and its children, replacing various
     * properties with the values of the wildcard-matched strings. </p>
     *
     * @param path The requested path
     * @param orig The original ActionConfig
     * @param vars A Map of wildcard-matched strings
     * @return A cloned ActionConfig with appropriate properties replaced with
     *         wildcard-matched values
     */
    protected ActionConfig convertActionConfig(String path, ActionConfig orig,
        Map vars) {
        ActionConfig config = null;

        try {
            config = (ActionConfig) BeanUtils.cloneBean(orig);
        } catch (Exception ex) {
            log.warn("Unable to clone action config, recommend not using "
                + "wildcards", ex);

            return null;
        }

        config.setName(convertParam(orig.getName(), vars));

        if ((path.length() == 0) || (path.charAt(0) != '/')) {
            path = "/" + path;
        }

        config.setPath(path);
        config.setType(convertParam(orig.getType(), vars));
        config.setRoles(convertParam(orig.getRoles(), vars));
        config.setParameter(convertParam(orig.getParameter(), vars));
        config.setAttribute(convertParam(orig.getAttribute(), vars));
        config.setForward(convertParam(orig.getForward(), vars));
        config.setInclude(convertParam(orig.getInclude(), vars));
        config.setInput(convertParam(orig.getInput(), vars));
        config.setCatalog(convertParam(orig.getCatalog(), vars));
        config.setCommand(convertParam(orig.getCommand(), vars));
        config.setMultipartClass(convertParam(orig.getMultipartClass(), vars));
        config.setPrefix(convertParam(orig.getPrefix(), vars));
        config.setSuffix(convertParam(orig.getSuffix(), vars));

        ForwardConfig[] fConfigs = orig.findForwardConfigs();
        ForwardConfig cfg;

        for (int x = 0; x < fConfigs.length; x++) {
            try {
                cfg = (ActionForward) BeanUtils.cloneBean(fConfigs[x]);
            } catch (Exception ex) {
                log.warn("Unable to clone action config, recommend not using "
                        + "wildcards", ex);
                return null;
            }
            cfg.setName(fConfigs[x].getName());
            cfg.setPath(convertParam(fConfigs[x].getPath(), vars));
            cfg.setRedirect(fConfigs[x].getRedirect());
            cfg.setCommand(convertParam(fConfigs[x].getCommand(), vars));
            cfg.setCatalog(convertParam(fConfigs[x].getCatalog(), vars));
            cfg.setModule(convertParam(fConfigs[x].getModule(), vars));

            replaceProperties(fConfigs[x].getProperties(), cfg.getProperties(),
                vars);

            config.removeForwardConfig(fConfigs[x]);
            config.addForwardConfig(cfg);
        }

        replaceProperties(orig.getProperties(), config.getProperties(), vars);

        ExceptionConfig[] exConfigs = orig.findExceptionConfigs();

        for (int x = 0; x < exConfigs.length; x++) {
            config.addExceptionConfig(exConfigs[x]);
        }

        config.freeze();

        return config;
    }

    /**
     * <p> Replaces placeholders from one Properties values set to another.
     * </p>
     *
     * @param orig  The original properties set with placehold values
     * @param props The target properties to store the processed values
     * @param vars  A Map of wildcard-matched strings
     */
    protected void replaceProperties(Properties orig, Properties props, Map vars) {
        Map.Entry entry = null;

        for (Iterator i = orig.entrySet().iterator(); i.hasNext();) {
            entry = (Map.Entry) i.next();
            props.setProperty((String) entry.getKey(),
                convertParam((String) entry.getValue(), vars));
        }
    }

    /**
     * <p> Inserts into a value wildcard-matched strings where specified.
     * </p>
     *
     * @param val  The value to convert
     * @param vars A Map of wildcard-matched strings
     * @return The new value
     */
    protected String convertParam(String val, Map vars) {
        if (val == null) {
            return null;
        } else if (val.indexOf("{") == -1) {
            return val;
        }

        Map.Entry entry;
        StringBuffer key = new StringBuffer("{0}");
        StringBuffer ret = new StringBuffer(val);
        String keyTmp;
        int x;

        for (Iterator i = vars.entrySet().iterator(); i.hasNext();) {
            entry = (Map.Entry) i.next();
            key.setCharAt(1, ((String) entry.getKey()).charAt(0));
            keyTmp = key.toString();

            // Replace all instances of the placeholder
            while ((x = ret.toString().indexOf(keyTmp)) > -1) {
                ret.replace(x, x + 3, (String) entry.getValue());
            }
        }

        return ret.toString();
    }

    /**
     * <p> Stores a compiled wildcard pattern and the ActionConfig it came
     * from. </p>
     */
    private class Mapping implements Serializable {
        /**
         * <p> The compiled pattern. </p>
         */
        private int[] pattern;

        /**
         * <p> The original ActionConfig. </p>
         */
        private ActionConfig config;

        /**
         * <p> Contructs a read-only Mapping instance. </p>
         *
         * @param pattern The compiled pattern
         * @param config  The original ActionConfig
         */
        public Mapping(int[] pattern, ActionConfig config) {
            this.pattern = pattern;
            this.config = config;
        }

        /**
         * <p> Gets the compiled wildcard pattern. </p>
         *
         * @return The compiled pattern
         */
        public int[] getPattern() {
            return this.pattern;
        }

        /**
         * <p> Gets the ActionConfig that contains the pattern. </p>
         *
         * @return The associated ActionConfig
         */
        public ActionConfig getActionConfig() {
            return this.config;
        }
    }
}
