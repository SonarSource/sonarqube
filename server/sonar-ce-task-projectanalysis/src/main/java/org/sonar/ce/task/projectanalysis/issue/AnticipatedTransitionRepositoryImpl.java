/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Collection;
import java.util.List;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.AnticipatedTransition;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.issue.AnticipatedTransitionDto;

public class AnticipatedTransitionRepositoryImpl implements AnticipatedTransitionRepository {

  private final DbClient dbClient;

  public AnticipatedTransitionRepositoryImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public Collection<AnticipatedTransition> getAnticipatedTransitionByProjectUuid(String componentUuid, String filePath) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      EntityDto entityDto = dbClient.entityDao().selectByComponentUuid(dbSession, componentUuid).orElseThrow(IllegalStateException::new);
      List<AnticipatedTransitionDto> anticipatedTransitionDtos = dbClient.anticipatedTransitionDao().selectByProjectUuid(dbSession, entityDto.getUuid());
      return getAnticipatedTransitions(anticipatedTransitionDtos);
    }
  }

  private Collection<AnticipatedTransition> getAnticipatedTransitions(List<AnticipatedTransitionDto> anticipatedTransitionDtos) {
    return anticipatedTransitionDtos
      .stream()
      .map(this::getAnticipatedTransition)
      .toList();
  }

  private AnticipatedTransition getAnticipatedTransition(AnticipatedTransitionDto transitionDto) {
    return new AnticipatedTransition(
      transitionDto.getProjectUuid(),
      "branch",
      transitionDto.getUserUuid(),
      RuleKey.parse(transitionDto.getRuleKey()),
      transitionDto.getMessage(),
      "filepath",
      transitionDto.getLine(),
      transitionDto.getLineHash(),
      transitionDto.getTransition(),
      transitionDto.getComment()
    );
  }

}
