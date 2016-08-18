/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.ce.ws;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.component.ComponentDto;
import org.sonarqube.ws.WsCe;

/**
 * Converts {@link CeActivityDto} and {@link CeQueueDto} to the protobuf objects
 * used to write WS responses (see ws-ce.proto in module sonar-ws)
 */
public class TaskFormatter {

  private final DbClient dbClient;
  private final System2 system2;

  public TaskFormatter(DbClient dbClient, System2 system2) {
    this.dbClient = dbClient;
    this.system2 = system2;
  }

  public List<WsCe.Task> formatQueue(DbSession dbSession, List<CeQueueDto> dtos) {
    ComponentDtoCache cache = new ComponentDtoCache(dbSession, ceQueueDtoToComponentUuids(dtos));
    return dtos.stream().map(input -> formatQueue(input, cache)).collect(Collectors.toList());
  }

  public WsCe.Task formatQueue(DbSession dbSession, CeQueueDto dto) {
    return formatQueue(dto, new ComponentDtoCache(dbSession, dto.getComponentUuid()));
  }

  private WsCe.Task formatQueue(CeQueueDto dto, ComponentDtoCache componentDtoCache) {
    WsCe.Task.Builder builder = WsCe.Task.newBuilder();
    builder.setId(dto.getUuid());
    builder.setStatus(WsCe.TaskStatus.valueOf(dto.getStatus().name()));
    builder.setType(dto.getTaskType());
    builder.setLogs(false);
    if (dto.getComponentUuid() != null) {
      builder.setComponentId(dto.getComponentUuid());
      buildComponent(builder, componentDtoCache.get(dto.getComponentUuid()));
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
    return formatActivity(dbSession, dto, null);
  }

  public WsCe.Task formatActivity(DbSession dbSession, CeActivityDto dto, @Nullable String scannerContext) {
    return formatActivity(dto, new ComponentDtoCache(dbSession, dto.getComponentUuid()), scannerContext);
  }

  public List<WsCe.Task> formatActivity(DbSession dbSession, List<CeActivityDto> dtos) {
    ComponentDtoCache cache = new ComponentDtoCache(dbSession, ceActivityDtoToComponentUuids(dtos));
    return dtos.stream().map(input -> formatActivity(input, cache, null)).collect(Collectors.toList());
  }

  private WsCe.Task formatActivity(CeActivityDto dto, ComponentDtoCache componentDtoCache, @Nullable String scannerContext) {
    WsCe.Task.Builder builder = WsCe.Task.newBuilder();
    builder.setId(dto.getUuid());
    builder.setStatus(WsCe.TaskStatus.valueOf(dto.getStatus().name()));
    builder.setType(dto.getTaskType());
    builder.setLogs(false);
    if (dto.getComponentUuid() != null) {
      builder.setComponentId(dto.getComponentUuid());
      buildComponent(builder, componentDtoCache.get(dto.getComponentUuid()));
    }
    if (dto.getAnalysisUuid() != null) {
      builder.setAnalysisId(dto.getAnalysisUuid());
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
    if (dto.getErrorMessage() != null) {
      builder.setErrorMessage(dto.getErrorMessage());
    }
    if (dto.getErrorStacktrace() != null) {
      builder.setErrorStacktrace(dto.getErrorStacktrace());
    }
    if (scannerContext != null) {
      builder.setScannerContext(scannerContext);
    }
    builder.setHasScannerContext(dto.isHasScannerContext());
    return builder.build();
  }

  private static void buildComponent(WsCe.Task.Builder builder, @Nullable ComponentDto componentDto) {
    if (componentDto != null) {
      builder.setComponentKey(componentDto.getKey());
      builder.setComponentName(componentDto.name());
      builder.setComponentQualifier(componentDto.qualifier());
    }
  }

  private static Set<String> ceQueueDtoToComponentUuids(List<CeQueueDto> dtos) {
    return dtos.stream().map(CeQueueDto::getComponentUuid)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
  }

  private static Set<String> ceActivityDtoToComponentUuids(List<CeActivityDto> dtos) {
    return dtos.stream().map(CeActivityDto::getComponentUuid)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
  }

  private class ComponentDtoCache {
    private final Map<String, ComponentDto> componentsByUuid;

    ComponentDtoCache(DbSession dbSession, Set<String> uuids) {
      this.componentsByUuid = dbClient.componentDao().selectByUuids(dbSession, uuids)
        .stream()
        .collect(Collectors.toMap(ComponentDto::uuid, Function.identity()));
    }

    ComponentDtoCache(DbSession dbSession, String uuid) {
      Optional<ComponentDto> componentDto = dbClient.componentDao().selectByUuid(dbSession, uuid);
      this.componentsByUuid = componentDto.isPresent() ? ImmutableMap.of(uuid, componentDto.get()) : Collections.emptyMap();
    }

    @CheckForNull
    ComponentDto get(@Nullable String uuid) {
      if (uuid == null) {
        return null;
      }
      return componentsByUuid.get(uuid);
    }
  }

  /**
   * now - startedAt
   */
  @CheckForNull
  private Long computeExecutionTimeMs(CeQueueDto dto) {
    Long startedAt = dto.getStartedAt();
    if (startedAt == null) {
      return null;
    }
    return system2.now() - startedAt;
  }

}
