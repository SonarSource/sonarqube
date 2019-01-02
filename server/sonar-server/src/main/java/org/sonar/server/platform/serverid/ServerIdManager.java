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
package org.sonar.server.platform.serverid;

import java.util.Optional;
import org.picocontainer.Startable;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.ServerId;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.platform.WebServer;
import org.sonar.server.property.InternalProperties;

import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.sonar.api.CoreProperties.SERVER_ID;
import static org.sonar.core.platform.ServerId.Format.DEPRECATED;
import static org.sonar.core.platform.ServerId.Format.NO_DATABASE_ID;
import static org.sonar.server.property.InternalProperties.SERVER_ID_CHECKSUM;

public class ServerIdManager implements Startable {
  private static final Logger LOGGER = Loggers.get(ServerIdManager.class);

  private final ServerIdChecksum serverIdChecksum;
  private final ServerIdFactory serverIdFactory;
  private final DbClient dbClient;
  private final SonarRuntime runtime;
  private final WebServer webServer;

  public ServerIdManager(ServerIdChecksum serverIdChecksum, ServerIdFactory serverIdFactory, DbClient dbClient, SonarRuntime runtime, WebServer webServer) {
    this.serverIdChecksum = serverIdChecksum;
    this.serverIdFactory = serverIdFactory;
    this.dbClient = dbClient;
    this.runtime = runtime;
    this.webServer = webServer;
  }

  @Override
  public void start() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (runtime.getSonarQubeSide() == SonarQubeSide.SERVER && webServer.isStartupLeader()) {
        Optional<String> checksum = dbClient.internalPropertiesDao().selectByKey(dbSession, SERVER_ID_CHECKSUM);

        ServerId serverId = readCurrentServerId(dbSession)
          .map(currentServerId -> keepOrReplaceCurrentServerId(dbSession, currentServerId, checksum))
          .orElseGet(() -> createFirstServerId(dbSession));
        updateChecksum(dbSession, serverId);

        dbSession.commit();
      } else {
        ensureServerIdIsValid(dbSession);
      }
    }
  }

  private ServerId keepOrReplaceCurrentServerId(DbSession dbSession, ServerId currentServerId, Optional<String> checksum) {
    if (keepServerId(currentServerId, checksum)) {
      return currentServerId;
    }

    ServerId serverId = replaceCurrentServerId(currentServerId);
    persistServerId(dbSession, serverId);
    return serverId;
  }

  private boolean keepServerId(ServerId serverId, Optional<String> checksum) {
    ServerId.Format format = serverId.getFormat();
    if (format == DEPRECATED || format == NO_DATABASE_ID) {
      LOGGER.info("Server ID is changed to new format.");
      return false;
    }

    if (checksum.isPresent()) {
      String expectedChecksum = serverIdChecksum.computeFor(serverId.toString());
      if (!expectedChecksum.equals(checksum.get())) {
        LOGGER.warn("Server ID is reset because it is not valid anymore. Database URL probably changed. The new server ID affects SonarSource licensed products.");
        return false;
      }
    }

    // Existing server ID must be kept when upgrading to 6.7+. In that case the checksum does not exist.
    return true;
  }

  private ServerId replaceCurrentServerId(ServerId currentServerId) {
    if (currentServerId.getFormat() == DEPRECATED) {
      return serverIdFactory.create();
    }
    return serverIdFactory.create(currentServerId);
  }

  private ServerId createFirstServerId(DbSession dbSession) {
    ServerId serverId = serverIdFactory.create();
    persistServerId(dbSession, serverId);
    return serverId;
  }

  private Optional<ServerId> readCurrentServerId(DbSession dbSession) {
    PropertyDto dto = dbClient.propertiesDao().selectGlobalProperty(dbSession, SERVER_ID);
    if (dto == null) {
      return Optional.empty();
    }

    String value = dto.getValue();
    if (isEmpty(value)) {
      return Optional.empty();
    }

    return Optional.of(ServerId.parse(value));
  }

  private void updateChecksum(DbSession dbSession, ServerId serverId) {
    // checksum must be generated when it does not exist (upgrading to 6.7 or greater)
    // or when server ID changed.
    String checksum = serverIdChecksum.computeFor(serverId.toString());
    persistChecksum(dbSession, checksum);
  }

  private void persistServerId(DbSession dbSession, ServerId serverId) {
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey(SERVER_ID).setValue(serverId.toString()));
  }

  private void persistChecksum(DbSession dbSession, String checksump) {
    dbClient.internalPropertiesDao().save(dbSession, InternalProperties.SERVER_ID_CHECKSUM, checksump);
  }

  private void ensureServerIdIsValid(DbSession dbSession) {
    PropertyDto id = dbClient.propertiesDao().selectGlobalProperty(dbSession, SERVER_ID);
    checkState(id != null, "Property %s is missing in database", SERVER_ID);
    checkState(isNotEmpty(id.getValue()), "Property %s is empty in database", SERVER_ID);

    Optional<String> checksum = dbClient.internalPropertiesDao().selectByKey(dbSession, SERVER_ID_CHECKSUM);
    checkState(checksum.isPresent(), "Internal property %s is missing in database", SERVER_ID_CHECKSUM);
    checkState(checksum.get().equals(serverIdChecksum.computeFor(id.getValue())), "Server ID is invalid");
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
