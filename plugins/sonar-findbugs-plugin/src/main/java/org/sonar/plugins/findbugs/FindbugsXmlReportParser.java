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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.XmlParserException;

import javax.annotation.CheckForNull;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.util.List;

class FindbugsXmlReportParser {

  private final File findbugsXmlReport;
  private final String findbugsXmlReportPath;

  public FindbugsXmlReportParser(File findbugsXmlReport) {
    this.findbugsXmlReport = findbugsXmlReport;
    findbugsXmlReportPath = findbugsXmlReport.getAbsolutePath();
    if (!findbugsXmlReport.exists()) {
      throw new SonarException("The findbugs XML report can't be found at '" + findbugsXmlReportPath + "'");
    }
  }

  public List<XmlBugInstance> getBugInstances() {
    List<XmlBugInstance> result = Lists.newArrayList();
    try {
      SMInputFactory inf = new SMInputFactory(XMLInputFactory.newInstance());
      SMInputCursor cursor = inf.rootElementCursor(findbugsXmlReport).advance();
      SMInputCursor bugInstanceCursor = cursor.childElementCursor("BugInstance").advance();
      while (bugInstanceCursor.asEvent() != null) {
        XmlBugInstance xmlBugInstance = new XmlBugInstance();
        xmlBugInstance.type = bugInstanceCursor.getAttrValue("type");
        xmlBugInstance.longMessage = "";
        result.add(xmlBugInstance);

        ImmutableList.Builder<XmlSourceLineAnnotation> lines = ImmutableList.builder();
        SMInputCursor bugInstanceChildCursor = bugInstanceCursor.childElementCursor().advance();
        while (bugInstanceChildCursor.asEvent() != null) {
          String nodeName = bugInstanceChildCursor.getLocalName();
          if ("LongMessage".equals(nodeName)) {
            xmlBugInstance.longMessage = bugInstanceChildCursor.collectDescendantText();
          } else if ("SourceLine".equals(nodeName)) {
            XmlSourceLineAnnotation xmlSourceLineAnnotation = new XmlSourceLineAnnotation();
            xmlSourceLineAnnotation.parseStart(bugInstanceChildCursor.getAttrValue("start"));
            xmlSourceLineAnnotation.parseEnd(bugInstanceChildCursor.getAttrValue("end"));
            xmlSourceLineAnnotation.parsePrimary(bugInstanceChildCursor.getAttrValue("primary"));
            xmlSourceLineAnnotation.className = bugInstanceChildCursor.getAttrValue("classname");
            lines.add(xmlSourceLineAnnotation);
          }
          bugInstanceChildCursor.advance();
        }
        xmlBugInstance.sourceLines = lines.build();
        bugInstanceCursor.advance();
      }
      cursor.getStreamReader().closeCompletely();
    } catch (XMLStreamException e) {
      throw new XmlParserException("Unable to parse the Findbugs XML Report '" + findbugsXmlReportPath + "'", e);
    }
    return result;
  }

  public static class XmlBugInstance {
    private String type;
    private String longMessage;
    private List<XmlSourceLineAnnotation> sourceLines;

    public String getType() {
      return type;
    }

    public String getLongMessage() {
      return longMessage;
    }

    @CheckForNull
    public XmlSourceLineAnnotation getPrimarySourceLine() {
      for (XmlSourceLineAnnotation sourceLine : sourceLines) {
        if (sourceLine.isPrimary()) {
          // According to source code of Findbugs 2.0 - should be exactly one primary
          return sourceLine;
        }
      }
      // As a last resort - return first line
      return sourceLines.isEmpty() ? null : sourceLines.get(0);
    }

  }

  public static class XmlSourceLineAnnotation {
    private boolean primary;
    private Integer start;
    private Integer end;
    @VisibleForTesting
    protected String className;

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

    public void parsePrimary(String attrValue) {
      primary = Boolean.parseBoolean(attrValue);
    }

    public boolean isPrimary() {
      return primary;
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

    public String getSonarJavaFileKey() {
      if (className.indexOf('$') > -1) {
        return className.substring(0, className.indexOf('$'));
      }
      return className;
    }

  }

}
