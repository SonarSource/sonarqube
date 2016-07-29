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
package org.sonar.server.platform;

import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.CoreProperties;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.platform.cluster.Cluster;

import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang.StringUtils.isBlank;

@ComputeEngineSide
@ServerSide
public class StartupMetadataProvider extends ProviderAdapter {

  private StartupMetadata cache = null;

  public StartupMetadata provide(UuidFactory uuidFactory, System2 system, SonarRuntime runtime, Cluster cluster, DbClient dbClient) {
    if (cache == null) {
      if (runtime.getSonarQubeSide() == SonarQubeSide.SERVER && cluster.isStartupLeader()) {
        cache = generate(uuidFactory, system);
      } else {
        cache = load(dbClient);
      }
    }
    return cache;
  }

  /**
   * Generate a UUID. It is not persisted yet as db structure may not be up-to-date if migrations
   * have to be executed. This is done later by {@link StartupMetadataPersister}
   */
  private static StartupMetadata generate(UuidFactory uuidFactory, System2 system) {
    String startupId = uuidFactory.create();
    long startedAt = system.now();
    return new StartupMetadata(startupId, startedAt);
  }

  /**
   * Load from database
   */
  private static StartupMetadata load(DbClient dbClient) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String startupId = selectProperty(dbClient, dbSession, CoreProperties.SERVER_ID);
      String startedAt = selectProperty(dbClient, dbSession, CoreProperties.SERVER_STARTTIME);
      return new StartupMetadata(startupId, DateUtils.parseDateTime(startedAt).getTime());
    }
  }

  private static String selectProperty(DbClient dbClient, DbSession dbSession, String key) {
    PropertyDto prop = dbClient.propertiesDao().selectGlobalProperty(dbSession, key);
    checkState(prop != null, "Property %s is missing in database", key);
    checkState(!isBlank(prop.getValue()), "Property %s is set but empty in database", key);
    return prop.getValue();
  }
}
