/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.ce.ws;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskCharacteristicDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonarqube.ws.WsCe;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.util.Protobuf.setNullable;

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
    DtoCache cache = DtoCache.forQueueDtos(dbClient, dbSession, dtos);
    return dtos.stream().map(input -> formatQueue(input, cache)).collect(MoreCollectors.toList(dtos.size()));
  }

  public WsCe.Task formatQueue(DbSession dbSession, CeQueueDto queue) {
    return formatQueue(queue, DtoCache.forQueueDtos(dbClient, dbSession, singletonList(queue)));
  }

  private WsCe.Task formatQueue(CeQueueDto dto, DtoCache componentDtoCache) {
    WsCe.Task.Builder builder = WsCe.Task.newBuilder();
    String organizationKey = componentDtoCache.getOrganizationKey(dto.getComponentUuid());
    // FIXME organization field should be set from the CeQueueDto rather than from the ComponentDto
    setNullable(organizationKey, builder::setOrganization);
    if (dto.getComponentUuid() != null) {
      builder.setComponentId(dto.getComponentUuid());
      buildComponent(builder, componentDtoCache.getComponent(dto.getComponentUuid()));
    }
    builder.setId(dto.getUuid());
    builder.setStatus(WsCe.TaskStatus.valueOf(dto.getStatus().name()));
    builder.setType(dto.getTaskType());
    builder.setLogs(false);
    setNullable(dto.getSubmitterLogin(), builder::setSubmitterLogin);
    builder.setSubmittedAt(formatDateTime(new Date(dto.getCreatedAt())));
    setNullable(dto.getStartedAt(), builder::setStartedAt, DateUtils::formatDateTime);
    setNullable(computeExecutionTimeMs(dto), builder::setExecutionTimeMs);
    builder.setIncremental(componentDtoCache.hasIncrementalCharacteristic(dto.getUuid()));
    return builder.build();
  }

  public WsCe.Task formatActivity(DbSession dbSession, CeActivityDto dto, @Nullable String scannerContext) {
    return formatActivity(dto, DtoCache.forActivityDtos(dbClient, dbSession, singletonList(dto)), scannerContext);
  }

  public List<WsCe.Task> formatActivity(DbSession dbSession, List<CeActivityDto> dtos) {
    DtoCache cache = DtoCache.forActivityDtos(dbClient, dbSession, dtos);
    return dtos.stream()
      .map(input -> formatActivity(input, cache, null))
      .collect(MoreCollectors.toList(dtos.size()));
  }

  private static WsCe.Task formatActivity(CeActivityDto dto, DtoCache componentDtoCache, @Nullable String scannerContext) {
    WsCe.Task.Builder builder = WsCe.Task.newBuilder();
    String organizationKey = componentDtoCache.getOrganizationKey(dto.getComponentUuid());
    // FIXME organization field should be set from the CeActivityDto rather than from the ComponentDto
    setNullable(organizationKey, builder::setOrganization);
    builder.setId(dto.getUuid());
    builder.setStatus(WsCe.TaskStatus.valueOf(dto.getStatus().name()));
    builder.setType(dto.getTaskType());
    builder.setLogs(false);
    setNullable(dto.getComponentUuid(), uuid -> buildComponent(builder, componentDtoCache.getComponent(uuid)).setComponentId(uuid));
    String analysisUuid = dto.getAnalysisUuid();
    if (analysisUuid != null) {
      builder.setAnalysisId(analysisUuid);
    }
    SnapshotDto analysis = analysisUuid == null ? null : componentDtoCache.getAnalysis(analysisUuid);
    builder.setIncremental(analysis != null && analysis.getIncremental());
    setNullable(analysisUuid, builder::setAnalysisId);
    setNullable(dto.getSubmitterLogin(), builder::setSubmitterLogin);
    builder.setSubmittedAt(formatDateTime(new Date(dto.getSubmittedAt())));
    setNullable(dto.getStartedAt(), builder::setStartedAt, DateUtils::formatDateTime);
    setNullable(dto.getExecutedAt(), builder::setExecutedAt, DateUtils::formatDateTime);
    setNullable(dto.getExecutionTimeMs(), builder::setExecutionTimeMs);
    setNullable(dto.getErrorMessage(), builder::setErrorMessage);
    setNullable(dto.getErrorStacktrace(), builder::setErrorStacktrace);
    setNullable(scannerContext, builder::setScannerContext);
    builder.setHasScannerContext(dto.isHasScannerContext());
    return builder.build();
  }

  private static WsCe.Task.Builder buildComponent(WsCe.Task.Builder builder, @Nullable ComponentDto componentDto) {
    if (componentDto != null) {
      builder.setComponentKey(componentDto.getDbKey());
      builder.setComponentName(componentDto.name());
      builder.setComponentQualifier(componentDto.qualifier());
    }
    return builder;
  }

  private static class DtoCache {
    private final Map<String, ComponentDto> componentsByUuid;
    private final Map<String, OrganizationDto> organizationsByUuid;
    private final Map<String, SnapshotDto> analysisByUuid;
    private final Multimap<String, CeTaskCharacteristicDto> characteristicsByTaskUuid;

    private DtoCache(Map<String, ComponentDto> componentsByUuid, Map<String, OrganizationDto> organizationsByUuid, Map<String, SnapshotDto> analysisByUuid,
      Multimap<String, CeTaskCharacteristicDto> characteristicsByTaskUuid) {
      this.componentsByUuid = componentsByUuid;
      this.organizationsByUuid = organizationsByUuid;
      this.analysisByUuid = analysisByUuid;
      this.characteristicsByTaskUuid = characteristicsByTaskUuid;
    }

    static DtoCache forQueueDtos(DbClient dbClient, DbSession dbSession, Collection<CeQueueDto> ceQueueDtos) {
      Map<String, ComponentDto> componentsByUuid = dbClient.componentDao().selectByUuids(dbSession, uuidOfCeQueueDtos(ceQueueDtos))
        .stream()
        .collect(MoreCollectors.uniqueIndex(ComponentDto::uuid));
      Multimap<String, CeTaskCharacteristicDto> characteristicsByTaskUuid = dbClient.ceTaskCharacteristicsDao()
        .selectByTaskUuids(dbSession, ceQueueDtos.stream().map(CeQueueDto::getUuid).collect(Collectors.toList()))
        .stream().collect(MoreCollectors.index(CeTaskCharacteristicDto::getTaskUuid));
      return new DtoCache(componentsByUuid, buildOrganizationsByUuid(dbClient, dbSession, componentsByUuid), Collections.emptyMap(), characteristicsByTaskUuid);
    }

    private static Set<String> uuidOfCeQueueDtos(Collection<CeQueueDto> ceQueueDtos) {
      return ceQueueDtos.stream()
        .filter(Objects::nonNull)
        .map(CeQueueDto::getComponentUuid)
        .filter(Objects::nonNull)
        .collect(MoreCollectors.toSet(ceQueueDtos.size()));
    }

    static DtoCache forActivityDtos(DbClient dbClient, DbSession dbSession, Collection<CeActivityDto> ceActivityDtos) {
      Map<String, ComponentDto> componentsByUuid = dbClient.componentDao().selectByUuids(
        dbSession,
        uuidOfCeActivityDtos(ceActivityDtos))
        .stream()
        .collect(MoreCollectors.uniqueIndex(ComponentDto::uuid));
      Set<String> analysisUuids = ceActivityDtos.stream().map(CeActivityDto::getAnalysisUuid).filter(Objects::nonNull).collect(MoreCollectors.toSet());
      Map<String, SnapshotDto> analysisByUuid = dbClient.snapshotDao().selectByUuids(dbSession, analysisUuids).stream().collect(MoreCollectors.uniqueIndex(SnapshotDto::getUuid));
      return new DtoCache(componentsByUuid, buildOrganizationsByUuid(dbClient, dbSession, componentsByUuid), analysisByUuid,
        ImmutableMultimap.<String, CeTaskCharacteristicDto>builder().build());
    }

    private static Set<String> uuidOfCeActivityDtos(Collection<CeActivityDto> ceActivityDtos) {
      return ceActivityDtos.stream()
        .filter(Objects::nonNull)
        .map(CeActivityDto::getComponentUuid)
        .filter(Objects::nonNull)
        .collect(MoreCollectors.toSet(ceActivityDtos.size()));
    }

    private static Map<String, OrganizationDto> buildOrganizationsByUuid(DbClient dbClient, DbSession dbSession, Map<String, ComponentDto> componentsByUuid) {
      return dbClient.organizationDao().selectByUuids(
        dbSession,
        componentsByUuid.values().stream()
          .map(ComponentDto::getOrganizationUuid)
          .collect(MoreCollectors.toSet(componentsByUuid.size())))
        .stream()
        .collect(MoreCollectors.uniqueIndex(OrganizationDto::getUuid));
    }

    @CheckForNull
    ComponentDto getComponent(@Nullable String uuid) {
      if (uuid == null) {
        return null;
      }
      return componentsByUuid.get(uuid);
    }

    @CheckForNull
    String getOrganizationKey(@Nullable String componentUuid) {
      if (componentUuid == null) {
        return null;
      }
      ComponentDto componentDto = componentsByUuid.get(componentUuid);
      if (componentDto == null) {
        return null;
      }
      String organizationUuid = componentDto.getOrganizationUuid();
      OrganizationDto organizationDto = organizationsByUuid.get(organizationUuid);
      checkState(organizationDto != null, "Organization with uuid '%s' not found", organizationUuid);
      return organizationDto.getKey();
    }

    @CheckForNull
    SnapshotDto getAnalysis(String analysisUuid) {
      return analysisByUuid.get(analysisUuid);
    }

    boolean hasIncrementalCharacteristic(String taskUuid) {
      return characteristicsByTaskUuid.get(taskUuid).stream()
        .filter(c -> c.getKey().equals(CeTaskCharacteristicDto.INCREMENTAL_KEY))
        .anyMatch(c -> c.getValue().equals("true"));
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
