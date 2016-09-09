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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import javax.annotation.Nullable;
import org.picocontainer.Startable;
import org.sonar.api.CoreProperties;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.platform.cluster.Cluster;

import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.sonar.api.CoreProperties.SERVER_ID;

public class ServerIdManager implements Startable {
  private final DbClient dbClient;
  private final SonarRuntime runtime;
  private final Cluster cluster;
  private final UuidFactory uuidFactory;

  public ServerIdManager(DbClient dbClient, SonarRuntime runtime, Cluster cluster, UuidFactory uuidFactory) {
    this.dbClient = dbClient;
    this.runtime = runtime;
    this.cluster = cluster;
    this.uuidFactory = uuidFactory;
  }

  @Override
  public void start() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      PropertyDto dto = dbClient.propertiesDao().selectGlobalProperty(dbSession, SERVER_ID);
      if (runtime.getSonarQubeSide() == SonarQubeSide.SERVER && cluster.isStartupLeader()) {
        persistServerIdIfMissingOrOldFormatted(dbSession, dto);
      } else {
        ensureServerIdIsSet(dto);
      }
    }
  }

  /**
   * Insert or update {@link CoreProperties#SERVER_ID} property in DB to a UUID if it doesn't exist or if it's a date
   * (per the old format of {@link CoreProperties#SERVER_ID} before 6.1).
   */
  private void persistServerIdIfMissingOrOldFormatted(DbSession dbSession, @Nullable PropertyDto dto) {
    if (dto == null || dto.getValue().isEmpty() || isDate(dto.getValue())) {
      dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey(SERVER_ID).setValue(uuidFactory.create()));
      dbSession.commit();
    }
  }

  /**
   * Checks whether the specified value is a date according to the old format of the {@link CoreProperties#SERVER_ID}.
   */
  private static boolean isDate(String value) {
    try {
      new SimpleDateFormat("yyyyMMddHHmmss").parse(value);
      return true;
    } catch (ParseException e) {
      return false;
    }
  }

  private static void ensureServerIdIsSet(@Nullable PropertyDto dto) {
    checkState(dto != null, "Property %s is missing in database", SERVER_ID);
    checkState(!isBlank(dto.getValue()), "Property %s is set but empty in database", SERVER_ID);
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
