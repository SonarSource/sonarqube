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
package org.sonar.server.platform.monitoring.cluster;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.es.EsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.systeminfo.SystemInfoUtils.attribute;

public class EsClusterStateSectionTest {

  @Rule
  public EsTester es = EsTester.create();

  private EsClusterStateSection underTest = new EsClusterStateSection(es.client());

  @Test
  public void test_name() {
    assertThat(underTest.toProtobuf().getName()).isEqualTo("Search State");
  }

  @Test
  public void test_attributes() {
    ProtobufSystemInfo.Section section = underTest.toProtobuf();
    assertThat(attribute(section, "Nodes").getLongValue()).isGreaterThan(0);
    assertThat(attribute(section, "State").getStringValue()).isIn("RED", "YELLOW", "GREEN");
  }
}
