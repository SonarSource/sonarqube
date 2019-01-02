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
package org.sonar.server.platform.monitoring;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.systeminfo.SystemInfoUtils.attribute;

public class DbSectionTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbSection underTest = new DbSection(dbTester.getDbClient());

  @Test
  public void db_info() {
    ProtobufSystemInfo.Section section = underTest.toProtobuf();
    SystemInfoTesting.assertThatAttributeIs(section, "Database", "H2");
    assertThat(attribute(section, "Database Version").getStringValue()).startsWith("1.");
    SystemInfoTesting.assertThatAttributeIs(section, "Username", "SONAR");
    assertThat(attribute(section, "Driver Version").getStringValue()).startsWith("1.");
  }
}
