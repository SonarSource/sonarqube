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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Optional;
import org.picocontainer.Startable;
import org.sonar.api.CoreProperties;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.property.InternalProperties;

import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.sonar.api.CoreProperties.SERVER_ID;
import static org.sonar.process.ProcessProperties.Property.JDBC_URL;
import static org.sonar.server.property.InternalProperties.SERVER_ID_CHECKSUM;

public class ServerIdManager implements Startable {
  private static final Logger LOGGER = Loggers.get(ServerIdManager.class);

  private final Configuration config;
  private final DbClient dbClient;
  private final SonarRuntime runtime;
  private final WebServer webServer;
  private final UuidFactory uuidFactory;

  public ServerIdManager(Configuration config, DbClient dbClient, SonarRuntime runtime, WebServer webServer, UuidFactory uuidFactory) {
    this.config = config;
    this.dbClient = dbClient;
    this.runtime = runtime;
    this.webServer = webServer;
    this.uuidFactory = uuidFactory;
  }

  @Override
  public void start() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (runtime.getSonarQubeSide() == SonarQubeSide.SERVER && webServer.isStartupLeader()) {
        if (needsToBeDropped(dbSession)) {
          dbClient.propertiesDao().deleteGlobalProperty(SERVER_ID, dbSession);
        }
        persistServerIdIfMissing(dbSession);
        dbSession.commit();
      } else {
        ensureServerIdIsValid(dbSession);
      }
    }
  }

  private boolean needsToBeDropped(DbSession dbSession) {
    PropertyDto dto = dbClient.propertiesDao().selectGlobalProperty(dbSession, SERVER_ID);
    if (dto == null) {
      // does not exist, no need to drop
      return false;
    }

    if (isEmpty(dto.getValue())) {
      return true;
    }

    if (isDate(dto.getValue())) {
      LOGGER.info("Server ID is changed to new format.");
      return true;
    }

    Optional<String> checksum = dbClient.internalPropertiesDao().selectByKey(dbSession, SERVER_ID_CHECKSUM);
    if (checksum.isPresent()) {
      String expectedChecksum = computeChecksum(dto.getValue());
      if (!expectedChecksum.equals(checksum.get())) {
        LOGGER.warn("Server ID is reset because it is not valid anymore. Database URL probably changed. The new server ID affects SonarSource licensed products.");
        return true;
      }
    }

    // Existing server ID must be kept when upgrading to 6.7+. In that case the checksum does
    // not exist.

    return false;
  }

  private void persistServerIdIfMissing(DbSession dbSession) {
    String serverId;
    PropertyDto idDto = dbClient.propertiesDao().selectGlobalProperty(dbSession, SERVER_ID);
    if (idDto == null) {
      serverId = uuidFactory.create();
      dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey(SERVER_ID).setValue(serverId));
    } else {
      serverId = idDto.getValue();
    }

    // checksum must be generated when it does not exist (upgrading to 6.7 or greater)
    // or when server ID changed.
    dbClient.internalPropertiesDao().save(dbSession, InternalProperties.SERVER_ID_CHECKSUM, computeChecksum(serverId));
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

  private String computeChecksum(String serverId) {
    String jdbcUrl = config.get(JDBC_URL.getKey()).orElseThrow(() -> new IllegalStateException("Missing JDBC URL"));
    return ServerIdChecksum.of(serverId, jdbcUrl);
  }

  private void ensureServerIdIsValid(DbSession dbSession) {
    PropertyDto id = dbClient.propertiesDao().selectGlobalProperty(dbSession, SERVER_ID);
    checkState(id != null, "Property %s is missing in database", SERVER_ID);
    checkState(isNotEmpty(id.getValue()), "Property %s is empty in database", SERVER_ID);

    Optional<String> checksum = dbClient.internalPropertiesDao().selectByKey(dbSession, SERVER_ID_CHECKSUM);
    checkState(checksum.isPresent(), "Internal property %s is missing in database", SERVER_ID_CHECKSUM);
    checkState(checksum.get().equals(computeChecksum(id.getValue())), "Server ID is invalid");
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
