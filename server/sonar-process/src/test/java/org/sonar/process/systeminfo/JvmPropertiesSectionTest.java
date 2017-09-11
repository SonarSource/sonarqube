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
package org.sonar.process.systeminfo;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.systeminfo.SystemInfoUtils.attribute;

public class JvmPropertiesSectionTest {

  private JvmPropertiesSection underTest = new JvmPropertiesSection("Web JVM Properties");

  @Test
  public void name_is_not_empty() {
    assertThat(underTest.toProtobuf().getName()).isEqualTo("Web JVM Properties");
  }

  @Test
  public void test_toProtobuf() {
    ProtobufSystemInfo.Section section = underTest.toProtobuf();

    Assertions.assertThat(attribute(section, "java.vm.vendor").getStringValue()).isNotEmpty();
    Assertions.assertThat(attribute(section, "os.name").getStringValue()).isNotEmpty();
  }
}
