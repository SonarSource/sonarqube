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
package org.sonar.plugins.cobertura;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.AbstractCoverageExtension;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.maven.DependsUponMavenPlugin;
import org.sonar.api.batch.maven.MavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.PropertiesBuilder;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.StaxParser;
import org.sonar.api.utils.XmlParserException;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Locale.ENGLISH;
import static org.sonar.api.utils.ParsingUtils.parseNumber;
import static org.sonar.api.utils.ParsingUtils.scaleValue;

public class CoberturaSensor extends AbstractCoverageExtension implements Sensor, DependsUponMavenPlugin {

  private CoberturaMavenPluginHandler handler;

  public CoberturaSensor(CoberturaMavenPluginHandler handler) {
    this.handler = handler;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return super.shouldExecuteOnProject(project) && project.getFileSystem().hasJavaSourceFiles();
  }

  public void analyse(Project project, SensorContext context) {
    File report = getReport(project);
    if (report != null) {
      parseReport(report, context);
    }
  }

  public MavenPluginHandler getMavenPluginHandler(Project project) {
    if (project.getAnalysisType().equals(Project.AnalysisType.DYNAMIC)) {
      return handler;
    }
    return null;
  }

  protected File getReport(Project project) {
    File report = getReportFromProperty(project);
    if (report == null) {
      report = getReportFromPluginConfiguration(project);
    }
    if (report == null) {
      report = getReportFromDefaultPath(project);
    }

    if (report == null || !report.exists() || !report.isFile()) {
      LoggerFactory.getLogger(CoberturaSensor.class).warn("Cobertura report not found at {}", report);
      report = null;
    }
    return report;
  }

  private File getReportFromProperty(Project project) {
    String path = (String) project.getProperty(CoreProperties.COBERTURA_REPORT_PATH_PROPERTY);
    if (path != null) {
      return project.getFileSystem().resolvePath(path);
    }
    return null;
  }

  private File getReportFromPluginConfiguration(Project project) {
    MavenPlugin mavenPlugin = MavenPlugin.getPlugin(project.getPom(), CoberturaMavenPluginHandler.GROUP_ID,
        CoberturaMavenPluginHandler.ARTIFACT_ID);
    if (mavenPlugin != null) {
      String path = mavenPlugin.getParameter("outputDirectory");
      if (path != null) {
        return new File(project.getFileSystem().resolvePath(path), "coverage.xml");
      }
    }
    return null;
  }

  private File getReportFromDefaultPath(Project project) {
    return new File(project.getFileSystem().getReportOutputDir(), "cobertura/coverage.xml");
  }

  protected void parseReport(File xmlFile, final SensorContext context) {
    try {
      LoggerFactory.getLogger(CoberturaSensor.class).info("parsing {}", xmlFile);
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
      Map<String, FileData> fileDataPerFilename = new HashMap<String, FileData>();
      collectFileMeasures(pack.descendantElementCursor("class"), fileDataPerFilename);
      for (FileData cci : fileDataPerFilename.values()) {
        if (javaFileExist(context, cci.getJavaFile())) {
          for (Measure measure : cci.getMeasures()) {
            context.saveMeasure(cci.getJavaFile(), measure);
          }
        }
      }
    }
  }

  private boolean javaFileExist(SensorContext context, JavaFile javaFile) {
    return context.getResource(javaFile) != null;
  }

  private void collectFileMeasures(SMInputCursor clazz, Map<String, FileData> dataPerFilename) throws ParseException, XMLStreamException {
    while (clazz.getNext() != null) {
      String fileName = FilenameUtils.removeExtension(clazz.getAttrValue("filename"));
      fileName = fileName.replace('/', '.').replace('\\', '.');
      FileData data = dataPerFilename.get(fileName);
      if (data == null) {
        data = new FileData(new JavaFile(fileName));
        dataPerFilename.put(fileName, data);
      }
      collectFileData(clazz, data);
    }
  }

  private void collectFileData(SMInputCursor clazz, FileData data) throws ParseException, XMLStreamException {
    SMInputCursor line = clazz.childElementCursor("lines").advance().childElementCursor("line");
    while (line.getNext() != null) {
      String lineId = line.getAttrValue("number");
      data.addLine(lineId, (int) parseNumber(line.getAttrValue("hits"), ENGLISH));

      String text = line.getAttrValue("condition-coverage");
      if (StringUtils.isNotBlank(text)) {
        String[] conditions = StringUtils.split(StringUtils.substringBetween(text, "(", ")"), "/");
        data.addConditionLine(lineId, Integer.parseInt(conditions[0]), Integer.parseInt(conditions[1]), StringUtils.substringBefore(text,
            " "));
      }
    }
  }

  private class FileData {

    private int lines = 0;
    private int conditions = 0;
    private int coveredLines = 0;
    private int coveredConditions = 0;

    private JavaFile javaFile;
    private PropertiesBuilder<String, Integer> lineHitsBuilder = new PropertiesBuilder<String, Integer>(CoreMetrics.COVERAGE_LINE_HITS_DATA);
    private PropertiesBuilder<String, String> branchHitsBuilder = new PropertiesBuilder<String, String>(
        CoreMetrics.BRANCH_COVERAGE_HITS_DATA);

    public void addLine(String lineId, int lineHits) {
      lines++;
      if (lineHits > 0) {
        coveredLines++;
      }
      lineHitsBuilder.add(lineId, lineHits);
    }

    public void addConditionLine(String lineId, int coveredConditions, int conditions, String label) {
      this.conditions += conditions;
      this.coveredConditions += coveredConditions;
      branchHitsBuilder.add(lineId, label);
    }

    public FileData(JavaFile javaFile) {
      this.javaFile = javaFile;
    }

    public List<Measure> getMeasures() {
      List<Measure> measures = new ArrayList<Measure>();
      if (lines > 0) {
        measures.add(new Measure(CoreMetrics.COVERAGE, calculateCoverage(coveredLines + coveredConditions, lines + conditions)));

        measures.add(new Measure(CoreMetrics.LINE_COVERAGE, calculateCoverage(coveredLines, lines)));
        measures.add(new Measure(CoreMetrics.LINES_TO_COVER, (double) lines));
        measures.add(new Measure(CoreMetrics.UNCOVERED_LINES, (double) lines - coveredLines));
        measures.add(lineHitsBuilder.build().setPersistenceMode(PersistenceMode.DATABASE));

        if (conditions > 0) {
          measures.add(new Measure(CoreMetrics.BRANCH_COVERAGE, calculateCoverage(coveredConditions, conditions)));
          measures.add(new Measure(CoreMetrics.CONDITIONS_TO_COVER, (double) conditions));
          measures.add(new Measure(CoreMetrics.UNCOVERED_CONDITIONS, (double) conditions - coveredConditions));
          measures.add(branchHitsBuilder.build().setPersistenceMode(PersistenceMode.DATABASE));
        }
      }
      return measures;
    }

    public JavaFile getJavaFile() {
      return javaFile;
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  private double calculateCoverage(int coveredElements, int elements) {
    if (elements > 0) {
      return scaleValue(100.0 * ((double) coveredElements / (double) elements));
    }
    return 0.0;
  }
}
