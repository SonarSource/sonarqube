/*
 * $Id: TestActionMessage.java 471754 2006-11-06 14:55:09Z husted $
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

/**
 * Unit tests for the <code>org.apache.struts.action.ActionMessage</code>
 * class.
 *
 * @version $Rev: 471754 $ $Date: 2005-05-19 23:50:06 -0400 (Thu, 19 May 2005)
 *          $
 */
public class TestActionMessage extends TestCase {
    protected ActionMessage amWithNoValue = null;
    protected ActionMessage amWithOneValue = null;
    protected ActionMessage amWithTwoValues = null;
    protected ActionMessage amWithThreeValues = null;
    protected ActionMessage amWithFourValues = null;
    protected ActionMessage amWithArrayValues = null;
    protected ActionMessage amWithTwoIntegerValues = null;
    protected ActionMessage amNoResource = null;
    protected Object[] test_values =
        new Object[] {
            "stringValue1", "stringValue2", "stringValue3", "stringValue4"
        };

    /**
     * Defines the testcase name for JUnit.
     *
     * @param theName the testcase's name.
     */
    public TestActionMessage(String theName) {
        super(theName);
    }

    /**
     * Start the tests.
     *
     * @param theArgs the arguments. Not used
     */
    public static void main(String[] theArgs) {
        junit.awtui.TestRunner.main(new String[] {
                TestActionMessage.class.getName()
            });
    }

    /**
     * @return a test suite (<code>TestSuite</code>) that includes all methods
     *         starting with "test"
     */
    public static Test suite() {
        // All methods starting with "test" will be executed in the test suite.
        return new TestSuite(TestActionMessage.class);
    }

    public void setUp() {
        amWithNoValue = new ActionMessage("amWithNoValue");
        amWithOneValue =
            new ActionMessage("amWithOneValue", new String("stringValue"));
        amWithTwoValues =
            new ActionMessage("amWithTwoValues", new String("stringValue1"),
                new String("stringValue2"));
        amWithThreeValues =
            new ActionMessage("amWithThreeValues", new String("stringValue1"),
                new String("stringValue2"), new String("stringValue3"));
        amWithFourValues =
            new ActionMessage("amWithFourValues", new String("stringValue1"),
                new String("stringValue2"), new String("stringValue3"),
                new String("stringValue4"));
        amWithArrayValues = new ActionMessage("amWithArrayValues", test_values);
        amWithTwoIntegerValues =
            new ActionMessage("amWithTwoIntegerValues", new Integer(5),
                new Integer(10));
        amNoResource = new ActionMessage("amNoResource", false);
    }

    public void tearDown() {
        amWithNoValue = null;
        amWithOneValue = null;
        amWithTwoValues = null;
        amWithThreeValues = null;
        amWithFourValues = null;
        amWithArrayValues = null;
        amWithTwoIntegerValues = null;
        amNoResource = null;
    }

    public void testActionMessageWithNoValue() {
        assertTrue(amWithNoValue.getValues() == null);
        assertTrue(amWithNoValue.isResource());
        assertTrue(amWithNoValue.getKey() == "amWithNoValue");
        assertTrue(amWithNoValue.toString().equals("amWithNoValue[]"));
    }

    public void testActionMessageWithAStringValue() {
        Object[] values = amWithOneValue.getValues();

        assertTrue(values != null);
        assertTrue(values.length == 1);
        assertTrue(values[0].equals("stringValue"));
        assertTrue(amWithOneValue.isResource());
        assertTrue(amWithOneValue.getKey() == "amWithOneValue");
        assertTrue(amWithOneValue.toString().equals("amWithOneValue[stringValue]"));
    }

    public void testActionMessageWithTwoValues() {
        Object[] values = amWithTwoValues.getValues();

        assertTrue(values != null);
        assertTrue(values.length == 2);
        assertTrue(values[0].equals("stringValue1"));
        assertTrue(values[1].equals("stringValue2"));
        assertTrue(amWithTwoValues.isResource());
        assertTrue(amWithTwoValues.getKey() == "amWithTwoValues");
        assertTrue(amWithTwoValues.toString().equals("amWithTwoValues[stringValue1, stringValue2]"));
    }

    public void testActionMessageWithThreeValues() {
        Object[] values = amWithThreeValues.getValues();

        assertTrue(values != null);
        assertTrue(values.length == 3);
        assertTrue(values[0].equals("stringValue1"));
        assertTrue(values[1].equals("stringValue2"));
        assertTrue(values[2].equals("stringValue3"));
        assertTrue(amWithThreeValues.getKey() == "amWithThreeValues");
        assertTrue(amWithThreeValues.isResource());
        assertTrue(amWithThreeValues.toString().equals("amWithThreeValues[stringValue1, stringValue2, stringValue3]"));
    }

    public void testActionMessageWithFourValues() {
        Object[] values = amWithFourValues.getValues();

        assertTrue(values != null);
        assertTrue(values.length == 4);
        assertTrue(values[0].equals("stringValue1"));
        assertTrue(values[1].equals("stringValue2"));
        assertTrue(values[2].equals("stringValue3"));
        assertTrue(values[3].equals("stringValue4"));
        assertTrue(amWithFourValues.isResource());
        assertTrue(amWithFourValues.getKey() == "amWithFourValues");
        assertTrue(amWithFourValues.toString().equals("amWithFourValues[stringValue1, stringValue2, stringValue3, stringValue4]"));
    }

    public void testActionMessageWithArrayValues() {
        Object[] values = amWithArrayValues.getValues();

        assertTrue(values != null);
        assertTrue(values.length == test_values.length);

        for (int i = 0; i < values.length; i++) {
            assertTrue(values[i] == test_values[i]);
        }

        assertTrue(amWithArrayValues.isResource());
        assertTrue(amWithArrayValues.getKey() == "amWithArrayValues");
        assertTrue(amWithArrayValues.toString().equals("amWithArrayValues[stringValue1, stringValue2, stringValue3, stringValue4]"));
    }

    public void testActionWithTwoIntegers() {
        Object[] values = amWithTwoIntegerValues.getValues();

        assertTrue(values != null);
        assertTrue(values.length == 2);
        assertTrue(values[0] instanceof Integer);
        assertTrue(values[0].toString().equals("5"));
        assertTrue(values[1] instanceof Integer);
        assertTrue(values[1].toString().equals("10"));
        assertTrue(amWithTwoIntegerValues.isResource());
        assertTrue(amWithTwoIntegerValues.getKey() == "amWithTwoIntegerValues");
        assertTrue(amWithTwoIntegerValues.toString().equals("amWithTwoIntegerValues[5, 10]"));
    }

    public void testActionNoResource() {
        assertTrue(amNoResource.getValues() == null);
        assertTrue(amNoResource.isResource() == false);
        assertTrue(amNoResource.getKey() == "amNoResource");
        assertTrue(amNoResource.toString().equals("amNoResource[]"));
    }
}
