/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.plugins.findbugs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.XmlParserException;

class FindbugsXmlReportParser {

  private final File findbugsXmlReport;
  private final String findbugsXmlReportPath;

  public FindbugsXmlReportParser(File findbugsXmlReport) {
    this.findbugsXmlReport = findbugsXmlReport;
    findbugsXmlReportPath = findbugsXmlReport.getAbsolutePath();
    if ( !findbugsXmlReport.exists()) {
      throw new SonarException("The findbugs XML report can't be found at '" + findbugsXmlReportPath + "'");
    }
  }

  public List<Violation> getViolations() {
    List<Violation> violations = new ArrayList<Violation>();
    try {
      SMInputFactory inf = new SMInputFactory(XMLInputFactory.newInstance());
      SMInputCursor cursor = inf.rootElementCursor(findbugsXmlReport).advance();
      SMInputCursor bugInstanceCursor = cursor.childElementCursor("BugInstance").advance();
      while (bugInstanceCursor.asEvent() != null) {
        String type = bugInstanceCursor.getAttrValue("type");
        String longMessage = "";
        SMInputCursor bugInstanceChildCursor = bugInstanceCursor.childElementCursor().advance();
        while (bugInstanceChildCursor.asEvent() != null) {
          String nodeName = bugInstanceChildCursor.getLocalName();
          if ("LongMessage".equals(nodeName)) {
            longMessage = bugInstanceChildCursor.collectDescendantText();
          } else if ("SourceLine".equals(nodeName)) {
            Violation fbViolation = new Violation();
            fbViolation.type = type;
            fbViolation.longMessage = longMessage;
            fbViolation.parseStart(bugInstanceChildCursor.getAttrValue("start"));
            fbViolation.parseEnd(bugInstanceChildCursor.getAttrValue("end"));
            fbViolation.className = bugInstanceChildCursor.getAttrValue("classname");
            fbViolation.sourcePath = bugInstanceChildCursor.getAttrValue("sourcepath");
            violations.add(fbViolation);
          }
          bugInstanceChildCursor.advance();
        }
        bugInstanceCursor.advance();
      }
      cursor.getStreamReader().closeCompletely();
    } catch (XMLStreamException e) {
      throw new XmlParserException("Unable to parse the Findbugs XML Report '" + findbugsXmlReportPath + "'", e);
    }
    return violations;
  }

  public static class Violation {

    private String type;
    private String longMessage;
    private Integer start;
    private Integer end;
    protected String className;
    protected String sourcePath;

    public String getType() {
      return type;
    }

    public void parseStart(String attrValue) {
      try {
        start = Integer.parseInt(attrValue);
      } catch (NumberFormatException e) {
        start = null;
      }
    }

    public void parseEnd(String attrValue) {
      try {
        end = Integer.parseInt(attrValue);
      } catch (NumberFormatException e) {
        end = null;
      }
    }

    public String getLongMessage() {
      return longMessage;
    }

    public Integer getStart() {
      return start;
    }

    public Integer getEnd() {
      return end;
    }

    public String getClassName() {
      return className;
    }

    public String getSourcePath() {
      return sourcePath;
    }

    public String getSonarJavaFileKey() {
      if (className.indexOf('$') > -1) {
        return className.substring(0, className.indexOf('$'));
      }
      return className;
    }
  }

}
