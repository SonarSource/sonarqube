/*
 * $Id: ConfigRuleSet.java 471754 2006-11-06 14:55:09Z husted $
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

import org.apache.commons.digester.AbstractObjectCreationFactory;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.commons.digester.RuleSetBase;
import org.apache.commons.digester.SetPropertyRule;
import org.apache.struts.util.RequestUtils;
import org.xml.sax.Attributes;

/**
 * <p>The set of Digester rules required to parse a Struts configuration file
 * (<code>struts-config.xml</code>).</p>
 *
 * @version $Rev: 471754 $ $Date: 2005-08-16 15:53:27 -0400 (Tue, 16 Aug 2005)
 *          $
 * @since Struts 1.1
 */
public class ConfigRuleSet extends RuleSetBase {
    // --------------------------------------------------------- Public Methods

    /**
     * <p>Add the set of Rule instances defined in this RuleSet to the
     * specified <code>Digester</code> instance, associating them with our
     * namespace URI (if any).  This method should only be called by a
     * Digester instance.  These rules assume that an instance of
     * <code>org.apache.struts.config.ModuleConfig</code> is pushed onto the
     * evaluation stack before parsing begins.</p>
     *
     * @param digester Digester instance to which the new Rule instances
     *                 should be added.
     */
    public void addRuleInstances(Digester digester) {
        ClassLoader cl = digester.getClassLoader();

        digester.addRule("struts-config/action-mappings",
            new SetActionMappingClassRule());

        digester.addFactoryCreate("struts-config/action-mappings/action",
            new ActionMappingFactory(cl));
        digester.addSetProperties("struts-config/action-mappings/action");
        digester.addSetNext("struts-config/action-mappings/action",
            "addActionConfig", "org.apache.struts.config.ActionConfig");

        digester.addRule("struts-config/action-mappings/action/set-property",
            new BaseConfigSetPropertyRule());

        digester.addObjectCreate("struts-config/action-mappings/action/exception",
            "org.apache.struts.config.ExceptionConfig", "className");
        digester.addSetProperties(
            "struts-config/action-mappings/action/exception");
        digester.addSetNext("struts-config/action-mappings/action/exception",
            "addExceptionConfig", "org.apache.struts.config.ExceptionConfig");

        digester.addRule("struts-config/action-mappings/action/exception/set-property",
            new BaseConfigSetPropertyRule());

        digester.addFactoryCreate("struts-config/action-mappings/action/forward",
            new ActionForwardFactory(cl));
        digester.addSetProperties(
            "struts-config/action-mappings/action/forward");
        digester.addSetNext("struts-config/action-mappings/action/forward",
            "addForwardConfig", "org.apache.struts.config.ForwardConfig");

        digester.addRule("struts-config/action-mappings/action/forward/set-property",
            new BaseConfigSetPropertyRule());

        digester.addObjectCreate("struts-config/controller",
            "org.apache.struts.config.ControllerConfig", "className");
        digester.addSetProperties("struts-config/controller");
        digester.addSetNext("struts-config/controller", "setControllerConfig",
            "org.apache.struts.config.ControllerConfig");

        digester.addRule("struts-config/controller/set-property",
            new BaseConfigSetPropertyRule());

        digester.addRule("struts-config/form-beans",
            new SetActionFormBeanClassRule());

        digester.addFactoryCreate("struts-config/form-beans/form-bean",
            new ActionFormBeanFactory(cl));
        digester.addSetProperties("struts-config/form-beans/form-bean");
        digester.addSetNext("struts-config/form-beans/form-bean",
            "addFormBeanConfig", "org.apache.struts.config.FormBeanConfig");

        digester.addObjectCreate("struts-config/form-beans/form-bean/form-property",
            "org.apache.struts.config.FormPropertyConfig", "className");
        digester.addSetProperties(
            "struts-config/form-beans/form-bean/form-property");
        digester.addSetNext("struts-config/form-beans/form-bean/form-property",
            "addFormPropertyConfig",
            "org.apache.struts.config.FormPropertyConfig");

        digester.addRule("struts-config/form-beans/form-bean/form-property/set-property",
            new BaseConfigSetPropertyRule());

        digester.addRule("struts-config/form-beans/form-bean/set-property",
            new BaseConfigSetPropertyRule());

        digester.addObjectCreate("struts-config/global-exceptions/exception",
            "org.apache.struts.config.ExceptionConfig", "className");
        digester.addSetProperties("struts-config/global-exceptions/exception");
        digester.addSetNext("struts-config/global-exceptions/exception",
            "addExceptionConfig", "org.apache.struts.config.ExceptionConfig");

        digester.addRule("struts-config/global-exceptions/exception/set-property",
            new BaseConfigSetPropertyRule());

        digester.addRule("struts-config/global-forwards",
            new SetActionForwardClassRule());

        digester.addFactoryCreate("struts-config/global-forwards/forward",
            new GlobalForwardFactory(cl));
        digester.addSetProperties("struts-config/global-forwards/forward");
        digester.addSetNext("struts-config/global-forwards/forward",
            "addForwardConfig", "org.apache.struts.config.ForwardConfig");

        digester.addRule("struts-config/global-forwards/forward/set-property",
            new BaseConfigSetPropertyRule());

        digester.addObjectCreate("struts-config/message-resources",
            "org.apache.struts.config.MessageResourcesConfig", "className");
        digester.addSetProperties("struts-config/message-resources");
        digester.addSetNext("struts-config/message-resources",
            "addMessageResourcesConfig",
            "org.apache.struts.config.MessageResourcesConfig");

        digester.addRule("struts-config/message-resources/set-property",
            new BaseConfigSetPropertyRule());

        digester.addObjectCreate("struts-config/plug-in",
            "org.apache.struts.config.PlugInConfig");
        digester.addSetProperties("struts-config/plug-in");
        digester.addSetNext("struts-config/plug-in", "addPlugInConfig",
            "org.apache.struts.config.PlugInConfig");

        digester.addRule("struts-config/plug-in/set-property",
            new PlugInSetPropertyRule());

        // PluginConfig does not extend BaseConfig, at least for now.
    }
}


/**
 * <p> Class that records the name and value of a configuration property to be
 * used in configuring a <code>PlugIn</code> instance when instantiated. </p>
 */
final class PlugInSetPropertyRule extends Rule {
    public PlugInSetPropertyRule() {
        super();
    }

    public void begin(String namespace, String names, Attributes attributes)
        throws Exception {
        PlugInConfig plugInConfig = (PlugInConfig) digester.peek();

        plugInConfig.addProperty(attributes.getValue("property"),
            attributes.getValue("value"));
    }
}


/**
 * <p> Class that sets the name of the class to use when creating action form
 * bean instances. The value is set on the object on the top of the stack,
 * which must be a <code>org.apache.struts.config.ModuleConfig</code>. </p>
 */
final class SetActionFormBeanClassRule extends Rule {
    public SetActionFormBeanClassRule() {
        super();
    }

    public void begin(String namespace, String name, Attributes attributes)
        throws Exception {
        String className = attributes.getValue("type");

        if (className != null) {
            ModuleConfig mc = (ModuleConfig) digester.peek();

            mc.setActionFormBeanClass(className);
        }
    }
}


/**
 * <p> A variant of the standard Digester <code>SetPropertyRule</code>.  If
 * the element being processed has a "key" attribute, then the value will be
 * used to call <code>setProperty(key,value)</code> on the object on top of
 * the stack, which will be assumed to be of type <code>ActionConfig</code>.
 * Otherwise, the standard <code>SetPropertyRule</code> behavior is invoked,
 * and the value will be used to set a bean property on the object on top of
 * the Digester stack. In that case, the element being processed is assumed to
 * have attributes "property" and "value". </p>
 */
final class BaseConfigSetPropertyRule extends SetPropertyRule {
    public BaseConfigSetPropertyRule() {
        super("property", "value");
    }

    public void begin(Attributes attributes)
        throws Exception {
        if (attributes.getIndex("key") == -1) {
            super.begin(attributes);

            return;
        }

        if (attributes.getIndex("property") != -1) {
            throw new IllegalArgumentException(
                "<set-property> accepts only one of 'key' or 'property' attributes.");
        }

        Object topOfStack = digester.peek();

        if (topOfStack instanceof BaseConfig) {
            BaseConfig config = (BaseConfig) topOfStack;

            config.setProperty(attributes.getValue("key"),
                attributes.getValue("value"));
        } else {
            throw new IllegalArgumentException(
                "'key' attribute of <set-property> only applicable to subclasses of BaseConfig; "
                + "object on top of stack is " + topOfStack + " [key: "
                + attributes.getValue("key") + ", value: "
                + attributes.getValue("value") + "]");
        }
    }
}


/**
 * <p> An object creation factory which creates action form bean instances,
 * taking into account the default class name, which may have been specified
 * on the parent element and which is made available through the object on the
 * top of the stack, which must be a <code>org.apache.struts.config.ModuleConfig</code>.
 * </p>
 */
final class ActionFormBeanFactory extends AbstractObjectCreationFactory {
    private ClassLoader cl;

    public ActionFormBeanFactory(ClassLoader cl) {
        super();
        this.cl = cl;
    }

    public Object createObject(Attributes attributes) {
        // Identify the name of the class to instantiate
        String className = attributes.getValue("className");

        if (className == null) {
            ModuleConfig mc = (ModuleConfig) digester.peek();

            className = mc.getActionFormBeanClass();
        }

        // Instantiate the new object and return it
        Object actionFormBean = null;

        try {
            actionFormBean = RequestUtils.applicationInstance(className, cl);
        } catch (Exception e) {
            digester.getLogger().error("ActionFormBeanFactory.createObject: ", e);
        }

        return actionFormBean;
    }
}


/**
 * <p> Class that sets the name of the class to use when creating action
 * mapping instances. The value is set on the object on the top of the stack,
 * which must be a <code>org.apache.struts.config.ModuleConfig</code>. </p>
 */
final class SetActionMappingClassRule extends Rule {
    public SetActionMappingClassRule() {
        super();
    }

    public void begin(String namespace, String name, Attributes attributes)
        throws Exception {
        String className = attributes.getValue("type");

        if (className != null) {
            ModuleConfig mc = (ModuleConfig) digester.peek();

            mc.setActionMappingClass(className);
        }
    }
}


/**
 * <p> An object creation factory which creates action mapping instances,
 * taking into account the default class name, which may have been specified
 * on the parent element and which is made available through the object on the
 * top of the stack, which must be a <code>org.apache.struts.config.ModuleConfig</code>.
 * </p>
 */
final class ActionMappingFactory extends AbstractObjectCreationFactory {
    private ClassLoader cl;

    public ActionMappingFactory(ClassLoader cl) {
        super();
        this.cl = cl;
    }

    public Object createObject(Attributes attributes) {
        // Identify the name of the class to instantiate
        String className = attributes.getValue("className");

        if (className == null) {
            ModuleConfig mc = (ModuleConfig) digester.peek();

            className = mc.getActionMappingClass();
        }

        // Instantiate the new object and return it
        Object actionMapping = null;

        try {
            actionMapping = RequestUtils.applicationInstance(className, cl);
        } catch (Exception e) {
            digester.getLogger().error("ActionMappingFactory.createObject: ", e);
        }

        return actionMapping;
    }
}


/**
 * <p> Class that sets the name of the class to use when creating global
 * forward instances. The value is set on the object on the top of the stack,
 * which must be a <code>org.apache.struts.config.ModuleConfig</code>. </p>
 */
final class SetActionForwardClassRule extends Rule {
    public SetActionForwardClassRule() {
        super();
    }

    public void begin(String namespace, String name, Attributes attributes)
        throws Exception {
        String className = attributes.getValue("type");

        if (className != null) {
            ModuleConfig mc = (ModuleConfig) digester.peek();

            mc.setActionForwardClass(className);
        }
    }
}


/**
 * <p> An object creation factory which creates global forward instances,
 * taking into account the default class name, which may have been specified
 * on the parent element and which is made available through the object on the
 * top of the stack, which must be a <code>org.apache.struts.config.ModuleConfig</code>.
 * </p>
 */
final class GlobalForwardFactory extends AbstractObjectCreationFactory {
    private ClassLoader cl;

    public GlobalForwardFactory(ClassLoader cl) {
        super();
        this.cl = cl;
    }

    public Object createObject(Attributes attributes) {
        // Identify the name of the class to instantiate
        String className = attributes.getValue("className");

        if (className == null) {
            ModuleConfig mc = (ModuleConfig) digester.peek();

            className = mc.getActionForwardClass();
        }

        // Instantiate the new object and return it
        Object globalForward = null;

        try {
            globalForward = RequestUtils.applicationInstance(className, cl);
        } catch (Exception e) {
            digester.getLogger().error("GlobalForwardFactory.createObject: ", e);
        }

        return globalForward;
    }
}


/**
 * <p> An object creation factory which creates action forward instances,
 * taking into account the default class name, which may have been specified
 * on the parent element and which is made available through the object on the
 * top of the stack, which must be a <code>org.apache.struts.config.ModuleConfig</code>.
 * </p>
 */
final class ActionForwardFactory extends AbstractObjectCreationFactory {
    private ClassLoader cl;

    public ActionForwardFactory(ClassLoader cl) {
        super();
        this.cl = cl;
    }

    public Object createObject(Attributes attributes) {
        // Identify the name of the class to instantiate
        String className = attributes.getValue("className");

        if (className == null) {
            ModuleConfig mc = (ModuleConfig) digester.peek(1);

            className = mc.getActionForwardClass();
        }

        // Instantiate the new object and return it
        Object actionForward = null;

        try {
            actionForward = RequestUtils.applicationInstance(className, cl);
        } catch (Exception e) {
            digester.getLogger().error("ActionForwardFactory.createObject: ", e);
        }

        return actionForward;
    }
}
