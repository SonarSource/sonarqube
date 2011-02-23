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
package org.sonar.plugins.pmd;

import java.io.File;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.StaxParser;

class PmdViolationsXmlParser {

  private Project project;
  private RuleFinder ruleFinder;
  private SensorContext context;

  public PmdViolationsXmlParser(Project project, RuleFinder ruleFinder, SensorContext context) {
    this.project = project;
    this.ruleFinder = ruleFinder;
    this.context = context;
  }

  public void parse(File file) throws XMLStreamException {
    StaxParser parser = new StaxParser(new StreamHandler(), true);
    parser.parse(file);
  }

  private class StreamHandler implements StaxParser.XmlStreamHandler {
    public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
      rootCursor.advance();

      SMInputCursor fileCursor = rootCursor.descendantElementCursor("file");
      while (fileCursor.getNext() != null) {
        String name = fileCursor.getAttrValue("name");
        Resource resource = JavaFile.fromAbsolutePath(name, project.getFileSystem().getSourceDirs(), false);

        // Save violations only for existing resources
        if (context.getResource(resource) != null) {
          streamViolations(fileCursor, resource);
        }
      }
    }

    private void streamViolations(SMInputCursor fileCursor, Resource resource) throws XMLStreamException {
      SMInputCursor violationCursor = fileCursor.descendantElementCursor("violation");
      while (violationCursor.getNext() != null) {
        int lineId = Integer.parseInt(violationCursor.getAttrValue("beginline"));
        String ruleKey = violationCursor.getAttrValue("rule");
        String message = StringUtils.trim(violationCursor.collectDescendantText());

        Rule rule = ruleFinder.findByKey(CoreProperties.PMD_PLUGIN, ruleKey);
        // Save violations only for enabled rules
        if (rule != null) {
          Violation violation = Violation.create(rule, resource).setLineId(lineId).setMessage(message);
          context.saveViolation(violation);
        }
      }
    }
  }

}
