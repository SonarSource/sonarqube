/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.AnticipatedTransition;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.AnticipatedTransitionDao;
import org.sonar.db.project.ProjectDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AnticipatedTransitionHandlerTest {

  private static final UuidFactory UUID_FACTORY = mock(UuidFactory.class);
  public static final String USER_UUID = "userUuid";
  public static final String PROJECT_KEY = "projectKey";
  private final AnticipatedTransitionParser anticipatedTransitionParser = mock(AnticipatedTransitionParser.class);
  private final AnticipatedTransitionDao anticipatedTransitionDao = mock(AnticipatedTransitionDao.class);
  private final DbClient dbClient = mock(DbClient.class);
  private final AnticipatedTransitionHandler underTest = new AnticipatedTransitionHandler(anticipatedTransitionParser, anticipatedTransitionDao, UUID_FACTORY, dbClient);

  @Test
  public void givenRequestBodyWithNoTransitions_whenHandleRequestBody_thenPreviousTransitionsArePurged() {
    // given
    ProjectDto projectDto = new ProjectDto()
      .setKey(PROJECT_KEY);

    String requestBody = "body_with_no_transitions";
    doReturn(List.of())
      .when(anticipatedTransitionParser).parse(requestBody, USER_UUID, PROJECT_KEY);

    DbSession dbSession = mockDbSession();

    // when
    underTest.handleRequestBody(requestBody, USER_UUID, projectDto);

    // then
    verify(dbClient).openSession(true);
    verify(anticipatedTransitionDao).deleteByProjectAndUser(dbSession, projectDto.getUuid(), USER_UUID);
    verify(anticipatedTransitionDao, never()).insert(eq(dbSession), any());
  }

  @Test
  public void fivenRequestBodyWithTransitions_whenHandleRequestBody_thenTransitionsAreInserted() {
    // given
    ProjectDto projectDto = new ProjectDto()
      .setKey(PROJECT_KEY);

    String requestBody = "body_with_transitions";
    doReturn(List.of(populateAnticipatedTransition(), populateAnticipatedTransition()))
      .when(anticipatedTransitionParser).parse(requestBody, USER_UUID, PROJECT_KEY);

    DbSession dbSession = mockDbSession();

    // when
    underTest.handleRequestBody(requestBody, USER_UUID, projectDto);

    // then
    verify(dbClient).openSession(true);
    verify(anticipatedTransitionDao).deleteByProjectAndUser(dbSession, projectDto.getUuid(), USER_UUID);
    verify(anticipatedTransitionDao, times(2)).insert(eq(dbSession), any());
  }

  private DbSession mockDbSession() {
    DbSession dbSession = mock(DbSession.class);
    doReturn(dbSession).when(dbClient).openSession(true);
    return dbSession;
  }

  private AnticipatedTransition populateAnticipatedTransition() {
    return new AnticipatedTransition(
      null,
      PROJECT_KEY,
      USER_UUID,
      RuleKey.of("repo", "squid:S0001"),
      "issueMessage1",
      "filePath1",
      1,
      "lineHash1",
      "transition1",
      "comment1");
  }
}
