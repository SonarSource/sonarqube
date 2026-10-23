/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class DbClientTest {

  @Test
  void constructor_with_all_registered_daos_succeeds() {
    Dao[] daos = DaoModule.classes().stream()
      .map(daoClass -> (Dao) mock(daoClass))
      .toArray(Dao[]::new);

    DbClient dbClient = new DbClient(mock(MyBatis.class), mock(DBSessions.class), daos);

    assertThat(dbClient).isNotNull();
  }

  @Test
  void constructor_without_required_daos_throws() {
    assertThatThrownBy(() -> new DbClient(mock(MyBatis.class), mock(DBSessions.class)))
      .isInstanceOf(IllegalArgumentException.class);
  }
}
