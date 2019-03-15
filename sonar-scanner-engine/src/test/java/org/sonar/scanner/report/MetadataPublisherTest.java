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
package org.sonar.scanner.report;

import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.scanner.ProjectInfo;
import org.sonar.scanner.bootstrap.ScannerPlugin;
import org.sonar.scanner.bootstrap.ScannerPluginRepository;
import org.sonar.scanner.cpd.CpdSettings;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.rule.QProfile;
import org.sonar.scanner.rule.QualityProfiles;
import org.sonar.scanner.scan.ScanProperties;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scm.ScmConfiguration;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class MetadataPublisherTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private DefaultInputModule rootModule;
  private MetadataPublisher underTest;
  private ScanProperties properties = mock(ScanProperties.class);
  private QualityProfiles qProfiles = mock(QualityProfiles.class);
  private ProjectInfo projectInfo = mock(ProjectInfo.class);
  private CpdSettings cpdSettings = mock(CpdSettings.class);
  private InputModuleHierarchy inputModuleHierarchy;
  private ScannerPluginRepository pluginRepository = mock(ScannerPluginRepository.class);
  private BranchConfiguration branches;
  private ScmConfiguration scmConfiguration;
  private ScmProvider scmProvider = mock(ScmProvider.class);

  @Before
  public void prepare() throws IOException {
    when(projectInfo.getAnalysisDate()).thenReturn(new Date(1234567L));
    when(scmProvider.relativePathFromScmRoot(any(Path.class))).thenReturn(Paths.get("dummy/path"));
    when(scmProvider.revisionId(any(Path.class))).thenReturn("dummy-sha1");

    createPublisher(ProjectDefinition.create());
    when(pluginRepository.getPluginsByKey()).thenReturn(emptyMap());
  }

  private void createPublisher(ProjectDefinition def) throws IOException {
    Path rootBaseDir = temp.newFolder().toPath();
    Path moduleBaseDir = rootBaseDir.resolve("moduleDir");
    Files.createDirectory(moduleBaseDir);
    rootModule = new DefaultInputModule(def
      .setBaseDir(rootBaseDir.toFile())
      .setKey("root")
      .setWorkDir(temp.newFolder()), TestInputFileBuilder.nextBatchId());
    inputModuleHierarchy = mock(InputModuleHierarchy.class);
    when(inputModuleHierarchy.root()).thenReturn(rootModule);
    DefaultInputModule child = new DefaultInputModule(ProjectDefinition.create()
      .setKey("module")
      .setBaseDir(moduleBaseDir.toFile())
      .setWorkDir(temp.newFolder()), TestInputFileBuilder.nextBatchId());
    when(inputModuleHierarchy.children(rootModule)).thenReturn(Collections.singletonList(child));
    when(inputModuleHierarchy.relativePathToRoot(child)).thenReturn("modulePath");
    when(inputModuleHierarchy.relativePathToRoot(rootModule)).thenReturn("");
    branches = mock(BranchConfiguration.class);
    scmConfiguration = mock(ScmConfiguration.class);
    when(scmConfiguration.provider()).thenReturn(scmProvider);
    underTest = new MetadataPublisher(projectInfo, inputModuleHierarchy, properties, qProfiles, cpdSettings,
      pluginRepository, branches, scmConfiguration);
  }

  @Test
  public void write_metadata() throws Exception {
    Date date = new Date();
    when(qProfiles.findAll()).thenReturn(asList(new QProfile("q1", "Q1", "java", date)));
    when(pluginRepository.getPluginsByKey()).thenReturn(ImmutableMap.of(
      "java", new ScannerPlugin("java", 12345L, null),
      "php", new ScannerPlugin("php", 45678L, null)));
    File outputDir = temp.newFolder();
    ScannerReportWriter writer = new ScannerReportWriter(outputDir);

    underTest.publish(writer);

    ScannerReportReader reader = new ScannerReportReader(outputDir);
    ScannerReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getAnalysisDate()).isEqualTo(1234567L);
    assertThat(metadata.getProjectKey()).isEqualTo("root");
    assertThat(metadata.getModulesProjectRelativePathByKeyMap()).containsOnly(entry("module", "modulePath"), entry("root", ""));
    assertThat(metadata.getProjectVersion()).isEmpty();
    assertThat(metadata.getQprofilesPerLanguageMap()).containsOnly(entry("java", org.sonar.scanner.protocol.output.ScannerReport.Metadata.QProfile.newBuilder()
      .setKey("q1")
      .setName("Q1")
      .setLanguage("java")
      .setRulesUpdatedAt(date.getTime())
      .build()));
    assertThat(metadata.getPluginsByKey()).containsOnly(entry("java", org.sonar.scanner.protocol.output.ScannerReport.Metadata.Plugin.newBuilder()
        .setKey("java")
        .setUpdatedAt(12345)
        .build()),
      entry("php", org.sonar.scanner.protocol.output.ScannerReport.Metadata.Plugin.newBuilder()
        .setKey("php")
        .setUpdatedAt(45678)
        .build()));
  }

  @Test
  public void write_project_branch() throws Exception {
    when(cpdSettings.isCrossProjectDuplicationEnabled()).thenReturn(false);

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
    assertThat(metadata.getProjectKey()).isEqualTo("root");
    assertThat(metadata.getDeprecatedBranch()).isEqualTo("myBranch");
    assertThat(metadata.getCrossProjectDuplicationActivated()).isFalse();
  }

  @Test
  public void write_project_organization() throws Exception {
    when(properties.organizationKey()).thenReturn(Optional.of("SonarSource"));

    File outputDir = temp.newFolder();
    ScannerReportWriter writer = new ScannerReportWriter(outputDir);

    underTest.publish(writer);

    ScannerReportReader reader = new ScannerReportReader(outputDir);
    ScannerReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getOrganizationKey()).isEqualTo("SonarSource");
  }

  @Test
  @UseDataProvider("projectVersions")
  public void write_project_version(@Nullable String projectVersion, String expected) throws Exception {
    when(projectInfo.getProjectVersion()).thenReturn(Optional.ofNullable(projectVersion));
    when(properties.organizationKey()).thenReturn(Optional.of("SonarSource"));

    File outputDir = temp.newFolder();
    ScannerReportWriter writer = new ScannerReportWriter(outputDir);

    underTest.publish(writer);

    ScannerReportReader reader = new ScannerReportReader(outputDir);
    ScannerReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getProjectVersion()).isEqualTo(expected);
  }

  @DataProvider
  public static Object[][] projectVersions() {
    String version = randomAlphabetic(15);
    return new Object[][] {
      {null, ""},
      {"", ""},
      {"5.6.3", "5.6.3"},
      {version, version}
    };
  }

  @Test
  @UseDataProvider("buildStrings")
  public void write_buildString(@Nullable String buildString, String expected) throws Exception {
    when(projectInfo.getBuildString()).thenReturn(Optional.ofNullable(buildString));
    when(properties.organizationKey()).thenReturn(Optional.of("SonarSource"));

    File outputDir = temp.newFolder();
    ScannerReportWriter writer = new ScannerReportWriter(outputDir);

    underTest.publish(writer);

    ScannerReportReader reader = new ScannerReportReader(outputDir);
    ScannerReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getBuildString()).isEqualTo(expected);
  }

  @DataProvider
  public static Object[][] buildStrings() {
    String randomBuildString = randomAlphabetic(15);
    return new Object[][] {
      {null, ""},
      {"", ""},
      {"5.6.3", "5.6.3"},
      {randomBuildString, randomBuildString}
    };
  }

  @Test
  public void write_long_lived_branch_info() throws Exception {
    String branchName = "long-lived";
    when(branches.branchName()).thenReturn(branchName);
    when(branches.branchType()).thenReturn(BranchType.LONG);

    File outputDir = temp.newFolder();
    underTest.publish(new ScannerReportWriter(outputDir));

    ScannerReportReader reader = new ScannerReportReader(outputDir);
    ScannerReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getBranchName()).isEqualTo(branchName);
    assertThat(metadata.getBranchType()).isEqualTo(ScannerReport.Metadata.BranchType.LONG);
  }

  @Test
  public void write_short_lived_branch_info() throws Exception {
    String branchName = "feature";
    String branchTarget = "short-lived";
    when(branches.branchName()).thenReturn(branchName);
    when(branches.longLivingSonarReferenceBranch()).thenReturn(branchTarget);

    File outputDir = temp.newFolder();
    underTest.publish(new ScannerReportWriter(outputDir));

    ScannerReportReader reader = new ScannerReportReader(outputDir);
    ScannerReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getBranchName()).isEqualTo(branchName);
    assertThat(metadata.getBranchType()).isEqualTo(ScannerReport.Metadata.BranchType.SHORT);
    assertThat(metadata.getMergeBranchName()).isEqualTo(branchTarget);
  }

  @Test
  public void write_project_basedir() throws Exception {
    String path = "some/dir";
    Path relativePathFromScmRoot = Paths.get(path);
    when(scmProvider.relativePathFromScmRoot(any(Path.class))).thenReturn(relativePathFromScmRoot);

    File outputDir = temp.newFolder();
    underTest.publish(new ScannerReportWriter(outputDir));

    ScannerReportReader reader = new ScannerReportReader(outputDir);
    ScannerReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getRelativePathFromScmRoot()).isEqualTo(path);
  }

  @Test
  public void write_revision_id() throws Exception {
    String revisionId = "some-sha1";
    when(scmProvider.revisionId(any(Path.class))).thenReturn(revisionId);

    File outputDir = temp.newFolder();
    underTest.publish(new ScannerReportWriter(outputDir));

    ScannerReportReader reader = new ScannerReportReader(outputDir);
    ScannerReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getScmRevisionId()).isEqualTo(revisionId);
  }

  @Test
  public void should_not_crash_when_scm_provider_does_not_support_relativePathFromScmRoot() throws IOException {
    String revisionId = "some-sha1";

    ScmProvider fakeScmProvider = new ScmProvider() {
      @Override
      public String key() {
        return "foo";
      }

      @Override
      public String revisionId(Path path) {
        return revisionId;
      }
    };
    when(scmConfiguration.provider()).thenReturn(fakeScmProvider);

    File outputDir = temp.newFolder();
    underTest.publish(new ScannerReportWriter(outputDir));

    ScannerReportReader reader = new ScannerReportReader(outputDir);
    ScannerReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getScmRevisionId()).isEqualTo(revisionId);
  }

  @Test
  public void should_not_crash_when_scm_provider_does_not_support_revisionId() throws IOException {
    String relativePathFromScmRoot = "some/path";

    ScmProvider fakeScmProvider = new ScmProvider() {
      @Override
      public String key() {
        return "foo";
      }

      @Override
      public Path relativePathFromScmRoot(Path path) {
        return Paths.get(relativePathFromScmRoot);
      }
    };
    when(scmConfiguration.provider()).thenReturn(fakeScmProvider);

    File outputDir = temp.newFolder();
    underTest.publish(new ScannerReportWriter(outputDir));

    ScannerReportReader reader = new ScannerReportReader(outputDir);
    ScannerReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getRelativePathFromScmRoot()).isEqualTo(relativePathFromScmRoot);
  }
}
