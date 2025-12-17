/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.core.plugin.PluginType;
import org.sonar.scanner.ProjectInfo;
import org.sonar.scanner.bootstrap.ScannerPlugin;
import org.sonar.scanner.bootstrap.ScannerPluginRepository;
import org.sonar.scanner.cpd.CpdSettings;
import org.sonar.scanner.fs.InputModuleHierarchy;
import org.sonar.scanner.protocol.output.FileStructure;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.repository.ReferenceBranchSupplier;
import org.sonar.scanner.rule.QProfile;
import org.sonar.scanner.rule.QualityProfiles;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.branch.BranchType;
import org.sonar.scanner.scan.filesystem.InputComponentStore;
import org.sonar.scanner.scm.ScmConfiguration;
import org.sonar.scanner.scm.ScmRevision;

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetadataPublisherTest {

  @TempDir
  File temp;

  private MetadataPublisher underTest;
  private final QualityProfiles qProfiles = mock(QualityProfiles.class);
  private final ProjectInfo projectInfo = mock(ProjectInfo.class);
  private final CpdSettings cpdSettings = mock(CpdSettings.class);
  private final ReferenceBranchSupplier referenceBranchSupplier = mock(ReferenceBranchSupplier.class);
  private final ScannerPluginRepository pluginRepository = mock(ScannerPluginRepository.class);
  private BranchConfiguration branches;
  private ScmConfiguration scmConfiguration;
  private final ScmProvider scmProvider = mock(ScmProvider.class);
  private final ScmRevision scmRevision = mock(ScmRevision.class);
  private final InputComponentStore componentStore = mock(InputComponentStore.class);
  private ScannerReportWriter writer;
  private ScannerReportReader reader;

  @BeforeEach
  void prepare() throws IOException {
    File tempFolder = new File(temp, "fileStructure");
    assertThat(tempFolder.mkdir()).isTrue();
    FileStructure fileStructure = new FileStructure(tempFolder);
    writer = new ScannerReportWriter(fileStructure);
    reader = new ScannerReportReader(fileStructure);
    when(projectInfo.getAnalysisDate()).thenReturn(new Date(1234567L));
    when(scmProvider.relativePathFromScmRoot(any(Path.class))).thenReturn(Paths.get("dummy/path"));
    when(scmProvider.revisionId(any(Path.class))).thenReturn("dummy-sha1");

    createPublisher(ProjectDefinition.create());
    when(pluginRepository.getPluginsByKey()).thenReturn(emptyMap());
  }

  private void createPublisher(ProjectDefinition def) throws IOException {
    File rootBaseDirFile = new File(temp, "rootBaseDir");
    assertThat(rootBaseDirFile.mkdir()).isTrue();
    Path rootBaseDir = rootBaseDirFile.toPath();
    Path moduleBaseDir = rootBaseDir.resolve("moduleDir");
    Files.createDirectory(moduleBaseDir);
    File rootWorkDir = new File(temp, "rootWorkDir");
    assertThat(rootWorkDir.mkdir()).isTrue();
    DefaultInputModule rootModule = new DefaultInputModule(def
      .setBaseDir(rootBaseDir.toFile())
      .setKey("root")
      .setWorkDir(rootWorkDir), TestInputFileBuilder.nextBatchId());
    InputModuleHierarchy inputModuleHierarchy = mock(InputModuleHierarchy.class);
    when(inputModuleHierarchy.root()).thenReturn(rootModule);
    File childWorkDir = new File(temp, "childWorkDir");
    assertThat(childWorkDir.mkdir()).isTrue();
    DefaultInputModule child = new DefaultInputModule(ProjectDefinition.create()
      .setKey("module")
      .setBaseDir(moduleBaseDir.toFile())
      .setWorkDir(childWorkDir), TestInputFileBuilder.nextBatchId());
    when(inputModuleHierarchy.children(rootModule)).thenReturn(Collections.singletonList(child));
    when(inputModuleHierarchy.relativePathToRoot(child)).thenReturn("modulePath");
    when(inputModuleHierarchy.relativePathToRoot(rootModule)).thenReturn("");
    branches = mock(BranchConfiguration.class);
    scmConfiguration = mock(ScmConfiguration.class);
    when(scmConfiguration.provider()).thenReturn(scmProvider);
    underTest = new MetadataPublisher(projectInfo, inputModuleHierarchy, qProfiles, cpdSettings,
      pluginRepository, branches, scmRevision, componentStore, scmConfiguration, referenceBranchSupplier);
  }

  @Test
  void write_metadata() {
    Date date = new Date();
    when(qProfiles.findAll()).thenReturn(Collections.singletonList(new QProfile("q1", "Q1", "java", date)));
    when(pluginRepository.getPluginsByKey()).thenReturn(ImmutableMap.of(
      "java", new ScannerPlugin("java", 12345L, PluginType.BUNDLED, null),
      "php", new ScannerPlugin("php", 45678L, PluginType.BUNDLED, null)));
    when(referenceBranchSupplier.getFromProperties()).thenReturn("newCodeReference");
    underTest.publish(writer);

    ScannerReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getAnalysisDate()).isEqualTo(1234567L);
    assertThat(metadata.getNewCodeReferenceBranch()).isEqualTo("newCodeReference");
    assertThat(metadata.getProjectKey()).isEqualTo("root");
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
  void write_analyzed_and_not_analyzed_indexed_file_count_per_extension() {
    when(componentStore.getAnalyzedIndexedFileCountPerExtension()).thenReturn(ImmutableMap.of("java", 150, "js", 43));
    when(componentStore.getNotAnalyzedIndexedFileCountPerExtension()).thenReturn(ImmutableMap.of("ts", 27, "py", 10));

    underTest.publish(writer);

    ScannerReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getAnalyzedIndexedFileCountPerTypeMap()).contains(entry("java", 150), entry("js", 43));
    assertThat(metadata.getNotAnalyzedIndexedFileCountPerTypeMap()).contains(entry("ts", 27), entry("py", 10));
  }

  @ParameterizedTest
  @MethodSource("projectVersions")
  void write_project_version(@Nullable String projectVersion, String expected) {
    when(projectInfo.getProjectVersion()).thenReturn(Optional.ofNullable(projectVersion));

    underTest.publish(writer);

    ScannerReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getProjectVersion()).isEqualTo(expected);
  }

  static Stream<Arguments> projectVersions() {
    String version = secure().nextAlphabetic(15);
    return Stream.of(
      Arguments.of(null, ""),
      Arguments.of("", ""),
      Arguments.of("5.6.3", "5.6.3"),
      Arguments.of(version, version)
    );
  }

  @ParameterizedTest
  @MethodSource("buildStrings")
  void write_buildString(@Nullable String buildString, String expected) {
    when(projectInfo.getBuildString()).thenReturn(Optional.ofNullable(buildString));

    underTest.publish(writer);

    ScannerReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getBuildString()).isEqualTo(expected);
  }

  static Stream<Arguments> buildStrings() {
    String randomBuildString = secure().nextAlphabetic(15);
    return Stream.of(
      Arguments.of(null, ""),
      Arguments.of("", ""),
      Arguments.of("5.6.3", "5.6.3"),
      Arguments.of(randomBuildString, randomBuildString)
    );
  }

  @Test
  void write_branch_info() {
    String branchName = "name";
    String targetName = "target";

    when(branches.branchName()).thenReturn(branchName);
    when(branches.branchType()).thenReturn(BranchType.BRANCH);
    when(branches.targetBranchName()).thenReturn(targetName);

    underTest.publish(writer);

    ScannerReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getBranchName()).isEqualTo(branchName);
    assertThat(metadata.getBranchType()).isEqualTo(ScannerReport.Metadata.BranchType.BRANCH);
    assertThat(metadata.getReferenceBranchName()).isEmpty();
    assertThat(metadata.getTargetBranchName()).isEqualTo(targetName);
  }

  @Test
  void dont_write_new_code_reference_if_not_specified_in_properties() {
    when(referenceBranchSupplier.get()).thenReturn("ref");
    when(referenceBranchSupplier.getFromProperties()).thenReturn(null);

    underTest.publish(writer);

    ScannerReport.Metadata metadata = reader.readMetadata();

    assertThat(metadata.getNewCodeReferenceBranch()).isEmpty();
  }

  @Test
  void write_project_basedir() {
    String path = "some/dir";
    Path relativePathFromScmRoot = Paths.get(path);
    when(scmProvider.relativePathFromScmRoot(any(Path.class))).thenReturn(relativePathFromScmRoot);

    underTest.publish(writer);

    ScannerReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getRelativePathFromScmRoot()).isEqualTo(path);
  }

  @Test
  void write_revision_id() {
    String revisionId = "some-sha1";
    when(scmRevision.get()).thenReturn(Optional.of(revisionId));

    underTest.publish(writer);

    ScannerReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getScmRevisionId()).isEqualTo(revisionId);
  }

  @Test
  void should_not_crash_when_scm_provider_does_not_support_relativePathFromScmRoot() {
    ScmProvider fakeScmProvider = new ScmProvider() {
      @Override
      public String key() {
        return "foo";
      }
    };
    when(scmConfiguration.provider()).thenReturn(fakeScmProvider);

    underTest.publish(writer);

    ScannerReport.Metadata metadata = reader.readMetadata();
    assertThat(metadata.getRelativePathFromScmRoot()).isEmpty();
  }
}
