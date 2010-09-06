/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.surefire;

import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.in.ElementFilter;
import org.codehaus.staxmate.in.SMEvent;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.utils.ParsingUtils;
import org.sonar.api.utils.StaxParser.XmlStreamHandler;

public class TestSuiteParser implements XmlStreamHandler {

  private Map<String, TestSuiteReport> reportsPerClass = new HashMap<String, TestSuiteReport>();

  public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
    SMInputCursor testSuite = rootCursor.constructDescendantCursor(new ElementFilter("testsuite"));
    SMEvent testSuiteEvent;
    while ((testSuiteEvent = testSuite.getNext()) != null) {
      if (testSuiteEvent.compareTo(SMEvent.START_ELEMENT) == 0) {
        String testSuiteClassName = testSuite.getAttrValue("name");
        if (StringUtils.contains(testSuiteClassName, "$")) {
          // test suites for inner classes are ignored
          return;
        }
        SMInputCursor testCase = testSuite.childCursor(new ElementFilter("testcase"));
        SMEvent event;
        while ((event = testCase.getNext()) != null) {
          if (event.compareTo(SMEvent.START_ELEMENT) == 0) {
            String testClassName = getClassname(testCase, testSuiteClassName);
            TestSuiteReport report = reportsPerClass.get(testClassName);
            if (report == null) {
              report = new TestSuiteReport(testClassName);
              reportsPerClass.put(testClassName, report);
            }
            cumulateTestCaseDetails(testCase, report);
          }
        }
      }
    }
  }

  public Collection<TestSuiteReport> getParsedReports() {
    return reportsPerClass.values();
  }

  private String getClassname(SMInputCursor testCaseCursor, String defaultClassname) throws XMLStreamException {
    String testClassName = testCaseCursor.getAttrValue("classname");
    testClassName = StringUtils.substringBeforeLast(testClassName, "$");
    return testClassName == null ? defaultClassname : testClassName;
  }

  private void cumulateTestCaseDetails(SMInputCursor testCaseCursor, TestSuiteReport report) throws XMLStreamException {
    TestCaseDetails detail = getTestCaseDetails(testCaseCursor);
    if (detail.getStatus().equals(TestCaseDetails.STATUS_SKIPPED)) {
      report.setSkipped(report.getSkipped() + 1);
    } else if (detail.getStatus().equals(TestCaseDetails.STATUS_FAILURE)) {
      report.setFailures(report.getFailures() + 1);
    } else if (detail.getStatus().equals(TestCaseDetails.STATUS_ERROR)) {
      report.setErrors(report.getErrors() + 1);
    }
    report.setTests(report.getTests() + 1);
    report.setTimeMS(report.getTimeMS() + detail.getTimeMS());
    report.getDetails().add(detail);
  }

  private void setStackAndMessage(TestCaseDetails detail, SMInputCursor stackAndMessageCursor) throws XMLStreamException {
    detail.setErrorMessage(stackAndMessageCursor.getAttrValue("message"));
    String stack = stackAndMessageCursor.collectDescendantText();
    detail.setStackTrace(stack);
  }

  private TestCaseDetails getTestCaseDetails(SMInputCursor testCaseCursor) throws XMLStreamException {
    TestCaseDetails detail = new TestCaseDetails();
    String name = getTestCaseName(testCaseCursor);
    detail.setName(name);

    String status = TestCaseDetails.STATUS_OK;
    Double time = getTimeAttributeInMS(testCaseCursor);

    SMInputCursor childNode = testCaseCursor.descendantElementCursor();
    if (childNode.getNext() != null) {
      String elementName = childNode.getLocalName();
      if (elementName.equals("skipped")) {
        status = TestCaseDetails.STATUS_SKIPPED;
        // bug with surefire reporting wrong time for skipped tests
        time = 0d;
      } else if (elementName.equals("failure")) {
        status = TestCaseDetails.STATUS_FAILURE;
        setStackAndMessage(detail, childNode);
      } else if (elementName.equals("error")) {
        status = TestCaseDetails.STATUS_ERROR;
        setStackAndMessage(detail, childNode);
      }
    }
    // make sure we loop till the end of the elements cursor
    while (childNode.getNext() != null) {
    }
    detail.setTimeMS(time.intValue());
    detail.setStatus(status);
    return detail;
  }

  private Double getTimeAttributeInMS(SMInputCursor testCaseCursor) throws XMLStreamException {
    // hardcoded to Locale.ENGLISH see http://jira.codehaus.org/browse/SONAR-602
    try {
      Double time = ParsingUtils.parseNumber(testCaseCursor.getAttrValue("time"), Locale.ENGLISH);
      return !Double.isNaN(time) ? ParsingUtils.scaleValue(time * 1000, 3) : 0;
    } catch (ParseException e) {
      throw new XMLStreamException(e);
    }
  }

  private String getTestCaseName(SMInputCursor testCaseCursor) throws XMLStreamException {
    String classname = testCaseCursor.getAttrValue("classname");
    String name = testCaseCursor.getAttrValue("name");
    if (StringUtils.contains(classname, "$")) {
      return StringUtils.substringAfterLast(classname, "$") + "/" + name;
    }
    return name;
  }

}
