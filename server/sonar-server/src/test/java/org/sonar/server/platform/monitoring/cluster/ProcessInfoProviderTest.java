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
package org.sonar.server.platform.monitoring.cluster;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static org.assertj.core.api.Assertions.assertThat;

public class ProcessInfoProviderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void remove_global_sections_from_results() {
    ProcessInfoProvider underTest = new ProcessInfoProvider(new SystemInfoSection[]{
      new TestGlobalSystemInfoSection("foo"),
      new TestSystemInfoSection("bar")});

    underTest.start();
    List<ProtobufSystemInfo.Section> sections = ProcessInfoProvider.provide().getSectionsList();
    assertThat(sections).extracting(ProtobufSystemInfo.Section::getName).containsExactly("bar");

    underTest.stop();
  }

  @Test
  public void merge_sections() {
    ProcessInfoProvider underTest = new ProcessInfoProvider(new SystemInfoSection[]{
      new TestSystemInfoSection("foo"),
      new TestSystemInfoSection("bar")});

    underTest.start();
    List<ProtobufSystemInfo.Section> sections = ProcessInfoProvider.provide().getSectionsList();
    assertThat(sections).extracting(ProtobufSystemInfo.Section::getName)
      .containsExactlyInAnyOrder("foo", "bar");

    underTest.stop();
  }

  @Test
  public void empty_result_is_returned_if_not_started_yet() {
    ProcessInfoProvider underTest = new ProcessInfoProvider(new SystemInfoSection[]{
      new TestSystemInfoSection("foo"),
      new TestSystemInfoSection("bar")});

    assertThat(ProcessInfoProvider.provide().getSectionsCount()).isEqualTo(0);

    underTest.start();
    assertThat(ProcessInfoProvider.provide().getSectionsCount()).isEqualTo(2);

    underTest.stop();
    assertThat(ProcessInfoProvider.provide().getSectionsCount()).isEqualTo(0);
  }
}
