/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo.Section;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseMBeanIT {

  @RegisterExtension
  private final DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final FakeDatabaseMBean underTest = new FakeDatabaseMBean(dbTester.getDbClient());

  @Test
  void verify_mBean() {

    assertThat(underTest.getPoolActiveConnections()).isNotNegative();
    assertThat(underTest.getPoolIdleConnections()).isNotNegative();
    assertThat(underTest.getPoolMaxConnections()).isNotNegative();
    assertThat(underTest.getPoolMaxLifeTimeMillis()).isNotNegative();
    assertThat(underTest.getPoolMinIdleConnections()).isNotNegative();
    assertThat(underTest.getPoolTotalConnections()).isNotNegative();
    assertThat(underTest.getPoolMaxWaitMillis()).isNotNegative();
    assertThat(underTest.name()).isEqualTo("FakeMBean");
  }

  private static class FakeDatabaseMBean extends DatabaseMBean {

    protected FakeDatabaseMBean(DbClient dbClient) {
      super(dbClient);
    }

    @Override
    protected String name() {
      return "FakeMBean";
    }

    @Override
    public Section toProtobuf() {
      return Section.newBuilder().build();
    }
  }
}
