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
package org.sonar.server.platform.monitoring;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.process.systeminfo.SystemInfoUtils.attribute;

public class DbConnectionSectionTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DatabaseVersion databaseVersion = mock(DatabaseVersion.class);
  private SonarRuntime runtime = mock(SonarRuntime.class);
  private DbConnectionSection underTest = new DbConnectionSection(databaseVersion, dbTester.getDbClient(), runtime);

  @Test
  public void jmx_name_is_not_empty() {
    assertThat(underTest.name()).isEqualTo("Database");
  }

  @Test
  public void pool_info() {
    ProtobufSystemInfo.Section section = underTest.toProtobuf();
    assertThat(attribute(section, "Pool Max Connections").getLongValue()).isGreaterThan(0L);
    assertThat(attribute(section, "Pool Idle Connections").getLongValue()).isGreaterThanOrEqualTo(0L);
    assertThat(attribute(section, "Pool Min Idle Connections").getLongValue()).isGreaterThanOrEqualTo(0L);
    assertThat(attribute(section, "Pool Max Idle Connections").getLongValue()).isGreaterThanOrEqualTo(0L);
    assertThat(attribute(section, "Pool Max Wait (ms)")).isNotNull();
    assertThat(attribute(section, "Pool Remove Abandoned")).isNotNull();
    assertThat(attribute(section, "Pool Remove Abandoned Timeout (seconds)").getLongValue()).isGreaterThanOrEqualTo(0L);
  }

  @Test
  public void section_name_depends_on_runtime_side() {
    when(runtime.getSonarQubeSide()).thenReturn(SonarQubeSide.COMPUTE_ENGINE);
    assertThat(underTest.toProtobuf().getName()).isEqualTo("Compute Engine Database Connection");

    when(runtime.getSonarQubeSide()).thenReturn(SonarQubeSide.SERVER );
    assertThat(underTest.toProtobuf().getName()).isEqualTo("Web Database Connection");
  }
}
