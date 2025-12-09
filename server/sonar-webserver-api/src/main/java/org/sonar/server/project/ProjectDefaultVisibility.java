/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.project;

import java.util.Optional;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;

public class ProjectDefaultVisibility {
  private static final String PROJECTS_DEFAULT_VISIBILITY_PROPERTY_NAME = "projects.default.visibility";
  private final DbClient dbClient;

  public ProjectDefaultVisibility(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public Visibility get(DbSession dbSession) {
    PropertyDto defaultProjectVisibility = Optional
      .ofNullable(dbClient.propertiesDao().selectGlobalProperty(dbSession, PROJECTS_DEFAULT_VISIBILITY_PROPERTY_NAME))
      .orElseThrow(() -> new IllegalStateException("Could not find default project visibility setting"));
    return Visibility.parseVisibility(defaultProjectVisibility.getValue());
  }

  public void set(DbSession dbSession, String visibilityLabel) {
    set(dbSession, Visibility.parseVisibility(visibilityLabel));
  }

  public void set(DbSession dbSession, Visibility visibility) {
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto()
      .setKey(PROJECTS_DEFAULT_VISIBILITY_PROPERTY_NAME)
      .setValue(visibility.getLabel()));
  }
}
