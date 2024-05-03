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
package org.sonar.server.qualitygate;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualitygate.QualityGateDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class QualityGateUpdaterIT {

  static final String QGATE_NAME = "Default";

  @Rule
  public DbTester db = DbTester.create();

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final QualityGateUpdater underTest = new QualityGateUpdater(dbClient);

  @Test
  public void create_quality_gate() {
    QualityGateDto result = underTest.create(dbSession, QGATE_NAME);

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo(QGATE_NAME);
    assertThat(result.getCreatedAt()).isNotNull();
    assertThat(result.isBuiltIn()).isFalse();
    QualityGateDto reloaded = dbClient.qualityGateDao().selectByName(dbSession, QGATE_NAME);
    assertThat(reloaded).isNotNull();
  }

  @Test
  public void fail_to_create_when_name_already_exists() {
    underTest.create(dbSession, QGATE_NAME);

    assertThatThrownBy(() -> underTest.create(dbSession, QGATE_NAME))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Name has already been taken");
  }
}
