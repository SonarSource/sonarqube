/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.startup;

import com.google.common.collect.ImmutableMap;
import org.picocontainer.Startable;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.CoreProperties;
import org.sonar.api.platform.Server;
import org.sonar.server.platform.PersistentSettings;

import java.text.SimpleDateFormat;

public final class ServerMetadataPersister implements Startable {

  private final Server server;
  private final PersistentSettings persistentSettings;

  public ServerMetadataPersister(Server server, PersistentSettings persistentSettings) {
    this.server = server;
    this.persistentSettings = persistentSettings;
  }

  @Override
  public void start() {
    Loggers.get(getClass()).debug("Persisting server metadata");
    persistentSettings.saveProperties(ImmutableMap.of(
      CoreProperties.SERVER_ID, server.getId(),
      CoreProperties.SERVER_VERSION, server.getVersion(),
      CoreProperties.SERVER_STARTTIME, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(server.getStartedAt())));
  }

  @Override
  public void stop() {
    // nothing
  }
}
