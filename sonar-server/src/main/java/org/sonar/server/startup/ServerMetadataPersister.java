/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.startup;

import org.sonar.api.CoreProperties;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.configuration.Property;
import org.sonar.api.platform.Server;

import java.text.SimpleDateFormat;

public class ServerMetadataPersister {

  private Server server;
  private DatabaseSession session;

  public ServerMetadataPersister(Server server, DatabaseSession session) {
    this.server = server;
    this.session = session;
  }

  public void start() {
    setProperty(CoreProperties.SERVER_ID, server.getId());
    setProperty(CoreProperties.SERVER_VERSION, server.getVersion());
    if (server.getStartedAt() != null) {
      setProperty(CoreProperties.SERVER_STARTTIME, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(server.getStartedAt()));
    }
    session.commit();
  }

  private void setProperty(String key, String value) {
    Property prop = session.getSingleResult(Property.class, "key", key);
    if (prop == null) {
      prop = new Property(key, value);
    } else {
      prop.setValue(value);
    }
    session.save(prop);
  }
}
