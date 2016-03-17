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
package org.sonar.batch.phases;

import org.sonar.api.batch.SensorContext;
import org.sonar.batch.cpd.CpdExecutor;
import org.sonar.batch.events.BatchStepEvent;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.issue.ignore.scanner.IssueExclusionsLoader;
import org.sonar.batch.report.ReportPublisher;
import org.sonar.batch.rule.QProfileVerifier;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.batch.scan.filesystem.FileSystemLogger;

public final class PublishPhaseExecutor extends AbstractPhaseExecutor {

  private final EventBus eventBus;
  private final ReportPublisher reportPublisher;
  private final CpdExecutor cpdExecutor;

  public PublishPhaseExecutor(InitializersExecutor initializersExecutor, PostJobsExecutor postJobsExecutor, SensorsExecutor sensorsExecutor, SensorContext sensorContext,
    DefaultIndex index, EventBus eventBus, ReportPublisher reportPublisher, ProjectInitializer pi, FileSystemLogger fsLogger, DefaultModuleFileSystem fs,
    QProfileVerifier profileVerifier, IssueExclusionsLoader issueExclusionsLoader, CpdExecutor cpdExecutor) {
    super(initializersExecutor, postJobsExecutor, sensorsExecutor, sensorContext, index, eventBus, pi, fsLogger, fs, profileVerifier, issueExclusionsLoader);
    this.eventBus = eventBus;
    this.reportPublisher = reportPublisher;
    this.cpdExecutor = cpdExecutor;
  }

  @Override
  protected void executeOnRoot() {
    computeDuplications();
    publishReportJob();
  }

  private void computeDuplications() {
    String stepName = "Computing duplications";
    eventBus.fireEvent(new BatchStepEvent(stepName, true));
    cpdExecutor.execute();
    eventBus.fireEvent(new BatchStepEvent(stepName, false));
  }

  private void publishReportJob() {
    String stepName = "Publish report";
    eventBus.fireEvent(new BatchStepEvent(stepName, true));
    this.reportPublisher.execute();
    eventBus.fireEvent(new BatchStepEvent(stepName, false));
  }
}
