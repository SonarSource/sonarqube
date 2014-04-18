/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.debt;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import static org.fest.assertions.Assertions.assertThat;

public class DebtMediumTest {

  ServerTester serverTester = new ServerTester();

  @Before
  public void before() throws Exception {
    serverTester.start();
  }

  @After
  public void after() throws Exception {
    serverTester.stop();
  }

  @Test
  public void find_characteristics() throws Exception {
    DebtModelService debtModelService = serverTester.get(DebtModelService.class);

    // Only root characteristics
    assertThat(debtModelService.characteristics()).hasSize(8);

    // Characteristics and sub-characteristics
    assertThat(debtModelService.allCharacteristics()).hasSize(39);
  }

  @Test
  public void create_characteristic() throws Exception {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    DebtModelService debtModelService = serverTester.get(DebtModelService.class);
    DebtCharacteristic result = debtModelService.create("New characteristic", null);

    assertThat(debtModelService.characteristicByKey(result.key())).isNotNull();
  }

}
