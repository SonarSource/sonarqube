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
package org.sonar.batch.report;

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
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.batch.bootstrap.BatchPluginRepository;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.platform.PluginInfo;
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

  private BatchPluginRepository pluginRepo = mock(BatchPluginRepository.class);
  private AnalysisContextReportPublisher publisher;
  private AnalysisMode analysisMode = mock(AnalysisMode.class);
  private System2 system2;

  @Before
  public void prepare() throws Exception {
    logTester.setLevel(LoggerLevel.INFO);
    system2 = mock(System2.class);
    when(system2.properties()).thenReturn(new Properties());
    publisher = new AnalysisContextReportPublisher(analysisMode, pluginRepo, system2);
  }

  @Test
  public void shouldOnlyDumpPluginsByDefault() throws Exception {
    when(pluginRepo.getPluginInfos()).thenReturn(Arrays.asList(new PluginInfo("xoo").setName("Xoo").setVersion(Version.create("1.0"))));

    BatchReportWriter writer = new BatchReportWriter(temp.newFolder());
    publisher.init(writer);

    assertThat(writer.getFileStructure().analysisLog()).exists();
    assertThat(FileUtils.readFileToString(writer.getFileStructure().analysisLog())).contains("Xoo 1.0 (xoo)");

    verifyZeroInteractions(system2);
  }

  @Test
  public void shouldNotDumpInIssuesMode() throws Exception {
    when(analysisMode.isIssues()).thenReturn(true);

    BatchReportWriter writer = new BatchReportWriter(temp.newFolder());
    publisher.init(writer);
    publisher.dumpSettings(ProjectDefinition.create().setProperty("sonar.projectKey", "foo"), new Settings());

    assertThat(writer.getFileStructure().analysisLog()).doesNotExist();
  }

  @Test
  public void shouldNotDumpSQPropsInSystemProps() throws Exception {
    logTester.setLevel(LoggerLevel.DEBUG);
    BatchReportWriter writer = new BatchReportWriter(temp.newFolder());
    Properties props = new Properties();
    props.setProperty(COM_FOO, "bar");
    props.setProperty(SONAR_SKIP, "true");
    when(system2.properties()).thenReturn(props);
    publisher.init(writer);

    String content = FileUtils.readFileToString(writer.getFileStructure().analysisLog());
    assertThat(content).containsOnlyOnce(COM_FOO);
    assertThat(content).doesNotContain(SONAR_SKIP);

    Settings settings = new Settings();
    settings.setProperty(COM_FOO, "bar");
    settings.setProperty(SONAR_SKIP, "true");

    publisher.dumpSettings(ProjectDefinition.create().setProperty("sonar.projectKey", "foo"), settings);

    content = FileUtils.readFileToString(writer.getFileStructure().analysisLog());
    assertThat(content).containsOnlyOnce(COM_FOO);
    assertThat(content).containsOnlyOnce(SONAR_SKIP);
  }

  @Test
  public void shouldNotDumpEnvTwice() throws Exception {
    logTester.setLevel(LoggerLevel.DEBUG);
    BatchReportWriter writer = new BatchReportWriter(temp.newFolder());

    Map<String, String> env = new HashMap<>();
    env.put(FOO, "BAR");
    env.put(BIZ, "BAZ");
    when(system2.envVariables()).thenReturn(env);
    publisher.init(writer);

    String content = FileUtils.readFileToString(writer.getFileStructure().analysisLog());
    assertThat(content).containsOnlyOnce(FOO);
    assertThat(content).containsOnlyOnce(BIZ);
    assertThat(content).containsSequence(BIZ, FOO);

    Settings settings = new Settings();
    settings.setProperty("env." + FOO, "BAR");

    publisher.dumpSettings(ProjectDefinition.create().setProperty("sonar.projectKey", "foo"), settings);

    content = FileUtils.readFileToString(writer.getFileStructure().analysisLog());
    assertThat(content).containsOnlyOnce(FOO);
    assertThat(content).containsOnlyOnce(BIZ);
    assertThat(content).doesNotContain("env." + FOO);
  }

  @Test
  public void shouldNotDumpSensitiveProperties() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(temp.newFolder());
    publisher.init(writer);

    assertThat(writer.getFileStructure().analysisLog()).exists();

    Settings settings = new Settings();
    settings.setProperty("sonar.projectKey", "foo");
    settings.setProperty("sonar.password", "azerty");
    settings.setProperty("sonar.cpp.license.secured", "AZERTY");
    publisher.dumpSettings(ProjectDefinition.create().setProperty("sonar.projectKey", "foo"), settings);

    assertThat(FileUtils.readFileToString(writer.getFileStructure().analysisLog())).containsSequence(
      "sonar.cpp.license.secured=******",
      "sonar.password=******",
      "sonar.projectKey=foo");
  }
}
