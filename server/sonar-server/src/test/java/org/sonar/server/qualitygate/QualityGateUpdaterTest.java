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
package org.sonar.server.qualitygate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QualityGateDto;

public class QualityGateUpdaterTest {

  static final String QGATE_NAME = "Default";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private QualityGateUpdater underTest = new QualityGateUpdater(dbClient, UuidFactoryFast.getInstance());

  @Test
  public void create_quality_gate() {
    OrganizationDto organization = db.organizations().insert();

    QualityGateDto result = underTest.create(dbSession, organization, QGATE_NAME);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo(QGATE_NAME);
    assertThat(result.getCreatedAt()).isNotNull();
    assertThat(result.isBuiltIn()).isFalse();
    QualityGateDto reloaded = dbClient.qualityGateDao().selectByName(dbSession, QGATE_NAME);
    assertThat(reloaded).isNotNull();
  }

  @Test
  public void fail_to_create_when_name_already_exists() {
    OrganizationDto org = db.organizations().insert();
    underTest.create(dbSession, org, QGATE_NAME);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Name has already been taken");

    underTest.create(dbSession, org, QGATE_NAME);
  }
}
