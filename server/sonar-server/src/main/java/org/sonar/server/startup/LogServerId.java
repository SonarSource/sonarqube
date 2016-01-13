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

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;

/**
 * @since 3.5
 */
public final class LogServerId {

  private final PropertiesDao propertiesDao;

  public LogServerId(PropertiesDao propertiesDao) {
    this.propertiesDao = propertiesDao;
  }

  public void start() {
    logServerId(Loggers.get(LogServerId.class));
  }

  @VisibleForTesting
  void logServerId(Logger logger) {
    PropertyDto serverIdProp = propertiesDao.selectGlobalProperty(CoreProperties.PERMANENT_SERVER_ID);
    if (serverIdProp != null) {
      // a server ID has been generated, let's print out the other useful informations that can help debugging license issues
      PropertyDto organisationProp = propertiesDao.selectGlobalProperty(CoreProperties.ORGANISATION);
      PropertyDto ipAddressProp = propertiesDao.selectGlobalProperty(CoreProperties.SERVER_ID_IP_ADDRESS);

      StringBuilder message = new StringBuilder("Server information:\n");
      message.append("  - ID            : ");
      addQuotedValue(serverIdProp, message);
      message.append("  - Organisation  : ");
      addQuotedValue(organisationProp, message);
      message.append("  - Registered IP : ");
      addQuotedValue(ipAddressProp, message);

      logger.info(message.toString());
    }
  }

  private static void addQuotedValue(PropertyDto property, StringBuilder message) {
    message.append("\"");
    message.append(property.getValue());
    message.append("\"\n");
  }

}
