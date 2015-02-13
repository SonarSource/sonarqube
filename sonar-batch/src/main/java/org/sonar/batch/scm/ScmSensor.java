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
package org.sonar.batch.scm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.batch.protocol.input.FileData;
import org.sonar.batch.protocol.input.ProjectRepositories;
import org.sonar.batch.scan.filesystem.InputFileMetadata;
import org.sonar.batch.scan.filesystem.InputPathCache;

import java.util.LinkedList;
import java.util.List;

public final class ScmSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(ScmSensor.class);

  private final ProjectDefinition projectDefinition;
  private final ScmConfiguration configuration;
  private final FileSystem fs;
  private final ProjectRepositories projectReferentials;
  private final InputPathCache inputPathCache;

  public ScmSensor(ProjectDefinition projectDefinition, ScmConfiguration configuration,
    ProjectRepositories projectReferentials, FileSystem fs, InputPathCache inputPathCache) {
    this.projectDefinition = projectDefinition;
    this.configuration = configuration;
    this.projectReferentials = projectReferentials;
    this.fs = fs;
    this.inputPathCache = inputPathCache;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("SCM Sensor")
      .disabledInPreview();
  }

  @Override
  public void execute(final SensorContext context) {
    if (configuration.isDisabled()) {
      LOG.info("SCM Sensor is disabled");
      return;
    }
    if (configuration.provider() == null) {
      LOG.info("No SCM system was detected. You can use the '" + CoreProperties.SCM_PROVIDER_KEY + "' property to explicitly specify it.");
      return;
    }

    List<InputFile> filesToBlame = collectFilesToBlame(context);
    if (!filesToBlame.isEmpty()) {
      String key = configuration.provider().key();
      LOG.info("SCM provider for this project is: " + key);
      DefaultBlameOutput output = new DefaultBlameOutput(context, filesToBlame);
      configuration.provider().blameCommand().blame(new DefaultBlameInput(fs, filesToBlame), output);
      output.finish();
    }
  }

  private List<InputFile> collectFilesToBlame(final SensorContext context) {
    if (configuration.forceReloadAll()) {
      LOG.warn("Forced reloading of SCM data for all files.");
    }
    List<InputFile> filesToBlame = new LinkedList<InputFile>();
    for (InputFile f : fs.inputFiles(fs.predicates().all())) {
      if (!configuration.forceReloadAll()) {
        copyPreviousMeasuresForUnmodifiedFiles(context, filesToBlame, f);
      } else {
        filesToBlame.add(f);
      }
    }
    return filesToBlame;
  }

  private void copyPreviousMeasuresForUnmodifiedFiles(final SensorContext context, List<InputFile> filesToBlame, InputFile f) {
    FileData fileData = projectReferentials.fileData(projectDefinition.getKeyWithBranch(), f.relativePath());

    if (f.status() == Status.SAME && fileData != null) {
      if (fileData.needBlame()) {
        addIfNotEmpty(filesToBlame, (DefaultInputFile) f);
      } else {
        // Copy previous measures
        String scmAuthorsByLine = fileData.scmAuthorsByLine();
        String scmLastCommitDatetimesByLine = fileData.scmLastCommitDatetimesByLine();
        String scmRevisionsByLine = fileData.scmRevisionsByLine();
        if (scmAuthorsByLine != null
          && scmLastCommitDatetimesByLine != null
          && scmRevisionsByLine != null) {
          saveMeasures(context, f, scmAuthorsByLine, scmLastCommitDatetimesByLine, scmRevisionsByLine);
        }
      }
    } else {
      addIfNotEmpty(filesToBlame, (DefaultInputFile) f);
    }
  }

  private void addIfNotEmpty(List<InputFile> filesToBlame, DefaultInputFile f) {
    InputFileMetadata metadata = inputPathCache.getFileMetadata(f.moduleKey(), f.relativePath());
    if (!metadata.isEmpty()) {
      filesToBlame.add(f);
    }
  }

  static void saveMeasures(SensorContext context, InputFile f, String scmAuthorsByLine, String scmLastCommitDatetimesByLine, String scmRevisionsByLine) {
    ((DefaultMeasure<String>) context.<String>newMeasure()
      .onFile(f)
      .forMetric(CoreMetrics.SCM_AUTHORS_BY_LINE)
      .withValue(scmAuthorsByLine))
      .setFromCore()
      .save();
    ((DefaultMeasure<String>) context.<String>newMeasure()
      .onFile(f)
      .forMetric(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE)
      .withValue(scmLastCommitDatetimesByLine))
      .setFromCore()
      .save();
    ((DefaultMeasure<String>) context.<String>newMeasure()
      .onFile(f)
      .forMetric(CoreMetrics.SCM_REVISIONS_BY_LINE)
      .withValue(scmRevisionsByLine))
      .setFromCore()
      .save();
  }
}
