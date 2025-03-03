/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.scanner.sca;

import java.io.File;
import java.io.IOException;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.event.Level;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.scanner.config.DefaultConfiguration;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.repository.featureflags.FeatureFlagsRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ScaExecutorTest {
  private final CliService cliService = mock(CliService.class);
  private final CliCacheService cliCacheService = mock(CliCacheService.class);
  private final ReportPublisher reportPublisher = mock(ReportPublisher.class);
  private final FeatureFlagsRepository featureFlagsRepository = mock(FeatureFlagsRepository.class);
  private final DefaultConfiguration configuration = mock(DefaultConfiguration.class);
  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();
  private final ScaExecutor underTest = new ScaExecutor(cliCacheService, cliService, reportPublisher, featureFlagsRepository, configuration);
  @TempDir
  File rootModuleDir;
  private DefaultInputModule root;

  @BeforeEach
  void before() {
    when(featureFlagsRepository.isEnabled("sca")).thenReturn(true);
    root = new DefaultInputModule(
      ProjectDefinition.create().setBaseDir(rootModuleDir).setWorkDir(rootModuleDir.toPath().getRoot().toFile()));
  }

  @Test
  void execute_shouldSkipAnalysisWhenFeatureFlagDisabled() {
    when(featureFlagsRepository.isEnabled("sca")).thenReturn(false);
    logTester.setLevel(Level.DEBUG);

    underTest.execute(root);

    assertThat(logTester.logs()).contains("Dependency analysis skipped");
    verifyNoInteractions(cliService, cliCacheService);
  }

  @Test
  void execute_shouldCallCliAndPublisher() throws IOException {
    File mockCliFile = Files.newTemporaryFile();
    File mockManifestZip = Files.newTemporaryFile();
    ScannerReportWriter mockReportWriter = mock(ScannerReportWriter.class);
    when(cliCacheService.cacheCli()).thenReturn(mockCliFile);
    when(cliService.generateManifestsZip(root, mockCliFile, configuration)).thenReturn(mockManifestZip);
    when(reportPublisher.getWriter()).thenReturn(mockReportWriter);

    logTester.setLevel(Level.DEBUG);

    underTest.execute(root);

    verify(cliService).generateManifestsZip(root, mockCliFile, configuration);
    verify(mockReportWriter).writeScaFile(mockManifestZip);
    assertThat(logTester.logs(Level.DEBUG)).contains("Zip ready for report: " + mockManifestZip);
    assertThat(logTester.logs(Level.DEBUG)).contains("Manifest zip written to report");
  }

  @Test
  void execute_whenIOException_shouldHandleException() throws IOException {
    File mockCliFile = Files.newTemporaryFile();
    when(cliCacheService.cacheCli()).thenReturn(mockCliFile);
    doThrow(IOException.class).when(cliService).generateManifestsZip(root, mockCliFile, configuration);

    logTester.setLevel(Level.INFO);

    underTest.execute(root);

    verify(cliService).generateManifestsZip(root, mockCliFile, configuration);
    assertThat(logTester.logs(Level.ERROR)).contains("Error gathering manifests");
  }

  @Test
  void execute_whenIllegalStateException_shouldHandleException() throws IOException {
    File mockCliFile = Files.newTemporaryFile();
    when(cliCacheService.cacheCli()).thenReturn(mockCliFile);
    doThrow(IllegalStateException.class).when(cliService).generateManifestsZip(root, mockCliFile, configuration);

    logTester.setLevel(Level.INFO);

    underTest.execute(root);

    verify(cliService).generateManifestsZip(root, mockCliFile, configuration);
    assertThat(logTester.logs(Level.ERROR)).contains("Error gathering manifests");
  }

  @Test
  void execute_whenNoCliFound_shouldSkipAnalysis() throws IOException {
    File mockCliFile = new File("");
    when(cliCacheService.cacheCli()).thenReturn(mockCliFile);

    underTest.execute(root);

    verify(cliService, never()).generateManifestsZip(root, mockCliFile, configuration);
  }
}
