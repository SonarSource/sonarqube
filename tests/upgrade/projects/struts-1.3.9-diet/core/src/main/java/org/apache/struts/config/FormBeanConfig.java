/*
 * $Id: FormBeanConfig.java 472728 2006-11-09 01:10:58Z niallp $
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
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.MutableDynaClass;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.DynaActionForm;
import org.apache.struts.action.DynaActionFormClass;
import org.apache.struts.chain.commands.util.ClassUtils;
import org.apache.struts.chain.contexts.ActionContext;
import org.apache.struts.chain.contexts.ServletActionContext;
import org.apache.struts.util.RequestUtils;
import org.apache.struts.validator.BeanValidatorForm;

import java.lang.reflect.InvocationTargetException;

import java.util.HashMap;

/**
 * <p>A JavaBean representing the configuration information of a
 * <code>&lt;form-bean&gt;</code> element in a Struts configuration file.<p>
 *
 * @version $Rev: 472728 $ $Date: 2006-01-17 07:26:20 -0500 (Tue, 17 Jan 2006)
 *          $
 * @since Struts 1.1
 */
public class FormBeanConfig extends BaseConfig {
    private static final Log log = LogFactory.getLog(FormBeanConfig.class);

    // ----------------------------------------------------- Instance Variables

    /**
     * The set of FormProperty elements defining dynamic form properties for
     * this form bean, keyed by property name.
     */
    protected HashMap formProperties = new HashMap();

    /**
     * <p>The lockable object we can synchronize on when creating
     * DynaActionFormClass.</p>
     */
    protected String lock = "";

    // ------------------------------------------------------------- Properties

    /**
     * The DynaActionFormClass associated with a DynaActionForm.
     */
    protected transient DynaActionFormClass dynaActionFormClass;

    /**
     * Is the form bean class an instance of DynaActionForm with dynamic
     * properties?
     */
    protected boolean dynamic = false;

    /**
     * The name of the FormBeanConfig that this config inherits configuration
     * information from.
     */
    protected String inherit = null;

    /**
     * Have the inheritance values for this class been applied?
     */
    protected boolean extensionProcessed = false;

    /**
     * The unique identifier of this form bean, which is used to reference
     * this bean in <code>ActionMapping</code> instances as well as for the
     * name of the request or session attribute under which the corresponding
     * form bean instance is created or accessed.
     */
    protected String name = null;

    /**
     * The fully qualified Java class name of the implementation class to be
     * used or generated.
     */
    protected String type = null;

    /**
     * Is this DynaClass currently restricted (for DynaBeans with a
     * MutableDynaClass).
     */
    protected boolean restricted = false;

    /**
     * <p>Return the DynaActionFormClass associated with a
     * DynaActionForm.</p>
     *
     * @throws IllegalArgumentException if the ActionForm is not dynamic
     */
    public DynaActionFormClass getDynaActionFormClass() {
        if (dynamic == false) {
            throw new IllegalArgumentException("ActionForm is not dynamic");
        }

        synchronized (lock) {
            if (dynaActionFormClass == null) {
                dynaActionFormClass = new DynaActionFormClass(this);
            }
        }

        return dynaActionFormClass;
    }

    public boolean getDynamic() {
        return (this.dynamic);
    }

    public String getExtends() {
        return (this.inherit);
    }

    public void setExtends(String extend) {
        throwIfConfigured();
        this.inherit = extend;
    }

    public boolean isExtensionProcessed() {
        return extensionProcessed;
    }

    public String getName() {
        return (this.name);
    }

    public void setName(String name) {
        throwIfConfigured();
        this.name = name;
    }

    public String getType() {
        return (this.type);
    }

    public void setType(String type) {
        throwIfConfigured();
        this.type = type;

        Class dynaBeanClass = DynaActionForm.class;
        Class formBeanClass = formBeanClass();

        if (formBeanClass != null) {
            if (dynaBeanClass.isAssignableFrom(formBeanClass)) {
                this.dynamic = true;
            } else {
                this.dynamic = false;
            }
        } else {
            this.dynamic = false;
        }
    }

    /**
     * <p>Indicates whether a MutableDynaClass is currently restricted.</p>
     * <p>If so, no changes to the existing registration of property names,
     * data types, readability, or writeability are allowed.</p>
     */
    public boolean isRestricted() {
        return restricted;
    }

    /**
     * <p>Set whether a MutableDynaClass is currently restricted.</p> <p>If
     * so, no changes to the existing registration of property names, data
     * types, readability, or writeability are allowed.</p>
     */
    public void setRestricted(boolean restricted) {
        this.restricted = restricted;
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * <p>Traces the hierarchy of this object to check if any of the ancestors
     * is extending this instance.</p>
     *
     * @param moduleConfig The configuration for the module being configured.
     * @return true if circular inheritance was detected.
     */
    protected boolean checkCircularInheritance(ModuleConfig moduleConfig) {
        String ancestorName = getExtends();

        while (ancestorName != null) {
            // check if we have the same name as an ancestor
            if (getName().equals(ancestorName)) {
                return true;
            }

            // get our ancestor's ancestor
            FormBeanConfig ancestor =
                moduleConfig.findFormBeanConfig(ancestorName);

            ancestorName = ancestor.getExtends();
        }

        return false;
    }

    /**
     * <p>Compare the form properties of this bean with that of the given and
     * copy those that are not present.</p>
     *
     * @param config The form bean config to copy properties from.
     * @see #inheritFrom(FormBeanConfig)
     */
    protected void inheritFormProperties(FormBeanConfig config)
        throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, InvocationTargetException {
        throwIfConfigured();

        // Inherit form property configs
        FormPropertyConfig[] baseFpcs = config.findFormPropertyConfigs();

        for (int i = 0; i < baseFpcs.length; i++) {
            FormPropertyConfig baseFpc = baseFpcs[i];

            // Do we have this prop?
            FormPropertyConfig prop =
                this.findFormPropertyConfig(baseFpc.getName());

            if (prop == null) {
                // We don't have this, so let's copy it
                prop =
                    (FormPropertyConfig) RequestUtils.applicationInstance(baseFpc.getClass()
                                                                                 .getName());

                BeanUtils.copyProperties(prop, baseFpc);
                this.addFormPropertyConfig(prop);
                prop.setProperties(baseFpc.copyProperties());
            }
        }
    }

    // --------------------------------------------------------- Public Methods

    /**
     * <p>Create and return an <code>ActionForm</code> instance appropriate to
     * the information in this <code>FormBeanConfig</code>.</p>
     *
     * <p>Although this method is not formally deprecated yet, where possible,
     * the form which accepts an <code>ActionContext</code> as an argument is
     * preferred, to help sever direct dependencies on the Servlet API.  As
     * the ActionContext becomes more familiar in Struts, this method will
     * almost certainly be deprecated.</p>
     *
     * @param servlet The action servlet
     * @return ActionForm instance
     * @throws IllegalAccessException if the Class or the appropriate
     *                                constructor is not accessible
     * @throws InstantiationException if this Class represents an abstract
     *                                class, an array class, a primitive type,
     *                                or void; or if instantiation fails for
     *                                some other reason
     */
    public ActionForm createActionForm(ActionServlet servlet)
        throws IllegalAccessException, InstantiationException {
        Object obj = null;

        // Create a new form bean instance
        if (getDynamic()) {
            obj = getDynaActionFormClass().newInstance();
        } else {
            obj = formBeanClass().newInstance();
        }

        ActionForm form = null;

        if (obj instanceof ActionForm) {
            form = (ActionForm) obj;
        } else {
            form = new BeanValidatorForm(obj);
        }

        form.setServlet(servlet);

        if (form instanceof DynaBean
            && ((DynaBean) form).getDynaClass() instanceof MutableDynaClass) {
            DynaBean dynaBean = (DynaBean) form;
            MutableDynaClass dynaClass =
                (MutableDynaClass) dynaBean.getDynaClass();

            // Add properties
            dynaClass.setRestricted(false);

            FormPropertyConfig[] props = findFormPropertyConfigs();

            for (int i = 0; i < props.length; i++) {
                dynaClass.add(props[i].getName(), props[i].getTypeClass());
                dynaBean.set(props[i].getName(), props[i].initial());
            }

            dynaClass.setRestricted(isRestricted());
        }

        if (form instanceof BeanValidatorForm) {
            ((BeanValidatorForm)form).initialize(this);
        }

        return form;
    }

    /**
     * <p>Create and return an <code>ActionForm</code> instance appropriate to
     * the information in this <code>FormBeanConfig</code>.</p>
     * <p><b>NOTE:</b> If the given <code>ActionContext</code> is not of type
     * <code>ServletActionContext</code> (or a subclass), then the form which
     * is returned will have a null <code>servlet</code> property.  Some of
     * the subclasses of <code>ActionForm</code> included in Struts will later
     * throw a <code>NullPointerException</code> in this case. </p> <p>TODO:
     * Find a way to control this direct dependency on the Servlet API.</p>
     *
     * @param context The ActionContext.
     * @return ActionForm instance
     * @throws IllegalAccessException if the Class or the appropriate
     *                                constructor is not accessible
     * @throws InstantiationException if this Class represents an abstract
     *                                class, an array class, a primitive type,
     *                                or void; or if instantiation fails for
     *                                some other reason
     */
    public ActionForm createActionForm(ActionContext context)
        throws IllegalAccessException, InstantiationException {
        ActionServlet actionServlet = null;

        if (context instanceof ServletActionContext) {
            ServletActionContext saContext = (ServletActionContext) context;

            actionServlet = saContext.getActionServlet();
        }

        return createActionForm(actionServlet);
    }

    /**
     * <p>Checks if the given <code>ActionForm</code> instance is suitable for
     * use as an alternative to calling this <code>FormBeanConfig</code>
     * instance's <code>createActionForm</code> method.</p>
     *
     * @param form an existing form instance that may be reused.
     * @return true if the given form can be reused as the form for this
     *         config.
     */
    public boolean canReuse(ActionForm form) {
        if (form != null) {
            if (this.getDynamic()) {
                String className = ((DynaBean) form).getDynaClass().getName();

                if (className.equals(this.getName())) {
                    log.debug("Can reuse existing instance (dynamic)");

                    return (true);
                }
            } else {
                try {
                    // check if the form's class is compatible with the class
                    //      we're configured for
                    Class formClass = form.getClass();

                    if (form instanceof BeanValidatorForm) {
                        BeanValidatorForm beanValidatorForm =
                            (BeanValidatorForm) form;

                        if (beanValidatorForm.getInstance() instanceof DynaBean) {
                            String formName = beanValidatorForm.getStrutsConfigFormName();
                            if (getName().equals(formName)) {
                                log.debug("Can reuse existing instance (BeanValidatorForm)");
                                return true;
                            } else {
                                return false;
                            }
                        }
                        formClass = beanValidatorForm.getInstance().getClass();
                    }

                    Class configClass =
                        ClassUtils.getApplicationClass(this.getType());

                    if (configClass.isAssignableFrom(formClass)) {
                        log.debug("Can reuse existing instance (non-dynamic)");

                        return (true);
                    }
                } catch (Exception e) {
                    log.debug("Error testing existing instance for reusability; just create a new instance",
                        e);
                }
            }
        }

        return false;
    }

    /**
     * Add a new <code>FormPropertyConfig</code> instance to the set
     * associated with this module.
     *
     * @param config The new configuration instance to be added
     * @throws IllegalArgumentException if this property name has already been
     *                                  defined
     */
    public void addFormPropertyConfig(FormPropertyConfig config) {
        throwIfConfigured();

        if (formProperties.containsKey(config.getName())) {
            throw new IllegalArgumentException("Property " + config.getName()
                + " already defined");
        }

        formProperties.put(config.getName(), config);
    }

    /**
     * Return the form property configuration for the specified property name,
     * if any; otherwise return <code>null</code>.
     *
     * @param name Form property name to find a configuration for
     */
    public FormPropertyConfig findFormPropertyConfig(String name) {
        return ((FormPropertyConfig) formProperties.get(name));
    }

    /**
     * Return the form property configurations for this module.  If there are
     * none, a zero-length array is returned.
     */
    public FormPropertyConfig[] findFormPropertyConfigs() {
        FormPropertyConfig[] results =
            new FormPropertyConfig[formProperties.size()];

        return ((FormPropertyConfig[]) formProperties.values().toArray(results));
    }

    /**
     * Freeze the configuration of this component.
     */
    public void freeze() {
        super.freeze();

        FormPropertyConfig[] fpconfigs = findFormPropertyConfigs();

        for (int i = 0; i < fpconfigs.length; i++) {
            fpconfigs[i].freeze();
        }
    }

    /**
     * <p>Inherit values that have not been overridden from the provided
     * config object.  Subclasses overriding this method should verify that
     * the given parameter is of a class that contains a property it is trying
     * to inherit:</p>
     *
     * <pre>
     * if (config instanceof MyCustomConfig) {
     *     MyCustomConfig myConfig =
     *         (MyCustomConfig) config;
     *
     *     if (getMyCustomProp() == null) {
     *         setMyCustomProp(myConfig.getMyCustomProp());
     *     }
     * }
     * </pre>
     *
     * <p>If the given <code>config</code> is extending another object, those
     * extensions should be resolved before it's used as a parameter to this
     * method.</p>
     *
     * @param config The object that this instance will be inheriting its
     *               values from.
     * @see #processExtends(ModuleConfig)
     */
    public void inheritFrom(FormBeanConfig config)
        throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, InvocationTargetException {
        throwIfConfigured();

        // Inherit values that have not been overridden
        if (getName() == null) {
            setName(config.getName());
        }

        if (!isRestricted()) {
            setRestricted(config.isRestricted());
        }

        if (getType() == null) {
            setType(config.getType());
        }

        inheritFormProperties(config);
        inheritProperties(config);
    }

    /**
     * <p>Inherit configuration information from the FormBeanConfig that this
     * instance is extending.  This method verifies that any form bean config
     * object that it inherits from has also had its processExtends() method
     * called.</p>
     *
     * @param moduleConfig The {@link ModuleConfig} that this bean is from.
     * @see #inheritFrom(FormBeanConfig)
     */
    public void processExtends(ModuleConfig moduleConfig)
        throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, InvocationTargetException {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        String ancestor = getExtends();

        if ((!extensionProcessed) && (ancestor != null)) {
            FormBeanConfig baseConfig =
                moduleConfig.findFormBeanConfig(ancestor);

            if (baseConfig == null) {
                throw new NullPointerException("Unable to find "
                    + "form bean '" + ancestor + "' to extend.");
            }

            // Check against circule inheritance and make sure the base config's
            //  own extends have been processed already
            if (checkCircularInheritance(moduleConfig)) {
                throw new IllegalArgumentException(
                    "Circular inheritance detected for form bean " + getName());
            }

            // Make sure the ancestor's own extension has been processed.
            if (!baseConfig.isExtensionProcessed()) {
                baseConfig.processExtends(moduleConfig);
            }

            // Copy values from the base config
            inheritFrom(baseConfig);
        }

        extensionProcessed = true;
    }

    /**
     * Remove the specified form property configuration instance.
     *
     * @param config FormPropertyConfig instance to be removed
     */
    public void removeFormPropertyConfig(FormPropertyConfig config) {
        if (configured) {
            throw new IllegalStateException("Configuration is frozen");
        }

        formProperties.remove(config.getName());
    }

    /**
     * Return a String representation of this object.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("FormBeanConfig[");

        sb.append("name=");
        sb.append(this.name);
        sb.append(",type=");
        sb.append(this.type);
        sb.append(",extends=");
        sb.append(this.inherit);
        sb.append("]");

        return (sb.toString());
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Return the <code>Class</code> instance for the form bean implementation
     * configured by this <code>FormBeanConfig</code> instance.  This method
     * uses the same algorithm as <code>RequestUtils.applicationClass()</code>
     * but is reproduced to avoid a runtime dependence.
     */
    protected Class formBeanClass() {
        ClassLoader classLoader =
            Thread.currentThread().getContextClassLoader();

        if (classLoader == null) {
            classLoader = this.getClass().getClassLoader();
        }

        try {
            return (classLoader.loadClass(getType()));
        } catch (Exception e) {
            return (null);
        }
    }
}
