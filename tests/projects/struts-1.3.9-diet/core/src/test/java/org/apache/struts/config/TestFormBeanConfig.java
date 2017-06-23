/*
 * $Id: TestFormBeanConfig.java 471754 2006-11-06 14:55:09Z husted $
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.lang.reflect.InvocationTargetException;

/**
 * Unit tests for the <code>org.apache.struts.config.FormBeanConfig</code>
 * class.  Currently only contains code to test the methods that support
 * configuration inheritance.
 *
 * @version $Rev: 471754 $ $Date: 2005-05-25 19:35:00 -0400 (Wed, 25 May 2005)
 *          $
 */
public class TestFormBeanConfig extends TestCase {
    // ----------------------------------------------------- Instance Variables

    /**
     * The ModuleConfig we'll use.
     */
    protected ModuleConfig config = null;

    /**
     * The common base we'll use.
     */
    protected FormBeanConfig baseForm = null;

    // ----------------------------------------------------------- Constructors

    /**
     * Construct a new instance of this test case.
     *
     * @param name Name of the test case
     */
    public TestFormBeanConfig(String name) {
        super(name);
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Set up instance variables required by this test case.
     */
    public void setUp() {
        ModuleConfigFactory factoryObject = ModuleConfigFactory.createFactory();

        config = factoryObject.createModuleConfig("");

        // setup the base form
        baseForm = new FormBeanConfig();
        baseForm.setName("baseForm");
        baseForm.setType("org.apache.struts.action.DynaActionForm");

        // set up id, name, and score
        FormPropertyConfig property = new FormPropertyConfig();

        property.setName("id");
        property.setType("java.lang.String");
        baseForm.addFormPropertyConfig(property);

        property = new FormPropertyConfig();
        property.setName("name");
        property.setType("java.lang.String");
        property.setProperty("count", "10");
        baseForm.addFormPropertyConfig(property);

        property = new FormPropertyConfig();
        property.setName("score");
        property.setType("java.lang.String");
        baseForm.addFormPropertyConfig(property);

        property = new FormPropertyConfig();
        property.setName("message");
        property.setType("java.lang.String");
        baseForm.addFormPropertyConfig(property);

        // register it to our config
        config.addFormBeanConfig(baseForm);
    }

    /**
     * Return the tests included in this test suite.
     */
    public static Test suite() {
        return (new TestSuite(TestFormBeanConfig.class));
    }

    /**
     * Tear down instance variables required by this test case.
     */
    public void tearDown() {
        config = null;
        baseForm = null;
    }

    // ------------------------------------------------------- Individual Tests

    /**
     * Basic check that shouldn't detect an error.
     */
    public void testCheckCircularInheritance() {
        FormBeanConfig child = new FormBeanConfig();

        child.setName("child");
        child.setExtends("baseForm");

        FormBeanConfig grandChild = new FormBeanConfig();

        grandChild.setName("grandChild");
        grandChild.setExtends("child");

        config.addFormBeanConfig(child);
        config.addFormBeanConfig(grandChild);

        assertTrue("Circular inheritance shouldn't have been detected",
            !grandChild.checkCircularInheritance(config));
    }

    /**
     * Basic check that SHOULD detect an error.
     */
    public void testCheckCircularInheritanceError() {
        FormBeanConfig child = new FormBeanConfig();

        child.setName("child");
        child.setExtends("baseForm");

        FormBeanConfig grandChild = new FormBeanConfig();

        grandChild.setName("grandChild");
        grandChild.setExtends("child");

        // establish the circular relationship with base
        baseForm.setExtends("grandChild");

        config.addFormBeanConfig(child);
        config.addFormBeanConfig(grandChild);

        assertTrue("Circular inheritance should've been detected",
            grandChild.checkCircularInheritance(config));
    }

    /**
     * Test that processExtends() makes sure that a base form's own extension
     * has been processed.
     */
    public void testProcessExtendsBaseFormExtends()
        throws Exception {
        CustomFormBeanConfig first = new CustomFormBeanConfig();

        first.setName("first");

        CustomFormBeanConfig second = new CustomFormBeanConfig();

        second.setName("second");
        second.setExtends("first");

        config.addFormBeanConfig(first);
        config.addFormBeanConfig(second);

        // set baseForm to extend second
        baseForm.setExtends("second");

        baseForm.processExtends(config);

        assertTrue("The first form's processExtends() wasn't called",
            first.processExtendsCalled);
        assertTrue("The second form's processExtends() wasn't called",
            second.processExtendsCalled);
    }

    /**
     * Make sure that correct exception is thrown if a base form can't be
     * found.
     */
    public void testProcessExtendsMissingBaseForm()
        throws Exception {
        baseForm.setExtends("someMissingForm");

        try {
            baseForm.processExtends(config);
            fail(
                "An exception should be thrown if a super form can't be found.");
        } catch (NullPointerException e) {
            // succeed
        } catch (InstantiationException e) {
            fail("Unrecognized exception thrown.");
        }
    }

    /**
     * Test a typical form bean configuration extension where various
     * properties should be inherited from a base form.  This method checks
     * all the properties.
     */
    public void testInheritFrom()
        throws Exception {
        // give baseForm some arbitrary parameters
        String baseFormCount = "1";

        baseForm.setProperty("count", baseFormCount);

        // create a basic subform
        FormBeanConfig subForm = new FormBeanConfig();
        String subFormName = "subForm";

        subForm.setName(subFormName);
        subForm.setExtends("baseForm");

        // override score
        FormPropertyConfig property = new FormPropertyConfig();

        property.setName("score");
        property.setType("java.lang.Integer");
        subForm.addFormPropertyConfig(property);

        // ... and id
        property = new FormPropertyConfig();
        property.setName("id");
        property.setType("java.lang.String");
        property.setInitial("unknown");
        subForm.addFormPropertyConfig(property);

        // ... and message
        property = new FormPropertyConfig();
        property.setName("message");
        property.setType("java.lang.String");
        property.setSize(10);
        subForm.addFormPropertyConfig(property);

        config.addFormBeanConfig(subForm);

        subForm.inheritFrom(baseForm);

        // check that our subForm is still the one in the config
        assertSame("subForm no longer in ModuleConfig", subForm,
            config.findFormBeanConfig("subForm"));

        // check our configured sub form
        assertNotNull("Form bean type was not inherited", subForm.getType());
        assertEquals("Wrong form bean name", subFormName, subForm.getName());
        assertEquals("Wrong form bean type", baseForm.getType(),
            subForm.getType());
        assertEquals("Wrong restricted value", baseForm.isRestricted(),
            subForm.isRestricted());

        FormPropertyConfig[] formPropertyConfigs =
            subForm.findFormPropertyConfigs();

        assertEquals("Wrong prop count", 4, formPropertyConfigs.length);

        // we want to check that the form is EXACTLY as we want it, so
        //  here are some fine grain checks
        property = subForm.findFormPropertyConfig("name");

        FormPropertyConfig original = baseForm.findFormPropertyConfig("name");

        assertNotNull("'name' property was not inherited", property);
        assertEquals("Wrong type for name", original.getType(),
            property.getType());
        assertEquals("Wrong initial value for name", original.getInitial(),
            property.getInitial());
        assertEquals("Wrong size value for name", original.getSize(),
            property.getSize());

        property = subForm.findFormPropertyConfig("id");
        original = baseForm.findFormPropertyConfig("id");
        assertNotNull("'id' property was not found", property);
        assertEquals("Wrong type for id", original.getType(), property.getType());
        assertEquals("Wrong initial value for id", "unknown",
            property.getInitial());
        assertEquals("Wrong size value for id", original.getSize(),
            property.getSize());

        property = subForm.findFormPropertyConfig("score");
        original = baseForm.findFormPropertyConfig("score");
        assertNotNull("'score' property was not found", property);
        assertEquals("Wrong type for score", "java.lang.Integer",
            property.getType());
        assertEquals("Wrong initial value for score", original.getInitial(),
            property.getInitial());
        assertEquals("Wrong size value for score", original.getSize(),
            property.getSize());

        property = subForm.findFormPropertyConfig("message");
        original = baseForm.findFormPropertyConfig("message");
        assertNotNull("'message' property was not found", property);
        assertEquals("Wrong type for message", original.getType(),
            property.getType());
        assertEquals("Wrong initial value for message", original.getInitial(),
            property.getInitial());
        assertEquals("Wrong size value for message", 10, property.getSize());

        property = subForm.findFormPropertyConfig("name");
        original = baseForm.findFormPropertyConfig("name");
        assertEquals("Arbitrary property not found",
            original.getProperty("count"), property.getProperty("count"));

        String count = subForm.getProperty("count");

        assertEquals("Arbitrary property was not inherited", baseFormCount,
            count);
    }

    /**
     * Used to detect that FormBeanConfig is making the right calls.
     */
    public static class CustomFormBeanConfig extends FormBeanConfig {
        boolean processExtendsCalled = false;

        public void processExtends(ModuleConfig moduleConfig)
            throws ClassNotFoundException, IllegalAccessException,
                InstantiationException, InvocationTargetException {
            super.processExtends(moduleConfig);
            processExtendsCalled = true;
        }
    }
}
