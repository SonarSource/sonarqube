/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.exceptions.BadRequestException;

import static java.lang.String.format;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class QualityGateUpdaterTest {

  static final String QGATE_NAME = "Default";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  DbClient dbClient = db.getDbClient();
  DbSession dbSession= db.getSession();

  QualityGateUpdater underTest = new QualityGateUpdater(dbClient);

  @Test
  public void create_quality_gate() throws Exception {
    QualityGateDto result = underTest.create(dbSession, QGATE_NAME);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo(QGATE_NAME);
    assertThat(result.getCreatedAt()).isNotNull();
    QualityGateDto reloaded = dbClient.qualityGateDao().selectByName(dbSession, QGATE_NAME);
    assertThat(reloaded).isNotNull();
  }

  @Test
  public void fail_to_create_when_name_is_empty() throws Exception {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage(format("errors.cant_be_empty", "Name"));
    underTest.create(dbSession, "");
  }

  @Test
  public void fail_to_create_when_name_already_exists() throws Exception {
    dbClient.qualityGateDao().insert(new QualityGateDto().setName(QGATE_NAME));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("errors.is_already_used");
    underTest.create(dbSession, QGATE_NAME);
  }
}
