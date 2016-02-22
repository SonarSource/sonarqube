/*
 * $Id: TestActionRedirect.java 514052 2007-03-03 02:00:37Z pbenedict $
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

import junit.framework.AssertionFailedError;
import junit.framework.ComparisonFailure;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Map;

/**
 * <p>Unit tests for the {@link ActionRedirect} class.</p>
 *
 * @version $Rev: 514052 $ $Date: 2007-03-03 03:00:37 +0100 (Sat, 03 Mar 2007) $
 */
public class TestActionRedirect extends TestCase {
    public TestActionRedirect(String s) {
        super(s);
    }

    public static TestSuite getSuite() {
        return new TestSuite(TestActionRedirect.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner runner = new junit.textui.TestRunner();

        runner.doRun(TestActionRedirect.getSuite());
    }

    // ----------------------------------------------------- Test Methods

    /**
     * Check that the redirect flag is set.
     */
    public void testActionRedirectRedirectFlag() {
        ActionRedirect ar = new ActionRedirect("/path.do");

        assertTrue("Redirect flag should be set to true.", ar.getRedirect());
    }

    /**
     * Test all addParameter methods accepting different data types.
     */
    public void testActionRedirectAddParameter() {
        ActionRedirect ar = new ActionRedirect("/path.do");

        ar.addParameter("st", "test");
        ar.addParameter("obj", new StringBuffer("someString"));

        assertTrue("Incorrect path", ar.getPath().indexOf("/path.do") == 0);
        assertHasParameter(ar.parameterValues, "st", "test");
        assertHasParameter(ar.parameterValues, "obj", "someString");
    }

    /**
     * Test redirect with anchor.
     */
    public void testActionRedirectWithAnchor() {
        ActionRedirect ar = new ActionRedirect("/path.do");

        ar.addParameter("st", "test");
        ar.setAnchor("foo");

        assertTrue("Incorrect path", "/path.do?st=test#foo".equals(ar.getPath()));
    }

    /**
     * Test adding parameters with the same name.
     */
    public void testActionRedirectAddSameNameParameter() {
        ActionRedirect ar = new ActionRedirect("/path.do");

        ar.addParameter("param", "param1");
        ar.addParameter("param", "param2");
        ar.addParameter("param", new StringBuffer("someString"));

        assertTrue("Incorrect path", ar.getPath().indexOf("/path.do") == 0);
        assertHasParameter(ar.parameterValues, "param", "param1");
        assertHasParameter(ar.parameterValues, "param", "param2");
        assertHasParameter(ar.parameterValues, "param", "someString");
        assertEquals("Incorrect number of parameters", 3,
            countParameters(ar.parameterValues, "param"));
    }

    /**
     * Test creating an ActionRedirect which copies its configuration from an
     * existing ActionForward (except for the "redirect" property).
     */
    public void testActionRedirectFromExistingForward() {
        ActionForward forward = new ActionForward("/path.do?param=param1");
        forward.setRedirect(false);
        forward.setProperty("key","value");

        ActionRedirect ar = new ActionRedirect(forward);

        ar.addParameter("param", "param2");
        ar.addParameter("object1", new StringBuffer("someString"));

        assertTrue("Incorrect path", ar.getPath().indexOf("/path.do") == 0);
        assertHasParameter(ar.parameterValues, "param", "param2");
        assertHasParameter(ar.parameterValues, "object1", "someString");
        assertEquals("Incorrect original path.", forward.getPath(),
            ar.getOriginalPath());
        assertEquals("Incorrect or missing property", "value",
            ar.getProperty("key"));
        assertTrue("Original had redirect to false", ar.getRedirect());
    }

    /**
     * Assert that the given parameters contains an entry for
     * <code>paramValue</code> under the <code>paramName</code> key. <p/>
     *
     * @param parameters the map of parameters to check into
     * @param paramName  the key of the value to be checked
     * @param paramValue the value to check for
     */
    static void assertHasParameter(Map parameters, String paramName,
        String paramValue) {
        Object value = parameters.get(paramName);

        if (value == null) {
            throw new AssertionFailedError("Parameter [" + paramName
                + "] not found");
        }

        if (value instanceof String) {
            if (!paramValue.equals(value)) {
                throw new ComparisonFailure("Incorrect value found",
                    paramValue, (String) value);
            }
        } else if (value instanceof String[]) {
            // see if our value is among those in the array
            String[] values = (String[]) value;

            for (int i = 0; i < values.length; i++) {
                if (paramValue.equals(values[i])) {
                    return;
                }
            }

            throw new AssertionFailedError(
                "Expected value not found for parameter [" + paramName + "]");
        } else {
            // can't recognize the value
            throw new AssertionFailedError(
                "Unexpected type found as parameter value for [" + paramName
                + "]");
        }
    }

    /**
     * Determine the number of values that are available for a specific
     * parameter. <p/>
     *
     * @param parameters the map of parameters to check into
     * @param paramName  the key of the value(s) to count
     * @return the number of values for the specified parameter
     */
    static int countParameters(Map parameters, String paramName) {
        Object value = parameters.get(paramName);

        if (value == null) {
            return 0;
        }

        if (value instanceof String) {
            return 1;
        } else if (value instanceof String[]) {
            String[] values = (String[]) value;

            return values.length;
        } else {
            // can't recognize the value
            throw new AssertionFailedError(
                "Unexpected type found as parameter value for [" + paramName
                + "]");
        }
    }
}
