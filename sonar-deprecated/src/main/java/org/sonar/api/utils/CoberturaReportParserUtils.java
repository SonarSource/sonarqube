/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.api.utils;

import com.google.common.collect.Maps;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoverageMeasuresBuilder;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;

import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.text.ParseException;
import java.util.Map;

import static java.util.Locale.ENGLISH;
import static org.sonar.api.utils.ParsingUtils.parseNumber;

/**
 * @since 3.7
 * @deprecated in 4.2. This class should be handled internally by plugins
 */
@Deprecated
public class CoberturaReportParserUtils {

  private CoberturaReportParserUtils() {
  }

  public interface FileResolver {

    /**
     * Return a SonarQube file resource from a filename present in Cobertura report
     */
    Resource resolve(String filename);
  }

  /**
   * Parse a Cobertura xml report and create measures accordingly
   */
  public static void parseReport(File xmlFile, final SensorContext context, final FileResolver fileResolver) {
    try {
      StaxParser parser = new StaxParser(new StaxParser.XmlStreamHandler() {

        public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
          rootCursor.advance();
          collectPackageMeasures(rootCursor.descendantElementCursor("package"), context, fileResolver);
        }
      });
      parser.parse(xmlFile);
    } catch (XMLStreamException e) {
      throw new XmlParserException(e);
    }
  }

  private static void collectPackageMeasures(SMInputCursor pack, SensorContext context, final FileResolver fileResolver) throws XMLStreamException {
    while (pack.getNext() != null) {
      Map<String, CoverageMeasuresBuilder> builderByFilename = Maps.newHashMap();
      collectFileMeasures(pack.descendantElementCursor("class"), builderByFilename);
      for (Map.Entry<String, CoverageMeasuresBuilder> entry : builderByFilename.entrySet()) {
        String filename = sanitizeFilename(entry.getKey());
        Resource file = fileResolver.resolve(filename);
        if (fileExists(context, file)) {
          for (Measure measure : entry.getValue().createMeasures()) {
            context.saveMeasure(file, measure);
          }
        }
      }
    }
  }

  private static boolean fileExists(SensorContext context, Resource file) {
    return context.getResource(file) != null;
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

  private static String sanitizeFilename(String s) {
    String fileName = FilenameUtils.removeExtension(s);
    fileName = fileName.replace('/', '.').replace('\\', '.');
    return fileName;
  }

}
