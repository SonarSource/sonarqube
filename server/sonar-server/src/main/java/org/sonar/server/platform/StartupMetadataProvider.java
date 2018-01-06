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
package org.sonar.server.platform;

import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.CoreProperties;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;

import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.sonar.api.CoreProperties.SERVER_STARTTIME;

@ComputeEngineSide
@ServerSide
public class StartupMetadataProvider extends ProviderAdapter {

  private StartupMetadata cache = null;

  public StartupMetadata provide(System2 system, SonarRuntime runtime, WebServer webServer, DbClient dbClient) {
    if (cache == null) {
      if (runtime.getSonarQubeSide() == SonarQubeSide.SERVER && webServer.isStartupLeader()) {
        cache = generate(system);
      } else {
        cache = load(dbClient);
      }
    }
    return cache;
  }

  /**
   * Generate a {@link CoreProperties#SERVER_STARTTIME}.
   * <p>
   * Persistence is performed by {@link StartupMetadataPersister}.
   * </p>
   */
  private static StartupMetadata generate(System2 system) {
    return new StartupMetadata(system.now());
  }

  /**
   * Load from database
   */
  private static StartupMetadata load(DbClient dbClient) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String startedAt = selectProperty(dbClient, dbSession, SERVER_STARTTIME);
      return new StartupMetadata(DateUtils.parseDateTime(startedAt).getTime());
    }
  }

  private static String selectProperty(DbClient dbClient, DbSession dbSession, String key) {
    PropertyDto prop = dbClient.propertiesDao().selectGlobalProperty(dbSession, key);
    checkState(prop != null, "Property %s is missing in database", key);
    checkState(!isBlank(prop.getValue()), "Property %s is set but empty in database", key);
    return prop.getValue();
  }
}
