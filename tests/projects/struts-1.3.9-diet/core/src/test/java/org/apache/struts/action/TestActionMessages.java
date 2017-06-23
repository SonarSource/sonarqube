/*
 * $Id: TestActionMessages.java 471754 2006-11-06 14:55:09Z husted $
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

import java.util.Iterator;

/**
 * Unit tests for the <code>org.apache.struts.action.ActionMessages</code>
 * class.
 *
 * @version $Rev: 471754 $ $Date: 2004-10-16 12:09:25 -0400 (Sat, 16 Oct 2004)
 *          $
 */
public class TestActionMessages extends TestCase {
    protected ActionMessages aMsgs = null;
    protected ActionMessages anMsgs = null;
    protected ActionMessage msg1 = null;
    protected ActionMessage msg2 = null;
    protected ActionMessage msg3 = null;
    protected ActionMessage msg4 = null;
    protected ActionMessage msg5 = null;

    /**
     * Defines the testcase name for JUnit.
     *
     * @param theName the testcase's name.
     */
    public TestActionMessages(String theName) {
        super(theName);
    }

    /**
     * Start the tests.
     *
     * @param theArgs the arguments. Not used
     */
    public static void main(String[] theArgs) {
        junit.awtui.TestRunner.main(new String[] {
                TestActionMessages.class.getName()
            });
    }

    /**
     * @return a test suite (<code>TestSuite</code>) that includes all methods
     *         starting with "test"
     */
    public static Test suite() {
        // All methods starting with "test" will be executed in the test suite.
        return new TestSuite(TestActionMessages.class);
    }

    public void setUp() {
        aMsgs = new ActionMessages();
        anMsgs = new ActionMessages();

        Object[] objs1 = new Object[] { "a", "b", "c", "d", "e" };
        Object[] objs2 = new Object[] { "f", "g", "h", "i", "j" };

        msg1 = new ActionMessage("aMessage", objs1);
        msg2 = new ActionMessage("anMessage", objs2);
        msg3 = new ActionMessage("msg3", "value1");
        msg4 = new ActionMessage("msg4", "value2");
        msg5 = new ActionMessage("msg5", "value3", "value4");
    }

    public void tearDown() {
        aMsgs = null;
    }

    public void testEmpty() {
        assertTrue("aMsgs is not empty!", aMsgs.isEmpty());
    }

    public void testNotEmpty() {
        aMsgs.add("myProp", msg1);
        assertTrue("aMsgs is empty!", aMsgs.isEmpty() == false);
    }

    public void testSizeWithOneProperty() {
        aMsgs.add("myProp", msg1);
        aMsgs.add("myProp", msg2);
        assertTrue("number of mesages is not 2", aMsgs.size("myProp") == 2);
    }

    public void testSizeWithManyProperties() {
        aMsgs.add("myProp1", msg1);
        aMsgs.add("myProp2", msg2);
        aMsgs.add("myProp3", msg3);
        aMsgs.add("myProp3", msg4);
        aMsgs.add("myProp4", msg5);
        assertTrue("number of messages for myProp1 is not 1",
            aMsgs.size("myProp1") == 1);
        assertTrue("number of messages", aMsgs.size() == 5);
    }

    public void testSizeAndEmptyAfterClear() {
        testSizeWithOneProperty();
        aMsgs.clear();
        testEmpty();
        assertTrue("number of meesages is not 0", aMsgs.size("myProp") == 0);
    }

    public void testGetWithNoProperty() {
        Iterator it = aMsgs.get("myProp");

        assertTrue("iterator is not empty!", it.hasNext() == false);
    }

    public void testGetForAProperty() {
        testSizeWithOneProperty();

        Iterator it = aMsgs.get("myProp");

        assertTrue("iterator is empty!", it.hasNext() == true);
    }

    /**
     * Tests adding an ActionMessages object to an ActionMessages object.
     */
    public void testAddMessages() {
        ActionMessage msg1 = new ActionMessage("key");
        ActionMessage msg2 = new ActionMessage("key2");
        ActionMessage msg3 = new ActionMessage("key3");
        ActionMessages msgs = new ActionMessages();
        ActionMessages add = new ActionMessages();

        msgs.add("prop1", msg1);
        add.add("prop1", msg2);
        add.add("prop3", msg3);

        msgs.add(add);
        assertTrue(msgs.size() == 3);
        assertTrue(msgs.size("prop1") == 2);

        // test message order
        Iterator props = msgs.get();
        int count = 1;

        while (props.hasNext()) {
            ActionMessage msg = (ActionMessage) props.next();

            if (count == 1) {
                assertTrue(msg.getKey().equals("key"));
            } else if (count == 2) {
                assertTrue(msg.getKey().equals("key2"));
            } else {
                assertTrue(msg.getKey().equals("key3"));
            }

            count++;
        }
    }
}
