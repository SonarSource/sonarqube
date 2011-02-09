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
package org.sonar.plugins.clover;

import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.in.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.PropertiesBuilder;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.ParsingUtils;
import static org.sonar.api.utils.ParsingUtils.scaleValue;
import org.sonar.api.utils.StaxParser;
import org.sonar.api.utils.XmlParserException;

import java.io.File;
import java.text.ParseException;
import javax.xml.stream.XMLStreamException;

public class XmlReportParser {

  private static final Logger LOG = LoggerFactory.getLogger(XmlReportParser.class);
  private SensorContext context;
  final PropertiesBuilder<String, Integer> lineHitsBuilder = new PropertiesBuilder<String, Integer>(CoreMetrics.COVERAGE_LINE_HITS_DATA);
  final PropertiesBuilder<String, String> branchHitsBuilder = new PropertiesBuilder<String, String>(CoreMetrics.BRANCH_COVERAGE_HITS_DATA);

  public XmlReportParser(SensorContext context) {
    this.context = context;
  }

  private boolean reportExists(File report) {
    return report != null && report.exists() && report.isFile();
  }

  protected void collect(File xmlFile) {
    try {
      if (reportExists(xmlFile)) {
        LOG.info("Parsing " + xmlFile.getCanonicalPath());
        StaxParser parser = new StaxParser(new StaxParser.XmlStreamHandler() {
          public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
            try {
              collectProjectMeasures(rootCursor.advance());
            } catch (ParseException e) {
              throw new XMLStreamException(e);
            }
          }
        });
        parser.parse(xmlFile);
      }
    } catch (Exception e) {
      throw new XmlParserException(e);
    }
  }

  private void collectProjectMeasures(SMInputCursor rootCursor) throws ParseException, XMLStreamException {
    SMInputCursor projectCursor = rootCursor.descendantElementCursor("project");
    SMInputCursor projectChildrenCursor = projectCursor.advance().childElementCursor();
    projectChildrenCursor.setFilter(new SimpleFilter(SMEvent.START_ELEMENT));

    SMInputCursor metricsCursor = projectChildrenCursor.advance();
    analyseMetricsNode(null, metricsCursor);
    collectPackageMeasures(projectChildrenCursor);
  }

  private void analyseMetricsNode(Resource resource, SMInputCursor metricsCursor) throws ParseException, XMLStreamException {
    int elements = (int) ParsingUtils.parseNumber(metricsCursor.getAttrValue("elements"));
    if (elements == 0) {
      return;
    }

    int statements = (int) ParsingUtils.parseNumber(metricsCursor.getAttrValue("statements"));
    int methods = (int) ParsingUtils.parseNumber(metricsCursor.getAttrValue("methods"));
    int conditionals = (int) ParsingUtils.parseNumber(metricsCursor.getAttrValue("conditionals"));
    int coveredElements = (int) ParsingUtils.parseNumber(metricsCursor.getAttrValue("coveredelements"));
    int coveredStatements = (int) ParsingUtils.parseNumber(metricsCursor.getAttrValue("coveredstatements"));
    int coveredMethods = (int) ParsingUtils.parseNumber(metricsCursor.getAttrValue("coveredmethods"));
    int coveredConditionals = (int) ParsingUtils.parseNumber(metricsCursor.getAttrValue("coveredconditionals"));

    context.saveMeasure(resource, CoreMetrics.COVERAGE, calculateCoverage(coveredElements, elements));

    context.saveMeasure(resource, CoreMetrics.LINE_COVERAGE, calculateCoverage(coveredMethods + coveredStatements, methods + statements));
    context.saveMeasure(resource, CoreMetrics.LINES_TO_COVER, (double) (statements + methods));
    context.saveMeasure(resource, CoreMetrics.UNCOVERED_LINES, (double) (statements + methods - coveredStatements - coveredMethods));

    if (conditionals > 0) {
      context.saveMeasure(resource, CoreMetrics.BRANCH_COVERAGE, calculateCoverage(coveredConditionals, conditionals));
      context.saveMeasure(resource, CoreMetrics.CONDITIONS_TO_COVER, (double) (conditionals));
      context.saveMeasure(resource, CoreMetrics.UNCOVERED_CONDITIONS, (double) (conditionals - coveredConditionals));
    }
  }

  private double calculateCoverage(int coveredElements, int elements) {
    if (elements > 0) {
      return scaleValue(100.0 * ((double) coveredElements / (double) elements));
    }
    return 0.0;
  }

  private void collectPackageMeasures(SMInputCursor packCursor) throws ParseException, XMLStreamException {
    while (packCursor.getNext() != null) {
      JavaPackage pack = new JavaPackage(packCursor.getAttrValue("name"));
      SMInputCursor packChildrenCursor = packCursor.descendantElementCursor();
      packChildrenCursor.setFilter(new SimpleFilter(SMEvent.START_ELEMENT));
      SMInputCursor metricsCursor = packChildrenCursor.advance();
      analyseMetricsNode(pack, metricsCursor);
      collectFileMeasures(packChildrenCursor, pack);
    }
  }

  private void collectFileMeasures(SMInputCursor fileCursor, JavaPackage pack) throws ParseException, XMLStreamException {
    fileCursor.setFilter(SMFilterFactory.getElementOnlyFilter("file"));
    while (fileCursor.getNext() != null) {
      if (fileCursor.asEvent().isStartElement()) {
        String classKey = extractClassName(fileCursor.getAttrValue("name"));
        if (classKey != null) {
          SMInputCursor fileChildrenCursor = fileCursor.childCursor(new SimpleFilter(SMEvent.START_ELEMENT));
          // cursor should be on the metrics element
          if (canBeIncludedInFileMetrics(fileChildrenCursor)) {
            JavaFile resource = new JavaFile(pack.getKey(), classKey, false);
            analyseMetricsNode(resource, fileChildrenCursor);

            // cursor should be now on the line cursor
            saveHitsData(resource, fileChildrenCursor);
          }
        }
      }
    }
  }

  private void saveHitsData(Resource resource, SMInputCursor lineCursor) throws ParseException, XMLStreamException {
    lineHitsBuilder.clear();
    branchHitsBuilder.clear();
    boolean hasBranches = false;

    while (lineCursor.getNext() != null) {
      // skip class elements on format 2_3_2
      if (lineCursor.getLocalName().equals("class")) {
        continue;
      }
      final String lineId = lineCursor.getAttrValue("num");
      int hits;
      String count = lineCursor.getAttrValue("count");
      if (StringUtils.isBlank(count)) {
        int trueCount = (int) ParsingUtils.parseNumber(lineCursor.getAttrValue("truecount"));
        int falseCount = (int) ParsingUtils.parseNumber(lineCursor.getAttrValue("falsecount"));
        hits = trueCount + falseCount;
        String branchHits;
        if (trueCount > 0 && falseCount > 0) {
          branchHits = "100%";
        } else if (trueCount == 0 && falseCount == 0) {
          branchHits = "0%";
        } else {
          branchHits = "50%";
        }
        branchHitsBuilder.add(lineId, branchHits);
        hasBranches = true;

      } else {
        hits = (int) ParsingUtils.parseNumber(count);
      }
      lineHitsBuilder.add(lineId, hits);
    }
    context.saveMeasure(resource, lineHitsBuilder.build());
    if (hasBranches) {
      context.saveMeasure(resource, branchHitsBuilder.build());
    }
  }

  private boolean canBeIncludedInFileMetrics(SMInputCursor metricsCursor) throws ParseException, XMLStreamException {
    while (metricsCursor.getNext() != null && metricsCursor.getLocalName().equals("class")) {
      // skip class elements on 1.x xml format
    }
    return ParsingUtils.parseNumber(metricsCursor.getAttrValue("elements")) > 0;
  }

  protected String extractClassName(String filename) {
    if (filename != null) {
      filename = StringUtils.replaceChars(filename, '\\', '/');
      filename = StringUtils.substringBeforeLast(filename, ".java");
      if (filename.indexOf('/') >= 0) {
        filename = StringUtils.substringAfterLast(filename, "/");
      }
    }
    return filename;
  }
}