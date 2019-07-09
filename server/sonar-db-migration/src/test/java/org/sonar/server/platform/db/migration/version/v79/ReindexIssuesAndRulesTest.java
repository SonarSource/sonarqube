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
package org.sonar.server.platform.db.migration.version.v79;

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.es.MigrationEsClient;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class ReindexIssuesAndRulesTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createEmpty();

  private MapSettings settings = new MapSettings();
  private MigrationEsClient esClient = mock(MigrationEsClient.class);

  private DataChange underTest = new ReindexIssuesAndRules(db.database(), settings.asConfig(), esClient);

  @Test
  public void update_es_indexes() throws SQLException {
    settings.setProperty("sonar.sonarcloud.enabled", "false");

    underTest.execute();

    verify(esClient).deleteIndexes("issues", "rules");
  }

  @Test
  public void do_nothing_on_sonarcloud() throws SQLException {
    settings.setProperty("sonar.sonarcloud.enabled", "true");

    underTest.execute();

    verifyZeroInteractions(esClient);
  }
}
