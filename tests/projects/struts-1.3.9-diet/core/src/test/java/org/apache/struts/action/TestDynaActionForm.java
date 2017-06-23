/*
 * $Id: TestDynaActionForm.java 471754 2006-11-06 14:55:09Z husted $
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
import junit.framework.TestSuite;

import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.config.FormBeanConfig;
import org.apache.struts.config.ModuleConfig;
import org.apache.struts.config.impl.ModuleConfigImpl;
import org.apache.struts.mock.MockHttpServletRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Suite of unit tests for the <code>org.apache.struts.action.DynaActionForm</code>
 * class.
 */
public class TestDynaActionForm extends TestDynaActionFormClass {
    /**
     * The set of property names we expect to have returned when calling
     * <code>getDynaProperties()</code>.  You should update this list when new
     * properties are added to TestBean.
     */
    protected final static String[] properties =
        {
            "booleanProperty", "booleanSecond", "doubleProperty",
            "floatProperty", "intArray", "intIndexed", "intProperty",
            "listIndexed", "longProperty", "mappedProperty", "mappedIntProperty",


            //        "nullProperty",
            "shortProperty", "stringArray", "stringIndexed", "stringProperty",
        };

    // ----------------------------------------------------- Instance Variables

    /**
     * Dummy ModuleConfig for calls to reset() and validate().
     */
    protected ModuleConfig moduleConfig = null;

    /**
     * The basic <code>DynaActionForm</code> to use for testing.
     */
    protected DynaActionForm dynaForm = null;

    /**
     * Dummy ActionMapping for calls to reset() and validate().
     */
    protected ActionMapping mapping = null;
    protected Log log = null;

    /**
     * Defines the testcase name for JUnit.
     *
     * @param theName the testcase's name.
     */
    public TestDynaActionForm(String theName) {
        super(theName);
    }

    /**
     * Start the tests.
     *
     * @param theArgs the arguments. Not used
     */
    public static void main(String[] theArgs) {
        junit.awtui.TestRunner.main(new String[] {
                TestDynaActionForm.class.getName()
            });
    }

    /**
     * @return a test suite (<code>TestSuite</code>) that includes all methods
     *         starting with "test"
     */
    public static Test suite() {
        // All methods starting with "test" will be executed in the test suite.
        return new TestSuite(TestDynaActionForm.class);
    }

    // ----------------------------------------------------- Setup and Teardown
    public void setUp() {
        super.setUp();

        try {
            dynaForm = (DynaActionForm) dynaClass.newInstance();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e.getMessage());
        } catch (InstantiationException e) {
            throw new RuntimeException(e.getMessage());
        }

        setupComplexProperties();
        moduleConfig = new DynaActionFormConfig(beanConfig);
        mapping = new DynaActionFormMapping(moduleConfig);
        log = LogFactory.getLog(this.getClass().getName() + "."
                + this.getName());
    }

    public void tearDown() {
        super.tearDown();
        moduleConfig = null;
        dynaForm = null;
        mapping = null;
    }

    // --------------------------------------------- Create New DynaActionForms
    // Test basic form bean properties on creation
    public void testBeanCreate() {
        assertEquals("booleanProperty", Boolean.TRUE,
            (Boolean) dynaForm.get("booleanProperty"));
        assertEquals("booleanSecond", Boolean.TRUE,
            (Boolean) dynaForm.get("booleanSecond"));
        assertEquals("doubleProperty", new Double(321.0),
            (Double) dynaForm.get("doubleProperty"));
        assertEquals("floatProperty", new Float((float) 123.0),
            (Float) dynaForm.get("floatProperty"));
        assertEquals("intProperty", new Integer(123),
            (Integer) dynaForm.get("intProperty"));

        // FIXME - listIndexed
        assertEquals("longProperty", new Long((long) 321),
            (Long) dynaForm.get("longProperty"));

        // FIXME - mappedProperty
        // FIXME - mappedIntProperty
        //        assertEquals("nullProperty", (String) null,
        //                     (String) dynaForm.get("nullProperty"));
        assertEquals("shortProperty", new Short((short) 987),
            (Short) dynaForm.get("shortProperty"));
        assertEquals("stringProperty", "This is a string",
            (String) dynaForm.get("stringProperty"));
    }

    // Test initialize() method on indexed values to ensure that the
    // result returned by FormPropertyConfig().initial() is never clobbered
    public void testIndexedInitialize() {
        // Update some values in the indexed properties
        dynaForm.set("intArray", 1, new Integer(111));
        assertEquals("intArray[1]", new Integer(111),
            (Integer) dynaForm.get("intArray", 1));
        dynaForm.set("intIndexed", 2, new Integer(222));
        assertEquals("intIndexed[2]", new Integer(222),
            (Integer) dynaForm.get("intIndexed", 2));
        dynaForm.set("stringArray", 3, "New String 3");
        assertEquals("stringArray[3]", "New String 3",
            (String) dynaForm.get("stringArray", 3));
        dynaForm.set("stringIndexed", 4, "New String 4");
        assertEquals("stringIndexed[4]", "New String 4",
            (String) dynaForm.get("stringIndexed", 4));

        // Perform initialize() and revalidate the original values
        // while ensuring our initial values did not get corrupted
        dynaForm.initialize(mapping);
        setupComplexProperties();
        testGetIndexedValues();
    }

    // Test initialize() method going back to initial values
    public void testScalarInitialize() {
        // Update a bunch of scalar properties to new values
        dynaForm.set("booleanProperty", Boolean.FALSE);
        assertEquals("booleanProperty", Boolean.FALSE,
            (Boolean) dynaForm.get("booleanProperty"));
        dynaForm.set("booleanSecond", Boolean.FALSE);
        dynaForm.set("doubleProperty", new Double(654.0));
        dynaForm.set("floatProperty", new Float((float) 543.0));
        dynaForm.set("intProperty", new Integer(555));
        dynaForm.set("longProperty", new Long((long) 777));
        dynaForm.set("shortProperty", new Short((short) 222));
        dynaForm.set("stringProperty", "New String Value");
        assertEquals("stringProperty", "New String Value",
            (String) dynaForm.get("stringProperty"));

        // Perform initialize() and revalidate the original values
        dynaForm.initialize(mapping);
        setupComplexProperties();
        testBeanCreate();
    }

    // --------------------------------------- Tests from BasicDynaBeanTestCase

    /**
     * Corner cases on getDynaProperty invalid arguments.
     */
    public void testGetDescriptorArguments() {
        DynaProperty descriptor =
            dynaForm.getDynaClass().getDynaProperty("unknown");

        assertNull("Unknown property descriptor should be null", descriptor);

        try {
            dynaForm.getDynaClass().getDynaProperty(null);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            ; // Expected response
        }
    }

    /**
     * Positive getDynaProperty on property <code>booleanProperty</code>.
     */
    public void testGetDescriptorBoolean() {
        testGetDescriptorBase("booleanProperty", Boolean.TYPE);
    }

    /**
     * Positive getDynaProperty on property <code>doubleProperty</code>.
     */
    public void testGetDescriptorDouble() {
        testGetDescriptorBase("doubleProperty", Double.TYPE);
    }

    /**
     * Positive getDynaProperty on property <code>floatProperty</code>.
     */
    public void testGetDescriptorFloat() {
        testGetDescriptorBase("floatProperty", Float.TYPE);
    }

    /**
     * Positive getDynaProperty on property <code>intProperty</code>.
     */
    public void testGetDescriptorInt() {
        testGetDescriptorBase("intProperty", Integer.TYPE);
    }

    /**
     * Positive getDynaProperty on property <code>longProperty</code>.
     */
    public void testGetDescriptorLong() {
        testGetDescriptorBase("longProperty", Long.TYPE);
    }

    /**
     * Positive getDynaProperty on property <code>booleanSecond</code> that
     * uses an "is" method as the getter.
     */
    public void testGetDescriptorSecond() {
        testGetDescriptorBase("booleanSecond", Boolean.TYPE);
    }

    /**
     * Positive getDynaProperty on property <code>shortProperty</code>.
     */
    public void testGetDescriptorShort() {
        testGetDescriptorBase("shortProperty", Short.TYPE);
    }

    /**
     * Positive getDynaProperty on property <code>stringProperty</code>.
     */
    public void testGetDescriptorString() {
        testGetDescriptorBase("stringProperty", String.class);
    }

    /**
     * Positive test for getDynaPropertys().  Each property name listed in
     * <code>properties</code> should be returned exactly once.
     */
    public void testGetDescriptors() {
        DynaProperty[] pd = dynaForm.getDynaClass().getDynaProperties();

        assertNotNull("Got descriptors", pd);

        int[] count = new int[properties.length];

        for (int i = 0; i < pd.length; i++) {
            String name = pd[i].getName();

            for (int j = 0; j < properties.length; j++) {
                if (name.equals(properties[j])) {
                    count[j]++;
                }
            }
        }

        for (int j = 0; j < properties.length; j++) {
            if (count[j] < 0) {
                fail("Missing property " + properties[j]);
            } else if (count[j] > 1) {
                fail("Duplicate property " + properties[j]);
            }
        }
    }

    /**
     * Corner cases on getIndexedProperty invalid arguments.
     */
    public void testGetIndexedArguments() {
        try {
            dynaForm.get("intArray", -1);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            ; // Expected response
        }
    }

    /**
     * Positive and negative tests on getIndexedProperty valid arguments.
     */
    public void testGetIndexedValues() {
        Object value = null;

        for (int i = 0; i < 5; i++) {
            value = dynaForm.get("intArray", i);
            assertNotNull("intArray returned value " + i, value);
            assertTrue("intArray returned Integer " + i,
                value instanceof Integer);
            assertEquals("intArray returned correct " + i, i * 10,
                ((Integer) value).intValue());

            value = dynaForm.get("intIndexed", i);
            assertNotNull("intIndexed returned value " + i, value);
            assertTrue("intIndexed returned Integer " + i,
                value instanceof Integer);
            assertEquals("intIndexed returned correct " + i, i * 100,
                ((Integer) value).intValue());

            value = dynaForm.get("listIndexed", i);
            assertNotNull("listIndexed returned value " + i, value);
            assertTrue("list returned String " + i, value instanceof String);
            assertEquals("listIndexed returned correct " + i, "String " + i,
                (String) value);

            value = dynaForm.get("stringArray", i);
            assertNotNull("stringArray returned value " + i, value);
            assertTrue("stringArray returned String " + i,
                value instanceof String);
            assertEquals("stringArray returned correct " + i, "String " + i,
                (String) value);

            value = dynaForm.get("stringIndexed", i);
            assertNotNull("stringIndexed returned value " + i, value);
            assertTrue("stringIndexed returned String " + i,
                value instanceof String);
            assertEquals("stringIndexed returned correct " + i, "String " + i,
                (String) value);
        }
    }

    /**
     * Corner cases on getMappedProperty invalid arguments.
     */
    public void testGetMappedArguments() {
        Object value = dynaForm.get("mappedProperty", "unknown");

        assertNull("Should not return a value", value);
    }

    /**
     * Positive and negative tests on getMappedProperty valid arguments.
     */
    public void testGetMappedValues() {
        Object value = null;

        value = dynaForm.get("mappedProperty", "First Key");
        assertEquals("Can find first value", "First Value", value);

        value = dynaForm.get("mappedProperty", "Second Key");
        assertEquals("Can find second value", "Second Value", value);

        value = dynaForm.get("mappedProperty", "Third Key");
        assertNull("Can not find third value", value);
    }

    /**
     * Corner cases on getSimpleProperty invalid arguments.
     */
    public void testGetSimpleArguments() {
        try {
            dynaForm.get(null);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            ; // Expected response
        }
    }

    /**
     * Test getSimpleProperty on a boolean property.
     */
    public void testGetSimpleBoolean() {
        Object value = dynaForm.get("booleanProperty");

        assertNotNull("Got a value", value);
        assertTrue("Got correct type", (value instanceof Boolean));
        assertTrue("Got correct value", ((Boolean) value).booleanValue() == true);
    }

    /**
     * Test getSimpleProperty on a double property.
     */
    public void testGetSimpleDouble() {
        Object value = dynaForm.get("doubleProperty");

        assertNotNull("Got a value", value);
        assertTrue("Got correct type", (value instanceof Double));
        assertEquals("Got correct value", ((Double) value).doubleValue(),
            (double) 321.0, (double) 0.005);
    }

    /**
     * Test getSimpleProperty on a float property.
     */
    public void testGetSimpleFloat() {
        Object value = dynaForm.get("floatProperty");

        assertNotNull("Got a value", value);
        assertTrue("Got correct type", (value instanceof Float));
        assertEquals("Got correct value", ((Float) value).floatValue(),
            (float) 123.0, (float) 0.005);
    }

    /**
     * Test getSimpleProperty on a int property.
     */
    public void testGetSimpleInt() {
        Object value = dynaForm.get("intProperty");

        assertNotNull("Got a value", value);
        assertTrue("Got correct type", (value instanceof Integer));
        assertEquals("Got correct value", ((Integer) value).intValue(),
            (int) 123);
    }

    /**
     * Test getSimpleProperty on a long property.
     */
    public void testGetSimpleLong() {
        Object value = dynaForm.get("longProperty");

        assertNotNull("Got a value", value);
        assertTrue("Got correct type", (value instanceof Long));
        assertEquals("Got correct value", ((Long) value).longValue(), (long) 321);
    }

    /**
     * Test getSimpleProperty on a short property.
     */
    public void testGetSimpleShort() {
        Object value = dynaForm.get("shortProperty");

        assertNotNull("Got a value", value);
        assertTrue("Got correct type", (value instanceof Short));
        assertEquals("Got correct value", ((Short) value).shortValue(),
            (short) 987);
    }

    /**
     * Test getSimpleProperty on a String property.
     */
    public void testGetSimpleString() {
        Object value = dynaForm.get("stringProperty");

        assertNotNull("Got a value", value);
        assertTrue("Got correct type", (value instanceof String));
        assertEquals("Got correct value", (String) value, "This is a string");
    }

    /**
     * Test <code>contains()</code> method for mapped properties.
     */
    public void testMappedContains() {
        assertTrue("Can see first key",
            dynaForm.contains("mappedProperty", "First Key"));

        assertTrue("Can not see unknown key",
            !dynaForm.contains("mappedProperty", "Unknown Key"));
    }

    /**
     * Test <code>remove()</code> method for mapped properties.
     */
    public void testMappedRemove() {
        assertTrue("Can see first key",
            dynaForm.contains("mappedProperty", "First Key"));
        dynaForm.remove("mappedProperty", "First Key");
        assertTrue("Can not see first key",
            !dynaForm.contains("mappedProperty", "First Key"));

        assertTrue("Can not see unknown key",
            !dynaForm.contains("mappedProperty", "Unknown Key"));
        dynaForm.remove("mappedProperty", "Unknown Key");
        assertTrue("Can not see unknown key",
            !dynaForm.contains("mappedProperty", "Unknown Key"));
    }

    /**
     * Test the reset method when the request method is GET.
     */
    public void testResetGet() {
        // set a choice set of props with non-initial values
        dynaForm.set("booleanProperty", Boolean.FALSE);
        dynaForm.set("booleanSecond", Boolean.FALSE);
        dynaForm.set("doubleProperty", new Double(456.0));
        dynaForm.set("floatProperty", new Float((float) 456.0));
        dynaForm.set("intProperty", new Integer(456));

        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setMethod("GET");
        dynaForm.reset(mapping, request);

        assertEquals("booleanProperty should be reset", Boolean.TRUE,
            (Boolean) dynaForm.get("booleanProperty"));
        assertEquals("booleanSecond should be reset", Boolean.TRUE,
            (Boolean) dynaForm.get("booleanSecond"));
        assertEquals("doubleProperty should be reset", new Double(321.0),
            (Double) dynaForm.get("doubleProperty"));
        assertEquals("floatProperty should NOT be reset",
            new Float((float) 456.0), (Float) dynaForm.get("floatProperty"));
        assertEquals("intProperty should NOT be reset", new Integer(456),
            (Integer) dynaForm.get("intProperty"));
    }

    /**
     * Test the reset method when the request method is GET.
     */
    public void testResetPost() {
        // set a choice set of props with non-initial values
        dynaForm.set("booleanProperty", Boolean.FALSE);
        dynaForm.set("booleanSecond", Boolean.FALSE);
        dynaForm.set("doubleProperty", new Double(456.0));
        dynaForm.set("floatProperty", new Float((float) 456.0));
        dynaForm.set("intProperty", new Integer(456));

        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setMethod("POST");
        dynaForm.reset(mapping, request);

        assertEquals("booleanProperty should be reset", Boolean.TRUE,
            (Boolean) dynaForm.get("booleanProperty"));
        assertEquals("booleanSecond should be reset", Boolean.TRUE,
            (Boolean) dynaForm.get("booleanSecond"));
        assertEquals("doubleProperty should NOT be reset", new Double(456),
            (Double) dynaForm.get("doubleProperty"));
        assertEquals("floatProperty should be reset", new Float((float) 123.0),
            (Float) dynaForm.get("floatProperty"));
        assertEquals("intProperty should NOT be reset", new Integer(456),
            (Integer) dynaForm.get("intProperty"));
    }

    /**
     * Corner cases on setIndexedProperty invalid arguments.
     */
    public void testSetIndexedArguments() {
        try {
            dynaForm.set("intArray", -1, new Integer(0));
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            ; // Expected response
        }
    }

    /**
     * Positive and negative tests on setIndexedProperty valid arguments.
     */
    public void testSetIndexedValues() {
        Object value = null;

        dynaForm.set("intArray", 0, new Integer(1));
        value = (Integer) dynaForm.get("intArray", 0);
        assertNotNull("Returned new value 0", value);
        assertTrue("Returned Integer new value 0", value instanceof Integer);
        assertEquals("Returned correct new value 0", 1,
            ((Integer) value).intValue());

        dynaForm.set("intIndexed", 1, new Integer(11));
        value = (Integer) dynaForm.get("intIndexed", 1);
        assertNotNull("Returned new value 1", value);
        assertTrue("Returned Integer new value 1", value instanceof Integer);
        assertEquals("Returned correct new value 1", 11,
            ((Integer) value).intValue());
        dynaForm.set("listIndexed", 2, "New Value 2");
        value = (String) dynaForm.get("listIndexed", 2);
        assertNotNull("Returned new value 2", value);
        assertTrue("Returned String new value 2", value instanceof String);
        assertEquals("Returned correct new value 2", "New Value 2",
            (String) value);

        dynaForm.set("stringArray", 3, "New Value 3");
        value = (String) dynaForm.get("stringArray", 3);
        assertNotNull("Returned new value 3", value);
        assertTrue("Returned String new value 3", value instanceof String);
        assertEquals("Returned correct new value 3", "New Value 3",
            (String) value);

        dynaForm.set("stringIndexed", 4, "New Value 4");
        value = (String) dynaForm.get("stringIndexed", 4);
        assertNotNull("Returned new value 4", value);
        assertTrue("Returned String new value 4", value instanceof String);
        assertEquals("Returned correct new value 4", "New Value 4",
            (String) value);
    }

    /**
     * Positive and negative tests on setMappedProperty valid arguments.
     */
    public void testSetMappedValues() {
        dynaForm.set("mappedProperty", "First Key", "New First Value");
        assertEquals("Can replace old value", "New First Value",
            (String) dynaForm.get("mappedProperty", "First Key"));

        dynaForm.set("mappedProperty", "Fourth Key", "Fourth Value");
        assertEquals("Can set new value", "Fourth Value",
            (String) dynaForm.get("mappedProperty", "Fourth Key"));
    }

    /**
     * Test setSimpleProperty on a boolean property.
     */
    public void testSetSimpleBoolean() {
        boolean oldValue =
            ((Boolean) dynaForm.get("booleanProperty")).booleanValue();
        boolean newValue = !oldValue;

        dynaForm.set("booleanProperty", new Boolean(newValue));
        assertTrue("Matched new value",
            newValue == ((Boolean) dynaForm.get("booleanProperty"))
            .booleanValue());
    }

    /**
     * Test setSimpleProperty on a double property.
     */
    public void testSetSimpleDouble() {
        double oldValue =
            ((Double) dynaForm.get("doubleProperty")).doubleValue();
        double newValue = oldValue + 1.0;

        dynaForm.set("doubleProperty", new Double(newValue));
        assertEquals("Matched new value", newValue,
            ((Double) dynaForm.get("doubleProperty")).doubleValue(),
            (double) 0.005);
    }

    /**
     * Test setSimpleProperty on a float property.
     */
    public void testSetSimpleFloat() {
        float oldValue = ((Float) dynaForm.get("floatProperty")).floatValue();
        float newValue = oldValue + (float) 1.0;

        dynaForm.set("floatProperty", new Float(newValue));
        assertEquals("Matched new value", newValue,
            ((Float) dynaForm.get("floatProperty")).floatValue(), (float) 0.005);
    }

    /**
     * Test setSimpleProperty on a int property.
     */
    public void testSetSimpleInt() {
        int oldValue = ((Integer) dynaForm.get("intProperty")).intValue();
        int newValue = oldValue + 1;

        dynaForm.set("intProperty", new Integer(newValue));
        assertEquals("Matched new value", newValue,
            ((Integer) dynaForm.get("intProperty")).intValue());
    }

    /**
     * Test setSimpleProperty on a long property.
     */
    public void testSetSimpleLong() {
        long oldValue = ((Long) dynaForm.get("longProperty")).longValue();
        long newValue = oldValue + 1;

        dynaForm.set("longProperty", new Long(newValue));
        assertEquals("Matched new value", newValue,
            ((Long) dynaForm.get("longProperty")).longValue());
    }

    /**
     * Test setSimpleProperty on a short property.
     */
    public void testSetSimpleShort() {
        short oldValue = ((Short) dynaForm.get("shortProperty")).shortValue();
        short newValue = (short) (oldValue + 1);

        dynaForm.set("shortProperty", new Short(newValue));
        assertEquals("Matched new value", newValue,
            ((Short) dynaForm.get("shortProperty")).shortValue());
    }

    /**
     * Test setSimpleProperty on a String property.
     */
    public void testSetSimpleString() {
        String oldValue = (String) dynaForm.get("stringProperty");
        String newValue = oldValue + " Extra Value";

        dynaForm.set("stringProperty", newValue);
        assertEquals("Matched new value", newValue,
            (String) dynaForm.get("stringProperty"));
    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Set up the complex properties that cannot be configured from the
     * initial value expression.
     */
    protected void setupComplexProperties() {
        List listIndexed = new ArrayList();

        listIndexed.add("String 0");
        listIndexed.add("String 1");
        listIndexed.add("String 2");
        listIndexed.add("String 3");
        listIndexed.add("String 4");
        dynaForm.set("listIndexed", listIndexed);

        Map mappedProperty = new HashMap();

        mappedProperty.put("First Key", "First Value");
        mappedProperty.put("Second Key", "Second Value");
        dynaForm.set("mappedProperty", mappedProperty);

        Map mappedIntProperty = new HashMap();

        mappedIntProperty.put("One", new Integer(1));
        mappedIntProperty.put("Two", new Integer(2));
        dynaForm.set("mappedIntProperty", mappedIntProperty);
    }

    /**
     * Base for testGetDescriptorXxxxx() series of tests.
     *
     * @param name Name of the property to be retrieved
     * @param type Expected class type of this property
     */
    protected void testGetDescriptorBase(String name, Class type) {
        DynaProperty descriptor = dynaForm.getDynaClass().getDynaProperty(name);

        assertNotNull("Got descriptor", descriptor);
        assertEquals("Got correct type", type, descriptor.getType());
    }
}


class DynaActionFormMapping extends ActionMapping {
    private ModuleConfig appConfig = null;

    public DynaActionFormMapping(ModuleConfig appConfig) {
        this.appConfig = appConfig;
    }

    public ModuleConfig getModuleConfig() {
        return (this.appConfig);
    }

    public String getName() {
        return ("dynaForm");
    }
}


class DynaActionFormConfig extends ModuleConfigImpl {
    private FormBeanConfig beanConfig = null;

    public DynaActionFormConfig(FormBeanConfig beanConfig) {
        super("");
        this.beanConfig = beanConfig;
    }

    public FormBeanConfig findFormBeanConfig(String name) {
        return (this.beanConfig);
    }
}
