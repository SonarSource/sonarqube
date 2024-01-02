/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.ce.task.projectanalysis.component.Component;
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
  public Collection<AnticipatedTransition> getAnticipatedTransitionByComponent(Component component) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String projectUuid = dbClient.entityDao().selectByComponentUuid(dbSession, component.getUuid()).map(EntityDto::getUuid)
        .orElse(calculateProjectUuidFromComponentKey(dbSession, component));
      List<AnticipatedTransitionDto> anticipatedTransitionDtos = dbClient.anticipatedTransitionDao()
        .selectByProjectUuidAndFilePath(dbSession, projectUuid, component.getName());
      return getAnticipatedTransitions(anticipatedTransitionDtos);
    }
  }

  private String calculateProjectUuidFromComponentKey(DbSession dbSession, Component component) {
    String projectKey = component.getKey().split(":")[0];
    return dbClient.projectDao().selectProjectByKey(dbSession, projectKey).map(EntityDto::getUuid).orElse("");
  }

  private Collection<AnticipatedTransition> getAnticipatedTransitions(List<AnticipatedTransitionDto> anticipatedTransitionDtos) {
    return anticipatedTransitionDtos
      .stream()
      .map(AnticipatedTransitionRepositoryImpl::getAnticipatedTransition)
      .toList();
  }

  private static AnticipatedTransition getAnticipatedTransition(AnticipatedTransitionDto transitionDto) {
    return new AnticipatedTransition(
      transitionDto.getUuid(),
      transitionDto.getProjectUuid(),
      transitionDto.getUserUuid(),
      RuleKey.parse(transitionDto.getRuleKey()),
      transitionDto.getMessage(),
      transitionDto.getFilePath(),
      transitionDto.getLine(),
      transitionDto.getLineHash(),
      transitionDto.getTransition(),
      transitionDto.getComment()
    );
  }
}
