/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.genericcoverage;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.MessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.scanner.deprecated.test.DefaultTestCase;
import org.sonar.scanner.deprecated.test.DefaultTestCase.Status;
import org.sonar.scanner.deprecated.test.DefaultTestPlan;
import org.sonar.scanner.deprecated.test.TestPlanBuilder;

import static org.sonar.api.utils.Preconditions.checkState;
import static org.sonar.scanner.genericcoverage.GenericCoverageReportParser.checkElementName;
import static org.sonar.scanner.genericcoverage.GenericCoverageReportParser.longValue;
import static org.sonar.scanner.genericcoverage.GenericCoverageReportParser.mandatoryAttribute;

public class GenericTestExecutionReportParser {

  private static final String ROOT_ELEMENT = "testExecutions";

  private static final String OLD_ROOT_ELEMENT = "unitTest";

  private static final Logger LOG = LoggerFactory.getLogger(GenericTestExecutionReportParser.class);

  private static final String NAME_ATTR = "name";
  private static final String DURATION_ATTR = "duration";

  public static final String ERROR = "error";
  public static final String FAILURE = "failure";
  public static final String SKIPPED = "skipped";

  private static final int MAX_STORED_UNKNOWN_FILE_PATHS = 5;

  private final TestPlanBuilder testPlanBuilder;

  private int numberOfUnknownFiles;
  private final List<String> firstUnknownFiles = new ArrayList<>();
  private final Set<String> matchedFileKeys = new HashSet<>();

  public GenericTestExecutionReportParser(TestPlanBuilder testPlanBuilder) {
    this.testPlanBuilder = testPlanBuilder;
  }

  public void parse(File reportFile, SensorContext context) {
    try (InputStream inputStream = new FileInputStream(reportFile)) {
      parse(inputStream, context);
    } catch (Exception e) {
      throw MessageException.of(
        "Error during parsing of generic test execution report '" + reportFile + "'. Look at the SonarQube documentation to know the expected XML format.", e);
    }
  }

  private void parse(InputStream inputStream, SensorContext context) throws XMLStreamException {
    new StaxParser(rootCursor -> {
      rootCursor.advance();
      parseRootNode(rootCursor, context);
    }).parse(inputStream);
  }

  private void parseRootNode(SMHierarchicCursor rootCursor, SensorContext context) throws XMLStreamException {
    String elementName = rootCursor.getLocalName();
    if (!OLD_ROOT_ELEMENT.equals(elementName) && !ROOT_ELEMENT.equals(elementName)) {
      throw new IllegalStateException(
        "Unknown XML node, expected \"" + ROOT_ELEMENT + "\" but got \"" + elementName + "\" at line " + rootCursor.getCursorLocation().getLineNumber());
    }
    if (OLD_ROOT_ELEMENT.equals(elementName)) {
      LOG.warn("Using '" + OLD_ROOT_ELEMENT + "' as root element of the report is deprecated. Please change to '" + ROOT_ELEMENT + "'.");
    }
    String version = rootCursor.getAttrValue("version");
    if (!"1".equals(version)) {
      throw new IllegalStateException("Unknown report version: " + version + ". This parser only handles version 1.");
    }
    parseFiles(rootCursor.childElementCursor(), context);
  }

  private void parseFiles(SMInputCursor fileCursor, SensorContext context) throws XMLStreamException {
    while (fileCursor.getNext() != null) {
      checkElementName(fileCursor, "file");
      String filePath = mandatoryAttribute(fileCursor, "path");
      InputFile inputFile = context.fileSystem().inputFile(context.fileSystem().predicates().hasPath(filePath));
      if (inputFile == null || inputFile.language() == null) {
        numberOfUnknownFiles++;
        if (numberOfUnknownFiles <= MAX_STORED_UNKNOWN_FILE_PATHS) {
          firstUnknownFiles.add(filePath);
        }
        if (inputFile != null) {
          LOG.debug("Skipping file '{}' in the generic test execution report because it doesn't have a known language", filePath);
        }
        continue;
      }
      checkState(
        inputFile.type() != InputFile.Type.MAIN,
        "Line %s of report refers to a file which is not configured as a test file: %s",
        fileCursor.getCursorLocation().getLineNumber(),
        filePath);
      matchedFileKeys.add(inputFile.absolutePath());

      DefaultTestPlan testPlan = testPlanBuilder.getTestPlan(inputFile);
      SMInputCursor testCaseCursor = fileCursor.childElementCursor();
      while (testCaseCursor.getNext() != null) {
        parseTestCase(testCaseCursor, testPlan);
      }
    }
  }

  private static void parseTestCase(SMInputCursor cursor, DefaultTestPlan testPlan) throws XMLStreamException {
    checkElementName(cursor, "testCase");
    DefaultTestCase testCase = testPlan.addTestCase(mandatoryAttribute(cursor, NAME_ATTR));
    Status status = Status.OK;
    testCase.setDurationInMs(longValue(mandatoryAttribute(cursor, DURATION_ATTR), cursor, DURATION_ATTR, 0));

    SMInputCursor child = cursor.descendantElementCursor();
    if (child.getNext() != null) {
      String elementName = child.getLocalName();
      if (SKIPPED.equals(elementName)) {
        status = Status.SKIPPED;
      } else if (FAILURE.equals(elementName)) {
        status = Status.FAILURE;
      } else if (ERROR.equals(elementName)) {
        status = Status.ERROR;
      }
    }
    testCase.setStatus(status);

  }

  public int numberOfMatchedFiles() {
    return matchedFileKeys.size();
  }

  public int numberOfUnknownFiles() {
    return numberOfUnknownFiles;
  }

  public List<String> firstUnknownFiles() {
    return firstUnknownFiles;
  }

}
