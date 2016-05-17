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

import com.google.common.base.Optional;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkNotNull;

public class ServerIdLoader {

  private final Settings settings;
  private final ServerIdGenerator idGenerator;

  public ServerIdLoader(Settings settings, ServerIdGenerator idGenerator) {
    this.settings = settings;
    this.idGenerator = idGenerator;
  }

  public Optional<String> get() {
    return fromNullable(settings.getString(CoreProperties.PERMANENT_SERVER_ID));
  }

  public boolean isValid(String serverId) {
    checkNotNull(serverId, "Server ID can not be null");
    String organisation = settings.getString(CoreProperties.ORGANISATION);
    String ipAddress = settings.getString(CoreProperties.SERVER_ID_IP_ADDRESS);
    if (organisation == null || ipAddress == null) {
      return false;
    }
    String generatedId = idGenerator.generate(organisation, ipAddress);
    return generatedId != null && generatedId.equals(serverId);
  }
}
