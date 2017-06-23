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
package org.sonarqube.tests.organization;

import com.sonar.orchestrator.Orchestrator;
import org.sonarqube.tests.Category4Suite;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Tester;

import static util.ItUtils.expectForbiddenError;

public class RootUserTest {

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Test
  public void nobody_is_root_by_default_when_organizations_are_disabled() {
    // anonymous
    expectForbiddenError(() -> tester.wsClient().roots().search());

    // admin
    expectForbiddenError(() -> tester.wsClient().roots().search());
  }
}
