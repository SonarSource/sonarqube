/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.utils.MessageException;

import static org.sonar.api.utils.Preconditions.checkState;

public class GenericCoverageReportParser {

  private static final String LINE_NUMBER_ATTR = "lineNumber";
  private static final String COVERED_ATTR = "covered";
  private static final String BRANCHES_TO_COVER_ATTR = "branchesToCover";
  private static final String COVERED_BRANCHES_ATTR = "coveredBranches";

  private static final int MAX_STORED_UNKNOWN_FILE_PATHS = 5;

  private int numberOfUnknownFiles;
  private final List<String> firstUnknownFiles = new ArrayList<>();
  private final Set<String> matchedFileKeys = new HashSet<>();

  public void parse(File reportFile, SensorContext context) {
    try (InputStream inputStream = new FileInputStream(reportFile)) {
      parse(inputStream, context);
    } catch (Exception e) {
      throw MessageException.of("Error during parsing of the generic coverage report '" + reportFile + "'. Look at SonarQube documentation to know the expected XML format.",
        e);
    }
  }

  private void parse(InputStream inputStream, SensorContext context) throws XMLStreamException {
    new StaxParser(rootCursor -> {
      rootCursor.advance();
      parseRootNode(rootCursor, context);
    }).parse(inputStream);
  }

  private void parseRootNode(SMHierarchicCursor rootCursor, SensorContext context) throws XMLStreamException {
    checkElementName(rootCursor, "coverage");
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
      if (inputFile == null) {
        numberOfUnknownFiles++;
        if (numberOfUnknownFiles <= MAX_STORED_UNKNOWN_FILE_PATHS) {
          firstUnknownFiles.add(filePath);
        }
        continue;
      }
      checkState(
        inputFile.language() != null,
        "Line %s of report refers to a file with an unknown language: %s",
        fileCursor.getCursorLocation().getLineNumber(),
        filePath);
      matchedFileKeys.add(inputFile.key());

      NewCoverage newCoverage = context.newCoverage().onFile(inputFile);
      SMInputCursor lineToCoverCursor = fileCursor.childElementCursor();
      while (lineToCoverCursor.getNext() != null) {
        parseLineToCover(lineToCoverCursor, newCoverage);
      }
      newCoverage.save();
    }
  }

  private static void parseLineToCover(SMInputCursor cursor, NewCoverage newCoverage)
    throws XMLStreamException {
    checkElementName(cursor, "lineToCover");
    String lineNumberAsString = mandatoryAttribute(cursor, LINE_NUMBER_ATTR);
    int lineNumber = intValue(lineNumberAsString, cursor, LINE_NUMBER_ATTR, 1);

    boolean covered = getCoveredValue(cursor);
    newCoverage.lineHits(lineNumber, covered ? 1 : 0);

    String branchesToCoverAsString = cursor.getAttrValue(BRANCHES_TO_COVER_ATTR);
    if (branchesToCoverAsString != null) {
      int branchesToCover = intValue(branchesToCoverAsString, cursor, BRANCHES_TO_COVER_ATTR, 0);
      String coveredBranchesAsString = cursor.getAttrValue(COVERED_BRANCHES_ATTR);
      int coveredBranches = 0;
      if (coveredBranchesAsString != null) {
        coveredBranches = intValue(coveredBranchesAsString, cursor, COVERED_BRANCHES_ATTR, 0);
        if (coveredBranches > branchesToCover) {
          throw new IllegalStateException("\"coveredBranches\" should not be greater than \"branchesToCover\" on line " + cursor.getCursorLocation().getLineNumber());
        }
      }
      newCoverage.conditions(lineNumber, branchesToCover, coveredBranches);
    }
  }

  private static boolean getCoveredValue(SMInputCursor cursor) throws XMLStreamException {
    String coveredAsString = mandatoryAttribute(cursor, COVERED_ATTR);
    if (!"true".equalsIgnoreCase(coveredAsString) && !"false".equalsIgnoreCase(coveredAsString)) {
      throw new IllegalStateException(expectedMessage("boolean value", COVERED_ATTR, coveredAsString, cursor.getCursorLocation().getLineNumber()));
    }
    return Boolean.parseBoolean(coveredAsString);
  }

  static void checkElementName(SMInputCursor cursor, String expectedName) throws XMLStreamException {
    String elementName = cursor.getLocalName();
    if (!expectedName.equals(elementName)) {
      throw new IllegalStateException("Unknown XML node, expected \"" + expectedName + "\" but got \"" + elementName + "\" at line " + cursor.getCursorLocation().getLineNumber());
    }
  }

  static String mandatoryAttribute(SMInputCursor cursor, String attributeName) throws XMLStreamException {
    String attributeValue = cursor.getAttrValue(attributeName);
    if (attributeValue == null) {
      throw new IllegalStateException(
        "Missing attribute \"" + attributeName + "\" in element \"" + cursor.getLocalName() + "\" at line " + cursor.getCursorLocation().getLineNumber());
    }
    return attributeValue;
  }

  static int intValue(String stringValue, SMInputCursor cursor, String attributeName, int minimum) throws XMLStreamException {
    int intValue;
    try {
      intValue = Integer.valueOf(stringValue);
    } catch (NumberFormatException e) {
      throw new IllegalStateException(expectedMessage("integer value", attributeName, stringValue, cursor.getCursorLocation().getLineNumber()), e);
    }
    if (intValue < minimum) {
      throw new IllegalStateException("Value of attribute \"" + attributeName + "\" at line " + cursor.getCursorLocation().getLineNumber() + " is \"" + intValue
        + "\" but it should be greater than or equal to " + minimum);
    }
    return intValue;
  }

  static long longValue(String stringValue, SMInputCursor cursor, String attributeName, long minimum) throws XMLStreamException {
    long longValue;
    try {
      longValue = Long.valueOf(stringValue);
    } catch (NumberFormatException e) {
      throw new IllegalStateException(expectedMessage("long value", attributeName, stringValue, cursor.getCursorLocation().getLineNumber()), e);
    }
    if (longValue < minimum) {
      throw new IllegalStateException("Value of attribute \"" + attributeName + "\" at line " + cursor.getCursorLocation().getLineNumber() + " is \"" + longValue
        + "\" but it should be greater than or equal to " + minimum);
    }
    return longValue;
  }

  private static String expectedMessage(String expected, String attributeName, String stringValue, int line) {
    return "Expected " + expected + " for attribute \"" + attributeName + "\" at line " + line + " but got \"" + stringValue + "\"";
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
