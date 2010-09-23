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
package org.sonar.api.batch;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.ParseException;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.IOUtils;
import org.codehaus.staxmate.in.SMEvent;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulesManager;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.ParsingUtils;
import org.sonar.api.utils.StaxParser;

/**
 * @since 1.10
 * @deprecated since 2.3. Too much "black-box".
 */
@Deprecated
public abstract class AbstractViolationsStaxParser {

  protected RulesManager rulesManager;
  protected SensorContext context;
  protected boolean doSaveViolationsOnUnexistedResource = true;

  /**
   * @deprecated since 1.11.
   */
  @Deprecated
  protected AbstractViolationsStaxParser(SensorContext context, RulesManager rulesManager, RulesProfile profile) {
    this.rulesManager = rulesManager;
    this.context = context;
  }

  protected AbstractViolationsStaxParser(SensorContext context, RulesManager rulesManager) {
    this.rulesManager = rulesManager;
    this.context = context;
  }

  /**
   * Cursor for child resources to parse, the returned input cursor should be filtered on SMEvent.START_ELEMENT for optimal perfs
   * 
   * @param rootCursor
   *          the root xml doc cursor
   * @return a cursor with child resources elements to parse
   */
  protected abstract SMInputCursor cursorForResources(SMInputCursor rootCursor) throws XMLStreamException;

  /**
   * Cursor for violations to parse for a given resource, the returned input cursor should be filtered on SMEvent.START_ELEMENT for optimal
   * perfs
   * 
   * @param resourcesCursor
   *          the current resource cursor
   * @return a cursor with child violations elements to parse
   */
  protected abstract SMInputCursor cursorForViolations(SMInputCursor resourcesCursor) throws XMLStreamException;

  /**
   * Transforms a given xml resource to a resource Object
   */
  protected abstract Resource toResource(SMInputCursor resourceCursor) throws XMLStreamException;

  protected abstract String messageFor(SMInputCursor violationCursor) throws XMLStreamException;

  protected abstract String ruleKey(SMInputCursor violationCursor) throws XMLStreamException;

  protected abstract String keyForPlugin();

  protected abstract String lineNumberForViolation(SMInputCursor violationCursor) throws XMLStreamException;

  /**
   * Specify if violations must be saved even if when the Resource associated to a violation doesn't yet exist.
   * In that case the Resource is automatically created.
   * 
   * @param doSaveViolationsOnUnexistedResource by default, the value is true
   */
  public final void setDoSaveViolationsOnUnexistedResource(boolean doSaveViolationsOnUnexistedResource) {
    this.doSaveViolationsOnUnexistedResource = doSaveViolationsOnUnexistedResource;
  }

  public void parse(File violationsXMLFile) throws XMLStreamException {
    if (violationsXMLFile != null && violationsXMLFile.exists()) {
      InputStream input = null;
      try {
        input = new FileInputStream(violationsXMLFile);
        parse(input);

      } catch (FileNotFoundException e) {
        throw new XMLStreamException(e);

      } finally {
        IOUtils.closeQuietly(input);
      }
    }
  }

  public final void parse(InputStream input) throws XMLStreamException {
    if (input != null) {
      StaxParser parser = new StaxParser(new StaxParser.XmlStreamHandler() {

        public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
          parseResources(rootCursor.advance());
        }
      }, true);
      parser.parse(input);
    }
  }

  private void parseResources(SMInputCursor rootCursor) throws XMLStreamException {
    SMInputCursor resourcesCursor = cursorForResources(rootCursor);
    SMEvent event;
    while ((event = resourcesCursor.getNext()) != null) {
      if (event.compareTo(SMEvent.START_ELEMENT) == 0) {
        parseViolations(resourcesCursor);
      }
    }
  }

  private void parseViolations(SMInputCursor resourcesCursor) throws XMLStreamException {
    Resource resource = toResource(resourcesCursor);
    if ( !doSaveViolationsOnUnexistedResource && context.getResource(resource) == null) {
      return;
    }
    SMInputCursor violationsCursor = cursorForViolations(resourcesCursor);
    SMEvent event;
    while ((event = violationsCursor.getNext()) != null) {
      if (event.compareTo(SMEvent.START_ELEMENT) == 0) {
        createViolationFor(resource, violationsCursor);
      }
    }
  }

  private void createViolationFor(Resource resource, SMInputCursor violationCursor) throws XMLStreamException {
    Rule rule = getRule(violationCursor);
    Integer line = getLineIndex(violationCursor);
    if (rule != null && resource != null) {
      Violation violation = Violation.create(rule, resource)
          .setLineId(line)
          .setMessage(messageFor(violationCursor));
      context.saveViolation(violation);
    }
  }

  private Rule getRule(SMInputCursor violationCursor) throws XMLStreamException {
    return rulesManager.getPluginRule(keyForPlugin(), ruleKey(violationCursor));
  }

  private Integer getLineIndex(SMInputCursor violationCursor) throws XMLStreamException {
    String line = lineNumberForViolation(violationCursor);
    return parseLineIndex(line);
  }

  protected static Integer parseLineIndex(String line) {
    if ( !isNotBlank(line) || line.indexOf('-') != -1) {
      return null;
    }
    try {
      return (int) ParsingUtils.parseNumber(line);
    } catch (ParseException ignore) {
      return null;
    }
  }

}
