/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.base.Optional;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;

import static com.google.common.base.Optional.fromNullable;

public class ServerIdLoader {

  private final Settings settings;
  private final ServerIdGenerator idGenerator;

  public ServerIdLoader(Settings settings, ServerIdGenerator idGenerator) {
    this.settings = settings;
    this.idGenerator = idGenerator;
  }

  public Optional<String> getRaw() {
    return fromNullable(settings.getString(CoreProperties.PERMANENT_SERVER_ID));
  }

  public Optional<ServerId> get() {
    Optional<String> rawId = getRaw();
    if (!rawId.isPresent()) {
      return Optional.absent();
    }

    String organization = settings.getString(CoreProperties.ORGANISATION);
    String ipAddress = settings.getString(CoreProperties.SERVER_ID_IP_ADDRESS);
    boolean validated = organization != null
      && ipAddress != null
      && idGenerator.validate(organization, ipAddress, rawId.get());

    return Optional.of(new ServerId(rawId.get(), validated));
  }
}
