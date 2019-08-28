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
package org.sonar.db.component;

import java.util.Arrays;
import java.util.function.Consumer;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.sonar.db.component.ProjectLinkTesting.newCustomLinkDto;
import static org.sonar.db.component.ProjectLinkTesting.newProvidedLinkDto;

public class ProjectLinkDbTester {
  private final DbTester db;
  private final DbClient dbClient;
  private final DbSession dbSession;

  public ProjectLinkDbTester(DbTester db) {
    this.db = db;
    this.dbClient = db.getDbClient();
    this.dbSession = db.getSession();
  }

  @SafeVarargs
  public final ProjectLinkDto insertProvidedLink(ComponentDto project, Consumer<ProjectLinkDto>... dtoPopulators) {
    return insertLink(project, newProvidedLinkDto(), dtoPopulators);
  }

  @SafeVarargs
  public final ProjectLinkDto insertCustomLink(ComponentDto project, Consumer<ProjectLinkDto>... dtoPopulators) {
    return insertLink(project, newCustomLinkDto(), dtoPopulators);
  }

  @SafeVarargs
  private final ProjectLinkDto insertLink(ComponentDto project, ProjectLinkDto componentLink, Consumer<ProjectLinkDto>... dtoPopulators) {
    Arrays.stream(dtoPopulators).forEach(dtoPopulator -> dtoPopulator.accept(componentLink));
    dbClient.projectLinkDao().insert(dbSession, componentLink.setProjectUuid(project.uuid()));
    db.commit();
    return componentLink;
  }
}
