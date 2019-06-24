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
package org.sonar.server.platform.db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.SonarRuntime;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.H2Database;
import org.sonar.db.dialect.H2;
import org.sonar.server.platform.DefaultServerUpgradeStatus;
import org.sonar.server.platform.db.migration.engine.MigrationEngine;
import org.sonar.server.platform.db.migration.history.MigrationHistoryTableImpl;
import org.sonar.server.platform.db.migration.step.MigrationSteps;
import org.sonar.server.property.InternalProperties;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AutoDbMigrationH2Test {

  private DbClient dbClient = mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS);
  private DefaultServerUpgradeStatus serverUpgradeStatus = mock(DefaultServerUpgradeStatus.class);
  private MigrationEngine migrationEngine = mock(MigrationEngine.class);
  private MigrationSteps migrationSteps = mock(MigrationSteps.class);
  private SonarRuntime sonarRuntime = mock(SonarRuntime.class);
  private System2 system2 = mock(System2.class);

  private AutoDbMigration underTest = new AutoDbMigration(serverUpgradeStatus, dbClient, migrationEngine, migrationSteps, sonarRuntime, system2);


  @Test
  public void testInstallH2() throws SQLException {
    DbSession dbSession = mock(DbSession.class);
    when(dbClient.getDatabase().getDialect()).thenReturn(new H2());
    when(dbClient.openSession(anyBoolean())).thenReturn(dbSession);
    when(system2.now()).thenReturn(123456789L);
    H2Database db = new H2Database("sonar", false);
    db.start();
    Connection connection = db.getDataSource().getConnection();
    when(dbSession.getConnection()).thenReturn(connection);
    Version version = Version.create(7, 9, 0);
    when(sonarRuntime.getApiVersion()).thenReturn(version);
    new MigrationHistoryTableImpl(db).start();

    underTest.installH2();

    String selectInstallVersion = "select text_value from internal_properties where kee = '" + InternalProperties.INSTALLATION_VERSION + "'";
    ResultSet resultSetVersion = db.getDataSource().getConnection().prepareStatement(selectInstallVersion).executeQuery();
    resultSetVersion.next();
    Assertions.assertThat(resultSetVersion.getString(1)).isEqualTo("7.9");

    String selectInstallDate = "select text_value from internal_properties where kee = '" + InternalProperties.INSTALLATION_DATE + "'";
    ResultSet resultSetDate = db.getDataSource().getConnection().prepareStatement(selectInstallDate).executeQuery();
    resultSetDate.next();
    Assertions.assertThat(resultSetDate.getString(1)).isEqualTo("123456789");
  }

}
