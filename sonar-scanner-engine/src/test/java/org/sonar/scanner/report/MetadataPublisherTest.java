/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.report;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.core.config.ScannerProperties;
import org.sonar.scanner.ProjectAnalysisInfo;
import org.sonar.scanner.cpd.CpdSettings;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.rule.ModuleQProfiles;
import org.sonar.scanner.rule.QProfile;
import org.sonar.scanner.scan.ProjectBranches;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetadataPublisherTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private DefaultInputModule rootModule;
  private MetadataPublisher underTest;
  private MapSettings settings;
  private ModuleQProfiles qProfiles;
  private ProjectAnalysisInfo projectAnalysisInfo;
  private CpdSettings cpdSettings;
  private InputModuleHierarchy inputModuleHierarchy;
  private AnalysisMode analysisMode;

  @Before
  public void prepare() throws IOException {
    projectAnalysisInfo = mock(ProjectAnalysisInfo.class);
    cpdSettings = mock(CpdSettings.class);
    when(projectAnalysisInfo.analysisDate()).thenReturn(new Date(1234567L));
    settings = new MapSettings();
    qProfiles = mock(ModuleQProfiles.class);
    createPublisher(ProjectDefinition.create().setKey("foo"));
  }

  private void createPublisher(ProjectDefinition def) throws IOException {
    rootModule = new DefaultInputModule(def.setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()), TestInputFileBuilder.nextBatchId());
    inputModuleHierarchy = mock(InputModuleHierarchy.class);
    when(inputModuleHierarchy.root()).thenReturn(rootModule);
    analysisMode = mock(AnalysisMode.class);
    ProjectBranches branches = mock(ProjectBranches.class);
    underTest = new MetadataPublisher(projectAnalysisInfo, inputModuleHierarchy, settings.asConfig(), qProfiles, cpdSettings, analysisMode, branches);
  }

  @Test
  public void write_metadata() throws Exception {
    settings.setProperty(CoreProperties.CPD_CROSS_PROJECT, "true");
    Date date = new Date();
    when(qProfiles.findAll()).thenReturn(asList(new QProfile("q1", "Q1", "java", date)));
    File outputDir = temp.newFolder();
    ScannerReportWriter writer = new ScannerReportWriter(outputDir);

    underTest.publish(writer);

    ScannerReportReader reader = new ScannerReportReader(outputDir);
    ScannerReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getAnalysisDate()).isEqualTo(1234567L);
    assertThat(metadata.getProjectKey()).isEqualTo("foo");
    assertThat(metadata.getProjectKey()).isEqualTo("foo");
    assertThat(metadata.getIncremental()).isFalse();
    assertThat(metadata.getQprofilesPerLanguage()).containsOnly(entry("java", org.sonar.scanner.protocol.output.ScannerReport.Metadata.QProfile.newBuilder()
      .setKey("q1")
      .setName("Q1")
      .setLanguage("java")
      .setRulesUpdatedAt(date.getTime())
      .build()));
  }

  @Test
  public void write_project_branch() throws Exception {
    when(cpdSettings.isCrossProjectDuplicationEnabled()).thenReturn(false);
    settings.setProperty(CoreProperties.CPD_CROSS_PROJECT, "true");
    settings.setProperty(CoreProperties.PROJECT_BRANCH_PROPERTY, "myBranch");

    ProjectDefinition projectDef = ProjectDefinition.create()
      .setKey("foo")
      .setProperty(CoreProperties.PROJECT_BRANCH_PROPERTY, "myBranch");
    createPublisher(projectDef);

    File outputDir = temp.newFolder();
    ScannerReportWriter writer = new ScannerReportWriter(outputDir);

    underTest.publish(writer);

    ScannerReportReader reader = new ScannerReportReader(outputDir);
    ScannerReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getAnalysisDate()).isEqualTo(1234567L);
    assertThat(metadata.getProjectKey()).isEqualTo("foo");
    assertThat(metadata.getDeprecatedBranch()).isEqualTo("myBranch");
    assertThat(metadata.getCrossProjectDuplicationActivated()).isFalse();
  }

  @Test
  public void write_project_organization() throws Exception {
    settings.setProperty(ScannerProperties.ORGANIZATION, "SonarSource");

    File outputDir = temp.newFolder();
    ScannerReportWriter writer = new ScannerReportWriter(outputDir);

    underTest.publish(writer);

    ScannerReportReader reader = new ScannerReportReader(outputDir);
    ScannerReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getOrganizationKey()).isEqualTo("SonarSource");
  }

}
