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
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.batch.bootstrap.BatchPluginRepository;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.platform.PluginInfo;
import org.sonar.updatecenter.common.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AnalysisContextReportPublisherTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private BatchPluginRepository pluginRepo = mock(BatchPluginRepository.class);
  private AnalysisContextReportPublisher publisher;
  private AnalysisMode analysisMode = mock(AnalysisMode.class);

  @Before
  public void prepare() throws Exception {
    publisher = new AnalysisContextReportPublisher(analysisMode, pluginRepo);
  }

  @Test
  public void shouldDumpPlugins() throws Exception {
    when(pluginRepo.getPluginInfos()).thenReturn(Arrays.asList(new PluginInfo("xoo").setName("Xoo").setVersion(Version.create("1.0"))));

    BatchReportWriter writer = new BatchReportWriter(temp.newFolder());
    publisher.init(writer);

    assertThat(writer.getFileStructure().analysisLog()).exists();
    assertThat(FileUtils.readFileToString(writer.getFileStructure().analysisLog())).contains("Xoo 1.0 (xoo)");
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
  public void shouldNotDumpSensitiveProperties() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(temp.newFolder());
    publisher.init(writer);

    assertThat(writer.getFileStructure().analysisLog()).exists();

    Settings settings = new Settings();
    settings.setProperty("sonar.projectKey", "foo");
    settings.setProperty("sonar.password", "azerty");
    settings.setProperty("sonar.cpp.license.secured", "AZERTY");
    publisher.dumpSettings(ProjectDefinition.create().setProperty("sonar.projectKey", "foo"), settings);

    assertThat(FileUtils.readFileToString(writer.getFileStructure().analysisLog())).contains("sonar.projectKey=foo", "sonar.password=******", "sonar.cpp.license.secured=******");
  }
}
