/*
 * $Id: TestDynaActionFormClass.java 471754 2006-11-06 14:55:09Z husted $
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.beanutils.DynaProperty;
import org.apache.struts.config.FormBeanConfig;
import org.apache.struts.config.FormPropertyConfig;

/**
 * Suite of unit tests for the <code>org.apache.struts.action.DynaActionFormClass</code>
 * class.
 */
public class TestDynaActionFormClass extends TestCase {
    /**
     * The set of <code>FormPropertyConfig</code> objects to use when creating
     * our <code>FormBeanConfig</code>.
     */
    protected static final FormPropertyConfig[] dynaProperties =
        {
            new FormPropertyConfig("booleanProperty", "boolean", "true", "true"),
            new FormPropertyConfig("booleanSecond", "boolean", "true", "true"),
            new FormPropertyConfig("doubleProperty", "double", "321.0", "GET"),
            new FormPropertyConfig("floatProperty", "float", "123.0",
                "POST, HEAD"),
            new FormPropertyConfig("intArray", "int[]",
                "{ 0, 10,20, \"30\" '40' }"),
            new FormPropertyConfig("intIndexed", "int[]",
                " 0 100, 200, 300, 400 "),
            new FormPropertyConfig("intProperty", "int", "123"),
            new FormPropertyConfig("listIndexed", "java.util.List", null),
            new FormPropertyConfig("longProperty", "long", "321"),
            new FormPropertyConfig("mappedProperty", "java.util.Map", null),
            new FormPropertyConfig("mappedIntProperty", "java.util.Map", null),


            //        new FormPropertyConfig("nullProperty", "java.lang.String", null),
            new FormPropertyConfig("shortProperty", "short", "987"),
            new FormPropertyConfig("stringArray", "java.lang.String[]",
                "{ 'String 0', 'String 1', 'String 2', 'String 3', 'String 4'}"),
            new FormPropertyConfig("stringIndexed", "java.lang.String[]",
                "{ 'String 0', 'String 1', 'String 2', 'String 3', 'String 4'}"),
            new FormPropertyConfig("stringProperty", "java.lang.String",
                "This is a string"),
        };

    // ----------------------------------------------------- Instance Variables

    /**
     * The <code>FormBeanConfig</code> structure for the form bean we will be
     * creating.
     */
    protected FormBeanConfig beanConfig = null;

    /**
     * The <code>DynaActionFormClass</code> to use for testing.
     */
    protected DynaActionFormClass dynaClass = null;

    /**
     * Defines the testcase name for JUnit.
     *
     * @param theName the testcase's name.
     */
    public TestDynaActionFormClass(String theName) {
        super(theName);
    }

    /**
     * Start the tests.
     *
     * @param theArgs the arguments. Not used
     */
    public static void main(String[] theArgs) {
        junit.awtui.TestRunner.main(new String[] {
                TestDynaActionFormClass.class.getName()
            });
    }

    /**
     * @return a test suite (<code>TestSuite</code>) that includes all methods
     *         starting with "test"
     */
    public static Test suite() {
        // All methods starting with "test" will be executed in the test suite.
        return new TestSuite(TestDynaActionFormClass.class);
    }

    // ----------------------------------------------------- Setup and Teardown
    public void setUp() {
        // Construct a FormBeanConfig to be used
        beanConfig = new FormBeanConfig();
        beanConfig.setName("dynaForm");
        beanConfig.setType("org.apache.struts.action.DynaActionForm");

        // Add relevant property definitions
        for (int i = 0; i < dynaProperties.length; i++) {
            beanConfig.addFormPropertyConfig(dynaProperties[i]);
        }

        // Construct a corresponding DynaActionFormClass
        dynaClass = new DynaActionFormClass(beanConfig);
    }

    public void tearDown() {
        dynaClass = null;
        beanConfig = null;
    }

    // -------------------------------------------------- Verify FormBeanConfig
    // Check for ability to add a property before and after freezing
    public void testConfigAdd() {
        FormPropertyConfig prop = null;

        // Before freezing
        prop = beanConfig.findFormPropertyConfig("fooProperty");
        assertNull("fooProperty not found", prop);
        beanConfig.addFormPropertyConfig(new FormPropertyConfig("fooProperty",
                "java.lang.String", ""));
        prop = beanConfig.findFormPropertyConfig("fooProperty");
        assertNotNull("fooProperty found", prop);

        // after freezing
        beanConfig.freeze();
        prop = beanConfig.findFormPropertyConfig("barProperty");
        assertNull("barProperty not found", prop);

        try {
            beanConfig.addFormPropertyConfig(new FormPropertyConfig(
                    "barProperty", "java.lang.String", ""));
            fail("barProperty add not prevented");
        } catch (IllegalStateException e) {
            ; // Expected result
        }
    }

    // Check basic FormBeanConfig properties
    public void testConfigCreate() {
        assertTrue("dynamic is correct", beanConfig.getDynamic());
        assertEquals("name is correct", "dynaForm", beanConfig.getName());
        assertEquals("type is correct",
            "org.apache.struts.action.DynaActionForm", beanConfig.getType());
    }

    // Check attempts to add a duplicate property name
    public void testConfigDuplicate() {
        FormPropertyConfig prop = null;

        assertNull("booleanProperty is found", prop);

        try {
            beanConfig.addFormPropertyConfig(new FormPropertyConfig(
                    "booleanProperty", "java.lang.String", ""));
            fail("Adding duplicate property not prevented");
        } catch (IllegalArgumentException e) {
            ; // Expected result
        }
    }

    // Check the configured FormPropertyConfig element initial values
    public void testConfigInitialValues() {
        assertEquals("booleanProperty value", Boolean.TRUE,
            beanConfig.findFormPropertyConfig("booleanProperty").initial());
        assertEquals("booleanSecond value", Boolean.TRUE,
            beanConfig.findFormPropertyConfig("booleanSecond").initial());
        assertEquals("doubleProperty value", new Double(321.0),
            beanConfig.findFormPropertyConfig("doubleProperty").initial());
        assertEquals("floatProperty value", new Float((float) 123.0),
            beanConfig.findFormPropertyConfig("floatProperty").initial());
        assertEquals("intProperty value", new Integer(123),
            beanConfig.findFormPropertyConfig("intProperty").initial());

        // FIXME - listIndexed
        assertEquals("longProperty value", new Long(321),
            beanConfig.findFormPropertyConfig("longProperty").initial());

        // FIXME - mappedProperty
        // FIXME - mappedIntProperty
        //        assertNull("nullProperty value",
        //                   beanConfig.findFormPropertyConfig("nullProperty").initial());
        assertEquals("shortProperty value", new Short((short) 987),
            beanConfig.findFormPropertyConfig("shortProperty").initial());

        // FIXME - stringArray
        // FIXME - stringIndexed
        assertEquals("stringProperty value", "This is a string",
            beanConfig.findFormPropertyConfig("stringProperty").initial());
    }

    // Check the configured FormPropertyConfig element properties
    public void testConfigProperties() {
        for (int i = 0; i < dynaProperties.length; i++) {
            FormPropertyConfig dynaProperty =
                beanConfig.findFormPropertyConfig(dynaProperties[i].getName());

            assertNotNull("Found dynaProperty " + dynaProperties[i].getName(),
                dynaProperty);
            assertEquals("dynaProperty name for " + dynaProperties[i].getName(),
                dynaProperties[i].getName(), dynaProperty.getName());
            assertEquals("dynaProperty type for " + dynaProperties[i].getName(),
                dynaProperties[i].getType(), dynaProperty.getType());
            assertEquals("dynaProperty initial for "
                + dynaProperties[i].getName(), dynaProperties[i].getInitial(),
                dynaProperty.getInitial());
        }
    }

    // Check for ability to remove a property before and after freezing
    public void testConfigRemove() {
        FormPropertyConfig prop = null;

        // Before freezing
        prop = beanConfig.findFormPropertyConfig("booleanProperty");
        assertNotNull("booleanProperty found", prop);
        beanConfig.removeFormPropertyConfig(prop);
        prop = beanConfig.findFormPropertyConfig("booleanProperty");
        assertNull("booleanProperty not deleted", prop);

        // after freezing
        beanConfig.freeze();
        prop = beanConfig.findFormPropertyConfig("booleanSecond");
        assertNotNull("booleanSecond found", prop);

        try {
            beanConfig.removeFormPropertyConfig(prop);
            fail("booleanSecond remove not prevented");
        } catch (IllegalStateException e) {
            ; // Expected result
        }
    }

    // --------------------------------------------- Create DynaActionFormClass
    // Test basic DynaActionFormClass name and properties
    public void testClassCreate() {
        assertEquals("name", "dynaForm", dynaClass.getName());

        for (int i = 0; i < dynaProperties.length; i++) {
            DynaProperty prop =
                dynaClass.getDynaProperty(dynaProperties[i].getName());

            assertNotNull("Found property " + dynaProperties[i].getName());
            assertEquals("Class for property " + dynaProperties[i].getName(),
                dynaProperties[i].getTypeClass(), prop.getType());
        }
    }
}
