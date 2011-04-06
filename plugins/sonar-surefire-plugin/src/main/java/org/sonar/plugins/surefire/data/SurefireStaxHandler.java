/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.surefire.data;

import java.text.ParseException;
import java.util.Locale;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.in.ElementFilter;
import org.codehaus.staxmate.in.SMEvent;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.utils.ParsingUtils;
import org.sonar.api.utils.StaxParser.XmlStreamHandler;

public class SurefireStaxHandler implements XmlStreamHandler {

  private UnitTestIndex index;

  public SurefireStaxHandler(UnitTestIndex index) {
    this.index = index;
  }

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
            UnitTestClassReport classReport = index.index(testClassName);
            parseTestCase(testCase, classReport);
          }
        }
      }
    }
  }

  private String getClassname(SMInputCursor testCaseCursor, String defaultClassname) throws XMLStreamException {
    String testClassName = testCaseCursor.getAttrValue("classname");
    return StringUtils.defaultIfBlank(testClassName, defaultClassname);
  }

  private void parseTestCase(SMInputCursor testCaseCursor, UnitTestClassReport report) throws XMLStreamException {
    report.add(parseTestResult(testCaseCursor));
  }

  private void setStackAndMessage(UnitTestResult result, SMInputCursor stackAndMessageCursor) throws XMLStreamException {
    result.setMessage(stackAndMessageCursor.getAttrValue("message"));
    String stack = stackAndMessageCursor.collectDescendantText();
    result.setStackTrace(stack);
  }

  private UnitTestResult parseTestResult(SMInputCursor testCaseCursor) throws XMLStreamException {
    UnitTestResult detail = new UnitTestResult();
    String name = getTestCaseName(testCaseCursor);
    detail.setName(name);

    String status = UnitTestResult.STATUS_OK;
    long duration = getTimeAttributeInMS(testCaseCursor);

    SMInputCursor childNode = testCaseCursor.descendantElementCursor();
    if (childNode.getNext() != null) {
      String elementName = childNode.getLocalName();
      if ("skipped".equals(elementName)) {
        status = UnitTestResult.STATUS_SKIPPED;
        // bug with surefire reporting wrong time for skipped tests
        duration = 0L;

      } else if ("failure".equals(elementName)) {
        status = UnitTestResult.STATUS_FAILURE;
        setStackAndMessage(detail, childNode);

      } else if ("error".equals(elementName)) {
        status = UnitTestResult.STATUS_ERROR;
        setStackAndMessage(detail, childNode);
      }
    }
    while (childNode.getNext() != null) {
      // make sure we loop till the end of the elements cursor
    }
    detail.setDurationMilliseconds(duration);
    detail.setStatus(status);
    return detail;
  }

  private long getTimeAttributeInMS(SMInputCursor testCaseCursor) throws XMLStreamException {
    // hardcoded to Locale.ENGLISH see http://jira.codehaus.org/browse/SONAR-602
    try {
      Double time = ParsingUtils.parseNumber(testCaseCursor.getAttrValue("time"), Locale.ENGLISH);
      return !Double.isNaN(time) ? new Double(ParsingUtils.scaleValue(time * 1000, 3)).longValue() : 0L;
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
