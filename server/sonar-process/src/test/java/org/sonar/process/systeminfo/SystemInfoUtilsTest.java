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
package org.sonar.process.systeminfo;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo.Section;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class SystemInfoUtilsTest {

  @Test
  public void test_setAttribute_with_boolean_parameter() {
    Section.Builder builder = Section.newBuilder();
    SystemInfoUtils.setAttribute(builder, "isNull", (Boolean)null);
    SystemInfoUtils.setAttribute(builder, "isTrue", true);
    SystemInfoUtils.setAttribute(builder, "isFalse", false);

    Section section = builder.build();
    assertThat(SystemInfoUtils.attribute(section, "isNull")).isNull();
    assertThat(SystemInfoUtils.attribute(section, "isTrue").getBooleanValue()).isTrue();
    assertThat(SystemInfoUtils.attribute(section, "isFalse").getBooleanValue()).isFalse();
  }

  @Test
  public void test_order() {
    Collection<Section> sections = asList(
      newSection("end2"),
      newSection("bar"),
      newSection("end1"),
      newSection("foo"));

    List<String> ordered = SystemInfoUtils.order(sections, "foo", "bar").stream()
      .map(Section::getName)
      .collect(Collectors.toList());
    assertThat(ordered).isEqualTo(asList("foo", "bar", "end1", "end2"));
  }

  private static Section newSection(String name) {
    return Section.newBuilder().setName(name).build();
  }
}
