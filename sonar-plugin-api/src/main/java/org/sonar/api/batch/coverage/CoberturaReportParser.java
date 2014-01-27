/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.batch.coverage;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.jfree.util.Log;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoverageMeasuresBuilder;
import org.sonar.api.measures.Measure;
import org.sonar.api.scan.filesystem.InputFile;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.utils.StaxParser;
import org.sonar.api.utils.XmlParserException;

import javax.annotation.CheckForNull;
import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Locale.ENGLISH;
import static org.sonar.api.utils.ParsingUtils.parseNumber;

/**
 * Parse a provided cobertura report and create appropriate measures.
 * @since 4.2
 */
public class CoberturaReportParser implements BatchComponent {

  private final ModuleFileSystem fs;

  public CoberturaReportParser(ModuleFileSystem fs) {
    this.fs = fs;
  }

  /**
   * Parse a Cobertura xml report and create measures accordingly
   */
  public void parseReport(File xmlFile, final SensorContext context) {
    try {
      StaxParser parser = new StaxParser(new StaxParser.XmlStreamHandler() {

        public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
          rootCursor.advance();
          SMInputCursor rootChildCursor = rootCursor.childElementCursor();

          List<File> sourceDirs = new ArrayList<File>();

          while (rootChildCursor.getNext() != null) {
            handleRootChildElement(sourceDirs, context, rootChildCursor);
          }
        }

      });
      parser.parse(xmlFile);
    } catch (XMLStreamException e) {
      throw new XmlParserException("Fail to parse " + xmlFile.getAbsolutePath(), e);
    }
  }

  private void handleRootChildElement(List<File> sourceDirs, SensorContext context, SMInputCursor rootChildCursor) throws XMLStreamException {
    if ("sources".equals(rootChildCursor.getLocalName())) {
      collectSourceFolders(rootChildCursor.childElementCursor(), sourceDirs);
    } else if ("packages".equals(rootChildCursor.getLocalName())) {
      collectPackageMeasures(rootChildCursor.childElementCursor(), context, sourceDirs);
    }
  }

  private void collectSourceFolders(SMInputCursor source, List<File> sourceDirs) throws XMLStreamException {
    while (source.getNext() != null) {
      sourceDirs.add(new File(source.collectDescendantText()));
    }
  }

  private void collectPackageMeasures(SMInputCursor pack, SensorContext context, List<File> sourceDirs) throws XMLStreamException {
    while (pack.getNext() != null) {
      Map<String, CoverageMeasuresBuilder> builderByFilename = Maps.newHashMap();
      collectFileMeasures(pack.descendantElementCursor("class"), builderByFilename);
      for (Map.Entry<String, CoverageMeasuresBuilder> entry : builderByFilename.entrySet()) {
        String filename = entry.getKey();

        InputFile inputfile = findInputFile(filename, sourceDirs);
        if (inputfile != null) {
          for (Measure measure : entry.getValue().createMeasures()) {
            context.saveMeasure(inputfile, measure);
          }
        }
      }
    }
  }

  @CheckForNull
  private InputFile findInputFile(String filename, List<File> sourceDirs) {
    for (File srcDir : sourceDirs) {
      File possibleFile = new File(srcDir, filename);
      InputFile inputFile = fs.inputFile(possibleFile);
      if (inputFile != null) {
        return inputFile;
      }
    }
    Log.debug("Filename " + filename + " was not found as an InputFile is any of the source folders: " + Joiner.on(", ").join(sourceDirs));
    return null;
  }

  private static void collectFileMeasures(SMInputCursor clazz, Map<String, CoverageMeasuresBuilder> builderByFilename) throws XMLStreamException {
    while (clazz.getNext() != null) {
      String fileName = clazz.getAttrValue("filename");
      CoverageMeasuresBuilder builder = builderByFilename.get(fileName);
      if (builder == null) {
        builder = CoverageMeasuresBuilder.create();
        builderByFilename.put(fileName, builder);
      }
      collectFileData(clazz, builder);
    }
  }

  private static void collectFileData(SMInputCursor clazz, CoverageMeasuresBuilder builder) throws XMLStreamException {
    SMInputCursor line = clazz.childElementCursor("lines").advance().childElementCursor("line");
    while (line.getNext() != null) {
      int lineId = Integer.parseInt(line.getAttrValue("number"));
      try {
        builder.setHits(lineId, (int) parseNumber(line.getAttrValue("hits"), ENGLISH));
      } catch (ParseException e) {
        throw new XmlParserException(e);
      }

      String isBranch = line.getAttrValue("branch");
      String text = line.getAttrValue("condition-coverage");
      if (StringUtils.equals(isBranch, "true") && StringUtils.isNotBlank(text)) {
        String[] conditions = StringUtils.split(StringUtils.substringBetween(text, "(", ")"), "/");
        builder.setConditions(lineId, Integer.parseInt(conditions[1]), Integer.parseInt(conditions[0]));
      }
    }
  }

}
