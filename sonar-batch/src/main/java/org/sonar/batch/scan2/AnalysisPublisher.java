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
package org.sonar.batch.scan2;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.Issue.Severity;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.ZipUtils;
import org.sonar.api.utils.text.JsonWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public final class AnalysisPublisher {

  private static final Logger LOG = LoggerFactory.getLogger(AnalysisPublisher.class);
  private final Settings settings;
  private final FileSystem fs;
  private final MeasureCache measureCache;
  private final ProjectDefinition def;
  private final IssueCache issueCache;

  public AnalysisPublisher(ProjectDefinition def, Settings settings, FileSystem fs,
    MeasureCache measureCache,
    IssueCache analyzerIssueCache) {
    this.def = def;
    this.settings = settings;
    this.fs = fs;
    this.measureCache = measureCache;
    this.issueCache = analyzerIssueCache;
  }

  public void execute() {
    if (settings.getBoolean("sonar.skipPublish")) {
      LOG.debug("Publishing of results is skipped");
      return;
    }
    File exportDir = prepareExportDir();

    exportAnalysisProperties(exportDir);

    exportSourceFiles(exportDir);

    exportMeasures(exportDir);

    exportIssues(exportDir);

    createZip(exportDir);

  }

  private void createZip(File exportDir) {
    File exportZip = new File(fs.workDir(), def.getKey() + "-export.zip");
    try {
      ZipUtils.zipDir(exportDir, exportZip);
      FileUtils.deleteDirectory(exportDir);
    } catch (IOException e) {
      throw unableToExport(e);
    }
    LOG.info("Results packaged in " + exportZip);
  }

  private IllegalStateException unableToExport(IOException e) {
    return new IllegalStateException("Unable to export result of analyzis", e);
  }

  private void exportIssues(File exportDir) {
    File issuesFile = new File(exportDir, "issues.json");
    FileWriter issueWriter = null;
    try {
      issueWriter = new FileWriter(issuesFile);
      JsonWriter jsonWriter = JsonWriter.of(issueWriter);
      jsonWriter
        .beginObject().name("issues")
        .beginArray();
      for (Issue issue : issueCache.byModule(def.getKey())) {
        jsonWriter.beginObject()
          .prop("repository", issue.ruleKey().repository())
          .prop("rule", issue.ruleKey().rule());
        InputPath inputPath = issue.inputPath();
        if (inputPath != null) {
          jsonWriter.prop("path", inputPath.relativePath());
        }
        jsonWriter.prop("message", issue.message())
          .prop("effortToFix", issue.effortToFix())
          .prop("line", issue.line());
        Severity overridenSeverity = issue.overridenSeverity();
        if (overridenSeverity != null) {
          jsonWriter.prop("severity", overridenSeverity.name());
        }
        jsonWriter.endObject();
      }
      jsonWriter.endArray()
        .endObject()
        .close();
    } catch (IOException e) {
      throw unableToExport(e);
    } finally {
      IOUtils.closeQuietly(issueWriter);
    }
  }

  private void exportMeasures(File exportDir) {
    File measuresFile = new File(exportDir, "measures.json");
    FileWriter measureWriter = null;
    try {
      measureWriter = new FileWriter(measuresFile);
      JsonWriter jsonWriter = JsonWriter.of(measureWriter);
      jsonWriter
        .beginObject().name("measures")
        .beginArray();
      for (Measure<?> measure : measureCache.byModule(def.getKey())) {
        jsonWriter.beginObject()
          .prop("metricKey", measure.metric().key());
        InputFile inputFile = measure.inputFile();
        if (inputFile != null) {
          jsonWriter.prop("filePath", inputFile.relativePath());
        }
        jsonWriter.prop("value", String.valueOf(measure.value()))
          .endObject();
      }
      jsonWriter.endArray()
        .endObject()
        .close();
    } catch (IOException e) {
      throw unableToExport(e);
    } finally {
      IOUtils.closeQuietly(measureWriter);
    }
  }

  private void exportSourceFiles(File exportDir) {
    File sourceDir = new File(exportDir, "sources");
    for (InputFile inputFile : fs.inputFiles(fs.predicates().all())) {
      File dest = new File(sourceDir, inputFile.relativePath());
      try {
        FileUtils.copyFile(inputFile.file(), dest);
      } catch (IOException e) {
        throw unableToExport(e);
      }
    }
  }

  private void exportAnalysisProperties(File exportDir) {
    File propsFile = new File(exportDir, "analysis.properties");
    Properties props = new Properties();
    props.putAll(settings.getProperties());
    FileWriter writer = null;
    try {
      writer = new FileWriter(propsFile);
      props.store(writer, "SonarQube batch");
    } catch (IOException e) {
      throw unableToExport(e);
    } finally {
      IOUtils.closeQuietly(writer);
    }
  }

  private File prepareExportDir() {
    File exportDir = new File(fs.workDir(), "export");
    try {
      if (exportDir.exists()) {
        FileUtils.forceDelete(exportDir);
      }
      FileUtils.forceMkdir(exportDir);
    } catch (IOException e) {
      throw unableToExport(e);
    }
    return exportDir;
  }
}
