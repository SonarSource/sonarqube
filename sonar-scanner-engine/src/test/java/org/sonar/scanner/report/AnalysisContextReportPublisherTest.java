/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.core.platform.PluginInfo;
import org.sonar.scanner.bootstrap.GlobalConfiguration;
import org.sonar.scanner.bootstrap.ScannerPluginRepository;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.repository.ProjectRepositories;
import org.sonar.updatecenter.common.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class AnalysisContextReportPublisherTest {

  private static final String BIZ = "BIZ";
  private static final String FOO = "FOO";
  private static final String SONAR_SKIP = "sonar.skip";
  private static final String COM_FOO = "com.foo";

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private ScannerPluginRepository pluginRepo = mock(ScannerPluginRepository.class);
  private AnalysisContextReportPublisher publisher;
  private AnalysisMode analysisMode = mock(AnalysisMode.class);
  private System2 system2;
  private ProjectRepositories projectRepos;
  private GlobalConfiguration globalSettings;
  private InputModuleHierarchy hierarchy;

  @Before
  public void prepare() throws Exception {
    logTester.setLevel(LoggerLevel.INFO);
    system2 = mock(System2.class);
    when(system2.properties()).thenReturn(new Properties());
    projectRepos = mock(ProjectRepositories.class);
    globalSettings = mock(GlobalConfiguration.class);
    hierarchy = mock(InputModuleHierarchy.class);
    publisher = new AnalysisContextReportPublisher(analysisMode, pluginRepo, system2, projectRepos, globalSettings, hierarchy);
  }

  @Test
  public void shouldOnlyDumpPluginsByDefault() throws Exception {
    when(pluginRepo.getPluginInfos()).thenReturn(Arrays.asList(new PluginInfo("xoo").setName("Xoo").setVersion(Version.create("1.0"))));

    ScannerReportWriter writer = new ScannerReportWriter(temp.newFolder());
    publisher.init(writer);

    assertThat(writer.getFileStructure().analysisLog()).exists();
    assertThat(FileUtils.readFileToString(writer.getFileStructure().analysisLog())).contains("Xoo 1.0 (xoo)");

    verifyZeroInteractions(system2);
  }

  @Test
  public void shouldNotDumpInIssuesMode() throws Exception {
    when(analysisMode.isIssues()).thenReturn(true);

    ScannerReportWriter writer = new ScannerReportWriter(temp.newFolder());
    publisher.init(writer);
    publisher.dumpModuleSettings(new DefaultInputModule(ProjectDefinition.create().setKey("foo").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder())));

    assertThat(writer.getFileStructure().analysisLog()).doesNotExist();
  }

  @Test
  public void dumpServerSideGlobalProps() throws Exception {
    logTester.setLevel(LoggerLevel.DEBUG);
    ScannerReportWriter writer = new ScannerReportWriter(temp.newFolder());
    when(globalSettings.getServerSideSettings()).thenReturn(ImmutableMap.of(COM_FOO, "bar", SONAR_SKIP, "true"));

    publisher.init(writer);

    String content = FileUtils.readFileToString(writer.getFileStructure().analysisLog());
    assertThat(content).containsOnlyOnce(COM_FOO);
    assertThat(content).containsOnlyOnce(SONAR_SKIP);
  }

  @Test
  public void dumpServerSideModuleProps() throws Exception {
    logTester.setLevel(LoggerLevel.DEBUG);
    ScannerReportWriter writer = new ScannerReportWriter(temp.newFolder());
    publisher.init(writer);

    when(projectRepos.moduleExists("foo")).thenReturn(true);
    when(projectRepos.settings("foo")).thenReturn(ImmutableMap.of(COM_FOO, "bar", SONAR_SKIP, "true"));

    publisher.dumpModuleSettings(new DefaultInputModule(ProjectDefinition.create().setKey("foo").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder())));

    String content = FileUtils.readFileToString(writer.getFileStructure().analysisLog());
    assertThat(content).doesNotContain(COM_FOO);
    assertThat(content).containsOnlyOnce(SONAR_SKIP);
  }

  @Test
  public void shouldNotDumpSQPropsInSystemProps() throws Exception {
    logTester.setLevel(LoggerLevel.DEBUG);
    ScannerReportWriter writer = new ScannerReportWriter(temp.newFolder());
    Properties props = new Properties();
    props.setProperty(COM_FOO, "bar");
    props.setProperty(SONAR_SKIP, "true");
    when(system2.properties()).thenReturn(props);
    publisher.init(writer);

    String content = FileUtils.readFileToString(writer.getFileStructure().analysisLog());
    assertThat(content).containsOnlyOnce(COM_FOO);
    assertThat(content).doesNotContain(SONAR_SKIP);

    publisher.dumpModuleSettings(new DefaultInputModule(ProjectDefinition.create()
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder())
      .setProperty("sonar.projectKey", "foo")
      .setProperty(COM_FOO, "bar")
      .setProperty(SONAR_SKIP, "true")));

    content = FileUtils.readFileToString(writer.getFileStructure().analysisLog());
    assertThat(content).containsOnlyOnce(COM_FOO);
    assertThat(content).containsOnlyOnce(SONAR_SKIP);
  }

  @Test
  public void shouldNotDumpEnvTwice() throws Exception {
    logTester.setLevel(LoggerLevel.DEBUG);
    ScannerReportWriter writer = new ScannerReportWriter(temp.newFolder());

    Map<String, String> env = new HashMap<>();
    env.put(FOO, "BAR");
    env.put(BIZ, "BAZ");
    when(system2.envVariables()).thenReturn(env);
    publisher.init(writer);

    String content = FileUtils.readFileToString(writer.getFileStructure().analysisLog());
    assertThat(content).containsOnlyOnce(FOO);
    assertThat(content).containsOnlyOnce(BIZ);
    assertThat(content).containsSubsequence(BIZ, FOO);

    publisher.dumpModuleSettings(new DefaultInputModule(ProjectDefinition.create()
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder())
      .setProperty("sonar.projectKey", "foo")
      .setProperty("env." + FOO, "BAR")));

    content = FileUtils.readFileToString(writer.getFileStructure().analysisLog());
    assertThat(content).containsOnlyOnce(FOO);
    assertThat(content).containsOnlyOnce(BIZ);
    assertThat(content).doesNotContain("env." + FOO);
  }

  @Test
  public void shouldNotDumpSensitiveModuleProperties() throws Exception {
    ScannerReportWriter writer = new ScannerReportWriter(temp.newFolder());
    publisher.init(writer);

    assertThat(writer.getFileStructure().analysisLog()).exists();

    publisher.dumpModuleSettings(new DefaultInputModule(ProjectDefinition.create()
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder())
      .setProperty("sonar.projectKey", "foo")
      .setProperty("sonar.projectKey", "foo")
      .setProperty("sonar.login", "my_token")
      .setProperty("sonar.password", "azerty")
      .setProperty("sonar.cpp.license.secured", "AZERTY")));

    assertThat(FileUtils.readFileToString(writer.getFileStructure().analysisLog())).containsSubsequence(
      "sonar.cpp.license.secured=******",
      "sonar.login=******",
      "sonar.password=******",
      "sonar.projectKey=foo");
  }

  // SONAR-7598
  @Test
  public void shouldNotDumpSensitiveGlobalProperties() throws Exception {
    ScannerReportWriter writer = new ScannerReportWriter(temp.newFolder());
    when(globalSettings.getServerSideSettings()).thenReturn(ImmutableMap.of("sonar.login", "my_token", "sonar.password", "azerty", "sonar.cpp.license.secured", "AZERTY"));

    publisher.init(writer);

    assertThat(FileUtils.readFileToString(writer.getFileStructure().analysisLog())).containsSubsequence(
      "sonar.cpp.license.secured=******",
      "sonar.login=******",
      "sonar.password=******");
  }

  // SONAR-7371
  @Test
  public void dontDumpParentProps() throws Exception {
    logTester.setLevel(LoggerLevel.DEBUG);
    ScannerReportWriter writer = new ScannerReportWriter(temp.newFolder());
    publisher.init(writer);

    DefaultInputModule module = new DefaultInputModule(ProjectDefinition.create()
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder())
      .setProperty("sonar.projectKey", "foo")
      .setProperty(SONAR_SKIP, "true"));

    DefaultInputModule parent = new DefaultInputModule(ProjectDefinition.create()
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder())
      .setProperty("sonar.projectKey", "parent")
      .setProperty(SONAR_SKIP, "true"));

    when(hierarchy.parent(module)).thenReturn(parent);

    publisher.dumpModuleSettings(module);

    String content = FileUtils.readFileToString(writer.getFileStructure().analysisLog());
    assertThat(content).doesNotContain(SONAR_SKIP);
  }
}
