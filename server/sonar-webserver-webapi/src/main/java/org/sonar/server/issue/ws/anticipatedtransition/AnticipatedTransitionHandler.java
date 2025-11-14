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
package org.sonar.server.issue.ws.anticipatedtransition;

import java.util.List;
import org.sonar.core.issue.AnticipatedTransition;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.AnticipatedTransitionDao;
import org.sonar.db.issue.AnticipatedTransitionDto;
import org.sonar.db.project.ProjectDto;

public class AnticipatedTransitionHandler {

  private final AnticipatedTransitionParser anticipatedTransitionParser;
  private final AnticipatedTransitionDao anticipatedTransitionDao;
  private final UuidFactory uuidFactory;
  private final DbClient dbClient;


  public AnticipatedTransitionHandler(AnticipatedTransitionParser anticipatedTransitionParser,
    AnticipatedTransitionDao anticipatedTransitionDao, UuidFactory uuidFactory, DbClient dbClient) {
    this.anticipatedTransitionParser = anticipatedTransitionParser;
    this.anticipatedTransitionDao = anticipatedTransitionDao;
    this.uuidFactory = uuidFactory;
    this.dbClient = dbClient;
  }

  public void handleRequestBody(String requestBody, String userUuid, ProjectDto projectDto) {
    // parse anticipated transitions from request body
    List<AnticipatedTransition> anticipatedTransitions = anticipatedTransitionParser.parse(requestBody, userUuid, projectDto.getKey());

    try (DbSession dbSession = dbClient.openSession(true)) {
      // delete previous anticipated transitions for the user and project
      deletePreviousAnticipatedTransitionsForUserAndProject(dbSession, userUuid, projectDto.getUuid());

      // insert new anticipated transitions
      insertAnticipatedTransitions(dbSession, anticipatedTransitions, projectDto.getUuid());
      dbSession.commit();
    }
  }

  private void deletePreviousAnticipatedTransitionsForUserAndProject(DbSession dbSession, String userUuid, String projectUuid) {
    anticipatedTransitionDao.deleteByProjectAndUser(dbSession, projectUuid, userUuid);
  }

  private void insertAnticipatedTransitions(DbSession dbSession, List<AnticipatedTransition> anticipatedTransitions, String projectUuid) {
    anticipatedTransitions.forEach(anticipatedTransition ->
      anticipatedTransitionDao.insert(dbSession, AnticipatedTransitionDto.toDto(anticipatedTransition, uuidFactory.create(), projectUuid)));
  }
}
