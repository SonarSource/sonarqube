/*
 * $Id: ModuleConfig.java 471754 2006-11-06 14:55:09Z husted $
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


/**
 * <p>The collection of static configuration information that describes a
 * Struts-based module.  Multiple modules are identified by a <em>prefix</em>
 * at the beginning of the context relative portion of the request URI.  If no
 * module prefix can be matched, the default configuration (with a prefix
 * equal to a zero-length string) is selected, which is elegantly backwards
 * compatible with the previous Struts behavior that only supported one
 * module.</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-08-06 04:12:10 -0400 (Sat, 06 Aug 2005)
 *          $
 * @since Struts 1.1
 */
public interface ModuleConfig {
    /**
     * <p> Has this module been completely configured yet.  Once this flag has
     * been set, any attempt to modify the configuration will return an
     * IllegalStateException. </p>
     */
    boolean getConfigured();

    /**
     * <p> The controller configuration object for this module. </p>
     */
    ControllerConfig getControllerConfig();

    /**
     * <p> The controller configuration object for this module. </p>
     *
     * @param cc The controller configuration object for this module.
     */
    void setControllerConfig(ControllerConfig cc);

    /**
     * <p> The prefix of the context-relative portion of the request URI, used
     * to select this configuration versus others supported by the controller
     * servlet.  A configuration with a prefix of a zero-length String is the
     * default configuration for this web module. </p>
     */
    String getPrefix();

    /**
     * <p> The prefix of the context-relative portion of the request URI, used
     * to select this configuration versus others supported by the controller
     * servlet.  A configuration with a prefix of a zero-length String is the
     * default configuration for this web module. </p>
     *
     * @param prefix The prefix of the context-relative portion of the request
     *               URI.
     */
    public void setPrefix(String prefix);

    /**
     * <p> The default class name to be used when creating action form bean
     * instances. </p>
     */
    String getActionFormBeanClass();

    /**
     * <p> The default class name to be used when creating action form bean
     * instances. </p>
     *
     * @param actionFormBeanClass default class name to be used when creating
     *                            action form bean instances.
     */
    void setActionFormBeanClass(String actionFormBeanClass);

    /**
     * <p> The default class name to be used when creating action mapping
     * instances. </p>
     */
    String getActionMappingClass();

    /**
     * <p> The default class name to be used when creating action mapping
     * instances. </p>
     *
     * @param actionMappingClass default class name to be used when creating
     *                           action mapping instances.
     */
    void setActionMappingClass(String actionMappingClass);

    /**
     * <p> Add a new <code>ActionConfig</code> instance to the set associated
     * with this module. </p>
     *
     * @param config The new configuration instance to be added
     * @throws IllegalStateException if this module configuration has been
     *                               frozen
     */
    void addActionConfig(ActionConfig config);

    /**
     * <p> Add a new <code>ExceptionConfig</code> instance to the set
     * associated with this module. </p>
     *
     * @param config The new configuration instance to be added
     * @throws IllegalStateException if this module configuration has been
     *                               frozen
     */
    void addExceptionConfig(ExceptionConfig config);

    /**
     * <p> Add a new <code>FormBeanConfig</code> instance to the set
     * associated with this module. </p>
     *
     * @param config The new configuration instance to be added
     * @throws IllegalStateException if this module configuration has been
     *                               frozen
     */
    void addFormBeanConfig(FormBeanConfig config);

    /**
     * <p> The default class name to be used when creating action forward
     * instances. </p>
     */
    String getActionForwardClass();

    /**
     * <p> The default class name to be used when creating action forward
     * instances. </p>
     *
     * @param actionForwardClass default class name to be used when creating
     *                           action forward instances.
     */
    void setActionForwardClass(String actionForwardClass);

    /**
     * <p> Add a new <code>ForwardConfig</code> instance to the set of global
     * forwards associated with this module. </p>
     *
     * @param config The new configuration instance to be added
     * @throws IllegalStateException if this module configuration has been
     *                               frozen
     */
    void addForwardConfig(ForwardConfig config);

    /**
     * <p> Add a new <code>MessageResourcesConfig</code> instance to the set
     * associated with this module. </p>
     *
     * @param config The new configuration instance to be added
     * @throws IllegalStateException if this module configuration has been
     *                               frozen
     */
    void addMessageResourcesConfig(MessageResourcesConfig config);

    /**
     * <p> Add a newly configured {@link PlugInConfig} instance to the set of
     * plug-in Actions for this module. </p>
     *
     * @param plugInConfig The new configuration instance to be added
     */
    void addPlugInConfig(PlugInConfig plugInConfig);

    /**
     * <p> Return the action configuration for the specified path, if any;
     * otherwise return <code>null</code>. </p>
     *
     * @param path Path of the action configuration to return
     */
    ActionConfig findActionConfig(String path);

    /**
     * <p> Return the action configurations for this module.  If there are
     * none, a zero-length array is returned. </p>
     */
    ActionConfig[] findActionConfigs();

    /**
     * <p>Returns the action configuration for the specifed action
     * action identifier.</p>
     *
     * @param actionId the action identifier
     * @return the action config if found; otherwise <code>null</code>
     * @see ActionConfig#getActionId()
     * @since Struts 1.3.6
     */
    ActionConfig findActionConfigId(String actionId);

    /**
     * <p> Return the exception configuration for the specified type, if any;
     * otherwise return <code>null</code>. </p>
     *
     * @param type Exception class name to find a configuration for
     */
    ExceptionConfig findExceptionConfig(String type);

    /**
     * <p> Perform a recursive search for an ExceptionConfig registered for
     * this class, or for any superclass.  This should only be used in the
     * case when an <code>ActionConfig</code> is not available; otherwise, use
     * <code>ActionConfig.findException(Class)</code> to preserve the search
     * order. </p>
     *
     * @param type Exception class name to find a configuration for
     * @see ActionConfig findException(Class)
     */
    ExceptionConfig findException(Class type);

    /**
     * <p> Return the exception configurations for this module.  If there are
     * none, a zero-length array is returned. </p>
     */
    ExceptionConfig[] findExceptionConfigs();

    /**
     * <p> Return the form bean configuration for the specified key, if any;
     * otherwise return <code>null</code>.
     *
     * @param name Name of the form bean configuration to return
     */
    FormBeanConfig findFormBeanConfig(String name);

    /**
     * <p> Return the form bean configurations for this module.  If there are
     * none, a zero-length array is returned. </p>
     */
    FormBeanConfig[] findFormBeanConfigs();

    /**
     * <p> Return the forward configuration for the specified key, if any;
     * otherwise return <code>null</code>. </p>
     *
     * @param name Name of the forward configuration to return
     */
    ForwardConfig findForwardConfig(String name);

    /**
     * <p> Return the form bean configurations for this module.  If there are
     * none, a zero-length array is returned. </p>
     */
    ForwardConfig[] findForwardConfigs();

    /**
     * <p> Return the message resources configuration for the specified key,
     * if any; otherwise return <code>null</code>. </p>
     *
     * @param key Key of the data source configuration to return
     */
    MessageResourcesConfig findMessageResourcesConfig(String key);

    /**
     * <p> Return the message resources configurations for this module. If
     * there are none, a zero-length array is returned. </p>
     */
    MessageResourcesConfig[] findMessageResourcesConfigs();

    /**
     * <p> Return the configured plug-in actions for this module.  If there
     * are none, a zero-length array is returned. </p>
     */
    PlugInConfig[] findPlugInConfigs();

    /**
     * <p> Freeze the configuration of this module.  After this method
     * returns, any attempt to modify the configuration will return an
     * IllegalStateException. </p>
     */
    void freeze();

    /**
     * <p> Remove the specified action configuration instance. </p>
     *
     * @param config ActionConfig instance to be removed
     * @throws IllegalStateException if this module configuration has been
     *                               frozen
     */
    void removeActionConfig(ActionConfig config);

    /**
     * <p> Remove the specified exception configuration instance. </p>
     *
     * @param config ActionConfig instance to be removed
     * @throws IllegalStateException if this module configuration has been
     *                               frozen
     */
    void removeExceptionConfig(ExceptionConfig config);

    /**
     * <p> Remove the specified form bean configuration instance. </p>
     *
     * @param config FormBeanConfig instance to be removed
     * @throws IllegalStateException if this module configuration has been
     *                               frozen
     */
    void removeFormBeanConfig(FormBeanConfig config);

    /**
     * <p> Remove the specified forward configuration instance. </p>
     *
     * @param config ForwardConfig instance to be removed
     * @throws IllegalStateException if this module configuration has been
     *                               frozen
     */
    void removeForwardConfig(ForwardConfig config);

    /**
     * <p> Remove the specified message resources configuration instance.
     * </p>
     *
     * @param config MessageResourcesConfig instance to be removed
     * @throws IllegalStateException if this module configuration has been
     *                               frozen
     */
    void removeMessageResourcesConfig(MessageResourcesConfig config);
}
