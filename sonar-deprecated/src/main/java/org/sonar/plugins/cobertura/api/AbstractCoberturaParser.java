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
package org.sonar.plugins.cobertura.api;

import com.google.common.collect.Maps;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoverageMeasuresBuilder;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.StaxParser;
import org.sonar.api.utils.XmlParserException;

import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.text.ParseException;
import java.util.Map;

import static java.util.Locale.ENGLISH;
import static org.sonar.api.utils.ParsingUtils.parseNumber;

/**
 * @since 2.4
 * @deprecated since 3.7 Used to keep backward compatibility since extraction
 * of Cobertura plugin.
 */
@Deprecated
public abstract class AbstractCoberturaParser {

  public void parseReport(File xmlFile, final SensorContext context) {
    try {
      StaxParser parser = new StaxParser(new StaxParser.XmlStreamHandler() {

        public void stream(SMHierarchicCursor rootCursor) throws XMLStreamException {
          try {
            rootCursor.advance();
            collectPackageMeasures(rootCursor.descendantElementCursor("package"), context);
          } catch (ParseException e) {
            throw new XMLStreamException(e);
          }
        }
      });
      parser.parse(xmlFile);
    } catch (XMLStreamException e) {
      throw new XmlParserException(e);
    }
  }

  private void collectPackageMeasures(SMInputCursor pack, SensorContext context) throws ParseException, XMLStreamException {
    while (pack.getNext() != null) {
      Map<String, CoverageMeasuresBuilder> builderByFilename = Maps.newHashMap();
      collectFileMeasures(pack.descendantElementCursor("class"), builderByFilename);
      for (Map.Entry<String, CoverageMeasuresBuilder> entry : builderByFilename.entrySet()) {
        String filename = sanitizeFilename(entry.getKey());
        Resource file = getResource(filename);
        if (fileExists(context, file)) {
          for (Measure measure : entry.getValue().createMeasures()) {
            context.saveMeasure(file, measure);
          }
        }
      }
    }
  }

  private boolean fileExists(SensorContext context, Resource file) {
    return context.getResource(file) != null;
  }

  private void collectFileMeasures(SMInputCursor clazz, Map<String, CoverageMeasuresBuilder> builderByFilename) throws ParseException, XMLStreamException {
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

  private void collectFileData(SMInputCursor clazz, CoverageMeasuresBuilder builder) throws ParseException, XMLStreamException {
    SMInputCursor line = clazz.childElementCursor("lines").advance().childElementCursor("line");
    while (line.getNext() != null) {
      int lineId = Integer.parseInt(line.getAttrValue("number"));
      builder.setHits(lineId, (int) parseNumber(line.getAttrValue("hits"), ENGLISH));

      String isBranch = line.getAttrValue("branch");
      String text = line.getAttrValue("condition-coverage");
      if (StringUtils.equals(isBranch, "true") && StringUtils.isNotBlank(text)) {
        String[] conditions = StringUtils.split(StringUtils.substringBetween(text, "(", ")"), "/");
        builder.setConditions(lineId, Integer.parseInt(conditions[1]), Integer.parseInt(conditions[0]));
      }
    }
  }

  private String sanitizeFilename(String s) {
    String fileName = FilenameUtils.removeExtension(s);
    fileName = fileName.replace('/', '.').replace('\\', '.');
    return fileName;
  }

  protected abstract Resource getResource(String fileName);
}
