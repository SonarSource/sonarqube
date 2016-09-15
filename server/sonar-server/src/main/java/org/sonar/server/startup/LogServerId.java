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
package org.sonar.server.startup;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.picocontainer.Startable;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;

public final class LogServerId implements Startable {

  private final DbClient dbClient;

  public LogServerId(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void start() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String propertyKey = CoreProperties.PERMANENT_SERVER_ID;
      PropertyDto serverIdProp = selectProperty(dbSession, propertyKey);
      if (serverIdProp != null) {
        // a server ID has been generated, let's print out the other useful information that can help debugging license issues
        PropertyDto organizationProp = selectProperty(dbSession, CoreProperties.ORGANISATION);
        PropertyDto ipAddressProp = selectProperty(dbSession, CoreProperties.SERVER_ID_IP_ADDRESS);

        StringBuilder message = new StringBuilder("Server information:\n");
        message.append("  - ID           : ");
        addQuotedValue(serverIdProp, message);
        message.append("  - Organization : ");
        addQuotedValue(organizationProp, message);
        message.append("  - Registered IP: ");
        addQuotedValue(ipAddressProp, message);

        Loggers.get(LogServerId.class).info(message.toString());
      }
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }

  @CheckForNull
  private PropertyDto selectProperty(DbSession dbSession, String propertyKey) {
    return dbClient.propertiesDao().selectGlobalProperty(dbSession, propertyKey);
  }

  private static void addQuotedValue(@Nullable PropertyDto property, StringBuilder message) {
    if (property == null || property.getValue() == null) {
      message.append('-');
    } else {
      message.append("\"");
      message.append(property.getValue());
      message.append("\"");
    }
    message.append('\n');
  }

}
