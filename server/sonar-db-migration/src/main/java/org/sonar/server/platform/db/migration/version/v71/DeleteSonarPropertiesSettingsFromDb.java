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

package org.sonar.server.platform.db.migration.version.v71;

import com.google.common.base.Joiner;
import java.sql.SQLException;
import java.util.List;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;
import org.sonar.server.platform.db.migration.step.SqlStatement;

import static java.util.Arrays.asList;

public class DeleteSonarPropertiesSettingsFromDb extends DataChange {

  private static final List<String> SONAR_PROPERTIES = asList(
    "sonar.jdbc.url",
    "sonar.jdbc.username",
    "sonar.jdbc.password",
    "sonar.jdbc.driverPath",
    "sonar.jdbc.maxActive",
    "sonar.jdbc.maxIdle",
    "sonar.jdbc.minIdle",
    "sonar.jdbc.maxWait",
    "sonar.jdbc.minEvictableIdleTimeMillis",
    "sonar.jdbc.timeBetweenEvictionRunsMillis",
    "sonar.embeddedDatabase.port",
    "sonar.path.data",
    "sonar.path.home",
    "sonar.path.logs",
    "sonar.path.temp",
    "sonar.path.web",
    "sonar.search.host",
    "sonar.search.port",
    "sonar.search.httpPort",
    "sonar.search.javaOpts",
    "sonar.search.javaAdditionalOpts",
    "sonar.search.replicas",
    "sonar.search.minimumMasterNodes",
    "sonar.search.initialStateTimeout",
    "sonar.web.javaOpts",
    "sonar.web.javaAdditionalOpts",
    "sonar.web.port",
    "sonar.auth.jwtBase64Hs256Secret",
    "sonar.ce.javaOpts",
    "sonar.ce.javaAdditionalOpts",
    "sonar.enableStopCommand",
    "http.proxyHost",
    "https.proxyHost",
    "http.proxyPort",
    "https.proxyPort",
    "http.proxyUser",
    "http.proxyPassword",
    "sonar.cluster.enabled",
    "sonar.cluster.node.type",
    "sonar.cluster.search.hosts",
    "sonar.cluster.hosts",
    "sonar.cluster.node.port",
    "sonar.cluster.node.host",
    "sonar.cluster.node.name",
    "sonar.cluster.name",
    "sonar.cluster.web.startupLeader",
    "sonar.sonarcloud.enabled",
    "sonar.updatecenter.activate");

  private static final Joiner COMA_JOINER = Joiner.on(",");

  public DeleteSonarPropertiesSettingsFromDb(Database db) {
    super(db);
  }

  @Override
  protected void execute(DataChange.Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    String selectSql = "select id from properties where prop_key in (";
    selectSql += SONAR_PROPERTIES.stream().map(p -> "?").collect(MoreCollectors.join(COMA_JOINER));
    selectSql += ")";
    SqlStatement selectStatement = massUpdate.select(selectSql);
    for (int i = 1; i <= SONAR_PROPERTIES.size(); i++) {
      selectStatement.setString(i, SONAR_PROPERTIES.get(i - 1));
    }
    massUpdate.update("delete from properties where id=?");
    massUpdate.execute((row, update) -> {
      update.setLong(1, row.getLong(1));
      return true;
    });
  }
}
