/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.scanner.scm;

import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.index.BatchComponent;
import org.sonar.scanner.index.BatchComponentCache;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Changesets.Builder;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.repository.FileData;
import org.sonar.scanner.repository.ProjectRepositories;

public final class ScmSensor implements Sensor {

  private static final Logger LOG = Loggers.get(ScmSensor.class);

  private final ProjectDefinition projectDefinition;
  private final ScmConfiguration configuration;
  private final FileSystem fs;
  private final ProjectRepositories projectRepositories;
  private final BatchComponentCache componentCache;
  private final ReportPublisher publishReportJob;

  public ScmSensor(ProjectDefinition projectDefinition, ScmConfiguration configuration,
    ProjectRepositories projectRepositories, FileSystem fs, BatchComponentCache componentCache, ReportPublisher publishReportJob) {
    this.projectDefinition = projectDefinition;
    this.configuration = configuration;
    this.projectRepositories = projectRepositories;
    this.fs = fs;
    this.componentCache = componentCache;
    this.publishReportJob = publishReportJob;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor.name("SCM Sensor");
  }

  @Override
  public void execute(SensorContext context) {
    if (configuration.isDisabled()) {
      LOG.info("SCM Publisher is disabled");
      return;
    }
    if (configuration.provider() == null) {
      LOG.info("No SCM system was detected. You can use the '" + CoreProperties.SCM_PROVIDER_KEY + "' property to explicitly specify it.");
      return;
    }

    List<InputFile> filesToBlame = collectFilesToBlame();
    if (!filesToBlame.isEmpty()) {
      String key = configuration.provider().key();
      LOG.info("SCM provider for this project is: " + key);
      DefaultBlameOutput output = new DefaultBlameOutput(publishReportJob.getWriter(), componentCache, filesToBlame);
      try {
        configuration.provider().blameCommand().blame(new DefaultBlameInput(fs, filesToBlame), output);
      } catch (Exception e) {
        output.finish(false);
        throw e;
      }
      output.finish(true);
    }
  }

  private List<InputFile> collectFilesToBlame() {
    if (configuration.forceReloadAll()) {
      LOG.warn("Forced reloading of SCM data for all files.");
    }
    List<InputFile> filesToBlame = new LinkedList<>();
    for (InputFile f : fs.inputFiles(fs.predicates().all())) {
      if (configuration.forceReloadAll() || f.status() != Status.SAME) {
        addIfNotEmpty(filesToBlame, f);
      } else {
        // File status is SAME so that mean fileData exists
        FileData fileData = projectRepositories.fileData(projectDefinition.getKeyWithBranch(), f.relativePath());
        if (StringUtils.isEmpty(fileData.revision())) {
          addIfNotEmpty(filesToBlame, f);
        } else {
          askToCopyDataFromPreviousAnalysis(f);
        }
      }
    }
    return filesToBlame;
  }

  private void askToCopyDataFromPreviousAnalysis(InputFile f) {
    BatchComponent batchComponent = componentCache.get(f);
    Builder scmBuilder = ScannerReport.Changesets.newBuilder();
    scmBuilder.setComponentRef(batchComponent.batchId());
    scmBuilder.setCopyFromPrevious(true);
    publishReportJob.getWriter().writeComponentChangesets(scmBuilder.build());
  }

  private static void addIfNotEmpty(List<InputFile> filesToBlame, InputFile f) {
    if (!f.isEmpty()) {
      filesToBlame.add(f);
    }
  }

}
