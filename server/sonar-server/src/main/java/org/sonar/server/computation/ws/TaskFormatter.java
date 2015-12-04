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

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.computation.log.CeLogging;
import org.sonar.server.computation.log.LogFileRef;
import org.sonarqube.ws.WsCe;

/**
 * Converts {@link CeActivityDto} and {@link CeQueueDto} to the protobuf objects
 * used to write WS responses (see ws-ce.proto in module sonar-ws)
 */
public class TaskFormatter {

  private final DbClient dbClient;
  private final CeLogging ceLogging;
  private final System2 system2;

  public TaskFormatter(DbClient dbClient, CeLogging ceLogging, System2 system2) {
    this.dbClient = dbClient;
    this.ceLogging = ceLogging;
    this.system2 = system2;
  }

  public List<WsCe.Task> formatQueue(DbSession dbSession, List<CeQueueDto> dtos) {
    ComponentCache cache = new ComponentCache(dbSession);
    List<WsCe.Task> result = new ArrayList<>();
    for (CeQueueDto dto : dtos) {
      result.add(formatQueue(dto, cache));
    }
    return result;
  }

  public WsCe.Task formatQueue(DbSession dbSession, CeQueueDto dto) {
    return formatQueue(dto, new ComponentCache(dbSession));
  }

  private WsCe.Task formatQueue(CeQueueDto dto, ComponentCache componentCache) {
    WsCe.Task.Builder builder = WsCe.Task.newBuilder();
    builder.setId(dto.getUuid());
    builder.setStatus(WsCe.TaskStatus.valueOf(dto.getStatus().name()));
    builder.setType(dto.getTaskType());
    builder.setLogs(ceLogging.getFile(LogFileRef.from(dto)).isPresent());
    if (dto.getComponentUuid() != null) {
      builder.setComponentId(dto.getComponentUuid());
      buildComponent(builder, componentCache.get(dto.getComponentUuid()));
    }
    if (dto.getSubmitterLogin() != null) {
      builder.setSubmitterLogin(dto.getSubmitterLogin());
    }
    builder.setSubmittedAt(DateUtils.formatDateTime(new Date(dto.getCreatedAt())));
    if (dto.getStartedAt() != null) {
      builder.setStartedAt(DateUtils.formatDateTime(new Date(dto.getStartedAt())));
    }
    //
    Long executionTimeMs = computeExecutionTimeMs(dto);
    if (executionTimeMs != null) {
      builder.setExecutionTimeMs(executionTimeMs);
    }
    return builder.build();
  }

  public WsCe.Task formatActivity(DbSession dbSession, CeActivityDto dto) {
    return formatActivity(dto, new ComponentCache(dbSession));
  }

  public List<WsCe.Task> formatActivity(DbSession dbSession, List<CeActivityDto> dtos) {
    ComponentCache cache = new ComponentCache(dbSession);
    List<WsCe.Task> result = new ArrayList<>();
    for (CeActivityDto dto : dtos) {
      result.add(formatActivity(dto, cache));
    }
    return result;
  }

  private WsCe.Task formatActivity(CeActivityDto dto, ComponentCache componentCache) {
    WsCe.Task.Builder builder = WsCe.Task.newBuilder();
    builder.setId(dto.getUuid());
    builder.setStatus(WsCe.TaskStatus.valueOf(dto.getStatus().name()));
    builder.setType(dto.getTaskType());
    builder.setLogs(ceLogging.getFile(LogFileRef.from(dto)).isPresent());
    if (dto.getComponentUuid() != null) {
      builder.setComponentId(dto.getComponentUuid());
      buildComponent(builder, componentCache.get(dto.getComponentUuid()));
    }
    if (dto.getSnapshotId() != null) {
      builder.setAnalysisId(String.valueOf(dto.getSnapshotId()));
    }
    if (dto.getSubmitterLogin() != null) {
      builder.setSubmitterLogin(dto.getSubmitterLogin());
    }
    builder.setSubmittedAt(DateUtils.formatDateTime(new Date(dto.getSubmittedAt())));
    if (dto.getStartedAt() != null) {
      builder.setStartedAt(DateUtils.formatDateTime(new Date(dto.getStartedAt())));
    }
    if (dto.getExecutedAt() != null) {
      builder.setExecutedAt(DateUtils.formatDateTime(new Date(dto.getExecutedAt())));
    }
    if (dto.getExecutionTimeMs() != null) {
      builder.setExecutionTimeMs(dto.getExecutionTimeMs());
    }
    return builder.build();
  }

  private static void buildComponent(WsCe.Task.Builder builder, @Nullable ComponentDto componentDto) {
    if (componentDto != null) {
      builder.setComponentKey(componentDto.getKey());
      builder.setComponentName(componentDto.name());
      builder.setComponentQualifier(componentDto.qualifier());
    }
  }

  private class ComponentCache {
    private final DbSession dbSession;
    private final Map<String, ComponentDto> componentsByUuid = new HashMap<>();

    ComponentCache(DbSession dbSession) {
      this.dbSession = dbSession;
    }

    @CheckForNull
    ComponentDto get(String uuid) {
      ComponentDto dto = componentsByUuid.get(uuid);
      if (dto == null) {
        Optional<ComponentDto> opt = dbClient.componentDao().selectByUuid(dbSession, uuid);
        if (opt.isPresent()) {
          dto = opt.get();
          componentsByUuid.put(uuid, dto);
        }
      }
      return dto;
    }
  }

  /**
   * now - startedAt
   */
  @CheckForNull
  Long computeExecutionTimeMs(CeQueueDto dto) {
    Long startedAt = dto.getStartedAt();
    if (startedAt == null) {
      return null;
    }
    return system2.now() - startedAt;
  }
}
