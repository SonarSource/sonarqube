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
package org.sonar.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;

import static org.assertj.core.api.Assertions.assertThat;

class IsAliveMapperIT {

  @RegisterExtension
  private final DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbSession session;
  private IsAliveMapper underTest;

  @BeforeEach
  void setUp() {
    session = dbTester.getSession();
    underTest = session.getMapper(IsAliveMapper.class);
  }

  @AfterEach
  void tearDown() {
    session.close();
  }

  @Test
  void isAlive_works_for_current_vendors() {
    assertThat(underTest.isAlive()).isEqualTo(IsAliveMapper.IS_ALIVE_RETURNED_VALUE);
  }
}
