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
package org.sonar.server.computation.ws;

import java.util.Date;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentService;
import org.sonarqube.ws.WsCe;

public class CeWsTaskFormatter {

  private final ComponentService componentService;

  public CeWsTaskFormatter(ComponentService componentService) {
    this.componentService = componentService;
  }

  public WsCe.Task format(CeQueueDto dto) {
    WsCe.Task.Builder builder = WsCe.Task.newBuilder();
    builder.setId(dto.getUuid());
    builder.setStatus(WsCe.TaskStatus.valueOf(dto.getStatus().name()));
    builder.setTaskType(dto.getTaskType());
    if (dto.getComponentUuid() != null) {
      builder.setComponentId(dto.getComponentUuid());
      buildComponent(builder, dto.getComponentUuid());
    }
    if (dto.getSubmitterLogin() != null) {
      builder.setSubmitterLogin(dto.getSubmitterLogin());
    }
    builder.setSubmittedAt(DateUtils.formatDateTime(new Date(dto.getCreatedAt())));
    if (dto.getStartedAt() != null) {
      builder.setStartedAt(DateUtils.formatDateTime(new Date(dto.getStartedAt())));
    }
    return builder.build();
  }

  public WsCe.Task format(CeActivityDto dto) {
    WsCe.Task.Builder builder = WsCe.Task.newBuilder();
    builder.setId(dto.getUuid());
    builder.setStatus(WsCe.TaskStatus.valueOf(dto.getStatus().name()));
    builder.setTaskType(dto.getTaskType());
    if (dto.getComponentUuid() != null) {
      builder.setComponentId(dto.getComponentUuid());
      buildComponent(builder, dto.getComponentUuid());
    }
    if (dto.getSubmitterLogin() != null) {
      builder.setSubmitterLogin(dto.getSubmitterLogin());
    }
    builder.setSubmittedAt(DateUtils.formatDateTime(new Date(dto.getCreatedAt())));
    if (dto.getStartedAt() != null) {
      builder.setStartedAt(DateUtils.formatDateTime(new Date(dto.getStartedAt())));
    }
    if (dto.getFinishedAt() != null) {
      builder.setFinishedAt(DateUtils.formatDateTime(new Date(dto.getFinishedAt())));
    }
    if (dto.getExecutionTimeMs() != null) {
      builder.setExecutionTimeMs(dto.getExecutionTimeMs());
    }
    return builder.build();
  }

  private void buildComponent(WsCe.Task.Builder builder, String componentUuid) {
    ComponentDto project = componentService.getNonNullByUuid(componentUuid);
    if (project != null) {
      builder.setComponentKey(project.getKey());
      builder.setComponentName(project.name());
    }
  }
}
