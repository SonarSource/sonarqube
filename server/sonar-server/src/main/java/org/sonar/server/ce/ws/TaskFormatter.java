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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentDtoFunctions;
import org.sonar.ce.log.CeLogging;
import org.sonar.ce.log.LogFileRef;
import org.sonarqube.ws.WsCe;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;

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

  public Iterable<WsCe.Task> formatQueue(DbSession dbSession, List<CeQueueDto> dtos) {
    ComponentDtoCache cache = new ComponentDtoCache(dbSession, ceQueueDtoToComponentUuids(dtos));
    return from(dtos)
      .transform(new CeQueueDtoToTask(cache));
  }

  public WsCe.Task formatQueue(DbSession dbSession, CeQueueDto dto) {
    return formatQueue(dto, new ComponentDtoCache(dbSession, dto.getComponentUuid()));
  }

  private WsCe.Task formatQueue(CeQueueDto dto, ComponentDtoCache componentDtoCache) {
    WsCe.Task.Builder builder = WsCe.Task.newBuilder();
    builder.setId(dto.getUuid());
    builder.setStatus(WsCe.TaskStatus.valueOf(dto.getStatus().name()));
    builder.setType(dto.getTaskType());
    builder.setLogs(ceLogging.getFile(LogFileRef.from(dto)).isPresent());
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
    return formatActivity(dto, new ComponentDtoCache(dbSession, dto.getComponentUuid()));
  }

  public Iterable<WsCe.Task> formatActivity(DbSession dbSession, List<CeActivityDto> dtos) {
    ComponentDtoCache cache = new ComponentDtoCache(dbSession, ceActivityDtoToComponentUuids(dtos));
    return from(dtos).transform(new CeActivityDtoToTask(cache));
  }

  private WsCe.Task formatActivity(CeActivityDto dto, ComponentDtoCache componentDtoCache) {
    WsCe.Task.Builder builder = WsCe.Task.newBuilder();
    builder.setId(dto.getUuid());
    builder.setStatus(WsCe.TaskStatus.valueOf(dto.getStatus().name()));
    builder.setType(dto.getTaskType());
    builder.setLogs(ceLogging.getFile(LogFileRef.from(dto)).isPresent());
    if (dto.getComponentUuid() != null) {
      builder.setComponentId(dto.getComponentUuid());
      buildComponent(builder, componentDtoCache.get(dto.getComponentUuid()));
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

  private static Set<String> ceQueueDtoToComponentUuids(Iterable<CeQueueDto> dtos) {
    return from(dtos)
      .transform(CeQueueDtoToComponentUuid.INSTANCE)
      .filter(notNull())
      .toSet();
  }

  private static Set<String> ceActivityDtoToComponentUuids(Iterable<CeActivityDto> dtos) {
    return from(dtos)
      .transform(CeActivityDtoToComponentUuid.INSTANCE)
      .filter(notNull())
      .toSet();
  }

  private enum CeQueueDtoToComponentUuid implements Function<CeQueueDto, String> {
    INSTANCE;

    @Override
    @Nullable
    public String apply(@Nonnull CeQueueDto input) {
      return input.getComponentUuid();
    }
  }

  private enum CeActivityDtoToComponentUuid implements Function<CeActivityDto, String> {
    INSTANCE;

    @Override
    @Nullable
    public String apply(@Nonnull CeActivityDto input) {
      return input.getComponentUuid();
    }
  }

  private class ComponentDtoCache {
    private final Map<String, ComponentDto> componentsByUuid;

    public ComponentDtoCache(DbSession dbSession, Set<String> uuids) {
      this.componentsByUuid = from(dbClient.componentDao().selectByUuids(dbSession, uuids)).uniqueIndex(ComponentDtoFunctions.toUuid());
    }

    public ComponentDtoCache(DbSession dbSession, String uuid) {
      Optional<ComponentDto> componentDto = dbClient.componentDao().selectByUuid(dbSession, uuid);
      this.componentsByUuid = componentDto.isPresent() ? ImmutableMap.of(uuid, componentDto.get()) : Collections.<String, ComponentDto>emptyMap();
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
  Long computeExecutionTimeMs(CeQueueDto dto) {
    Long startedAt = dto.getStartedAt();
    if (startedAt == null) {
      return null;
    }
    return system2.now() - startedAt;
  }

  private final class CeActivityDtoToTask implements Function<CeActivityDto, WsCe.Task> {
    private final ComponentDtoCache cache;

    public CeActivityDtoToTask(ComponentDtoCache cache) {
      this.cache = cache;
    }

    @Override
    @Nonnull
    public WsCe.Task apply(@Nonnull CeActivityDto input) {
      return formatActivity(input, cache);
    }
  }

  private final class CeQueueDtoToTask implements Function<CeQueueDto, WsCe.Task> {
    private final ComponentDtoCache cache;

    public CeQueueDtoToTask(ComponentDtoCache cache) {
      this.cache = cache;
    }

    @Override
    @Nonnull
    public WsCe.Task apply(@Nonnull CeQueueDto input) {
      return formatQueue(input, cache);
    }
  }
}
