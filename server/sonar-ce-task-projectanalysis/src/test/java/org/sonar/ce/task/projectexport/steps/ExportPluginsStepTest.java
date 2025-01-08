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
package org.sonar.ce.task.projectexport.steps;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.updatecenter.common.Version;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExportPluginsStepTest {

  @Rule
  public LogTester logTester = new LogTester();

  private PluginRepository pluginRepository = mock(PluginRepository.class);
  private FakeDumpWriter dumpWriter = new FakeDumpWriter();
  ExportPluginsStep underTest = new ExportPluginsStep(pluginRepository, dumpWriter);

  @Before
  public void before() {
    logTester.setLevel(Level.DEBUG);
  }

  @Test
  public void export_plugins() {
    when(pluginRepository.getPluginInfos()).thenReturn(Arrays.asList(
      new PluginInfo("java"), new PluginInfo("cs")));

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.Plugin> exportedPlugins = dumpWriter.getWrittenMessagesOf(DumpElement.PLUGINS);
    assertThat(exportedPlugins).hasSize(2);
    assertThat(exportedPlugins).extracting(ProjectDump.Plugin::getKey).containsExactlyInAnyOrder("java", "cs");
    assertThat(logTester.logs(Level.DEBUG)).contains("2 plugins exported");
  }

  @Test
  public void export_zero_plugins() {
    when(pluginRepository.getPluginInfos()).thenReturn(Collections.emptyList());

    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.PLUGINS)).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).contains("0 plugins exported");
  }

  @Test
  public void test_exported_fields() {
    when(pluginRepository.getPluginInfos()).thenReturn(singletonList(
      new PluginInfo("java").setName("Java").setVersion(Version.create("1.2.3"))));

    underTest.execute(new TestComputationStepContext());

    ProjectDump.Plugin exportedPlugin = dumpWriter.getWrittenMessagesOf(DumpElement.PLUGINS).get(0);
    assertThat(exportedPlugin.getKey()).isEqualTo("java");
    assertThat(exportedPlugin.getName()).isEqualTo("Java");
    assertThat(exportedPlugin.getVersion()).isEqualTo("1.2.3");
  }

  @Test
  public void test_nullable_exported_fields() {
    when(pluginRepository.getPluginInfos()).thenReturn(singletonList(
      new PluginInfo("java")));

    underTest.execute(new TestComputationStepContext());

    ProjectDump.Plugin exportedPlugin = dumpWriter.getWrittenMessagesOf(DumpElement.PLUGINS).get(0);
    assertThat(exportedPlugin.getKey()).isEqualTo("java");
    // if name is not set, then value is the same as key
    assertThat(exportedPlugin.getName()).isEqualTo("java");
    assertThat(exportedPlugin.getVersion()).isEmpty();
  }

  @Test
  public void test_getDescription() {
    assertThat(underTest.getDescription()).isEqualTo("Export plugins");
  }
}
