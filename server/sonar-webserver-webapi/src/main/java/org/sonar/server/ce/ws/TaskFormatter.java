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
package org.sonar.server.ce.ws;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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
import org.sonar.db.ce.CeTaskMessageDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.dismissmessage.MessageType;
import org.sonar.db.user.UserDto;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.Common;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.core.ce.CeTaskCharacteristics.BRANCH;
import static org.sonar.core.ce.CeTaskCharacteristics.BRANCH_TYPE;
import static org.sonar.core.ce.CeTaskCharacteristics.PULL_REQUEST;

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

  public List<Ce.Task> formatQueue(DbSession dbSession, List<CeQueueDto> dtos) {
    DtoCache cache = DtoCache.forQueueDtos(dbClient, dbSession, dtos);
    return dtos.stream().map(input -> formatQueue(input, cache)).toList();
  }

  public Ce.Task formatQueue(DbSession dbSession, CeQueueDto queue) {
    return formatQueue(queue, DtoCache.forQueueDtos(dbClient, dbSession, singletonList(queue)));
  }

  private Ce.Task formatQueue(CeQueueDto dto, DtoCache cache) {
    Ce.Task.Builder builder = Ce.Task.newBuilder();
    if (dto.getComponentUuid() != null) {
      builder.setComponentId(dto.getComponentUuid());
      setComponent(builder, dto.getComponentUuid(), cache);
    }
    builder.setId(dto.getUuid());
    builder.setStatus(Ce.TaskStatus.valueOf(dto.getStatus().name()));
    builder.setType(dto.getTaskType());
    cache.getUser(dto.getSubmitterUuid()).ifPresent(user -> builder.setSubmitterLogin(user.getLogin()));
    builder.setSubmittedAt(formatDateTime(new Date(dto.getCreatedAt())));
    ofNullable(dto.getStartedAt()).map(DateUtils::formatDateTime).ifPresent(builder::setStartedAt);
    ofNullable(computeExecutionTimeMs(dto)).ifPresent(builder::setExecutionTimeMs);
    setBranchOrPullRequest(builder, dto.getUuid(), cache);
    return builder.build();
  }

  public Ce.Task formatActivity(DbSession dbSession, CeActivityDto dto, @Nullable String scannerContext) {
    return formatActivity(dto, DtoCache.forActivityDtos(dbClient, dbSession, singletonList(dto)), scannerContext);
  }

  public List<Ce.Task> formatActivity(DbSession dbSession, List<CeActivityDto> dtos) {
    DtoCache cache = DtoCache.forActivityDtos(dbClient, dbSession, dtos);
    return dtos.stream()
      .map(input -> formatActivity(input, cache, null))
      .toList();
  }

  private static Ce.Task formatActivity(CeActivityDto activityDto, DtoCache cache, @Nullable String scannerContext) {
    Ce.Task.Builder builder = Ce.Task.newBuilder();
    builder.setId(activityDto.getUuid());
    builder.setStatus(Ce.TaskStatus.valueOf(activityDto.getStatus().name()));
    builder.setType(activityDto.getTaskType());
    ofNullable(activityDto.getNodeName()).ifPresent(builder::setNodeName);
    ofNullable(activityDto.getComponentUuid()).ifPresent(uuid -> setComponent(builder, uuid, cache).setComponentId(uuid));
    String analysisUuid = activityDto.getAnalysisUuid();
    ofNullable(analysisUuid).ifPresent(builder::setAnalysisId);
    setBranchOrPullRequest(builder, activityDto.getUuid(), cache);
    ofNullable(analysisUuid).ifPresent(builder::setAnalysisId);
    cache.getUser(activityDto.getSubmitterUuid()).ifPresent(user -> builder.setSubmitterLogin(user.getLogin()));
    builder.setSubmittedAt(formatDateTime(new Date(activityDto.getSubmittedAt())));
    ofNullable(activityDto.getStartedAt()).map(DateUtils::formatDateTime).ifPresent(builder::setStartedAt);
    ofNullable(activityDto.getExecutedAt()).map(DateUtils::formatDateTime).ifPresent(builder::setExecutedAt);
    ofNullable(activityDto.getExecutionTimeMs()).ifPresent(builder::setExecutionTimeMs);
    ofNullable(activityDto.getErrorMessage()).ifPresent(builder::setErrorMessage);
    ofNullable(activityDto.getErrorStacktrace()).ifPresent(builder::setErrorStacktrace);
    ofNullable(activityDto.getErrorType()).ifPresent(builder::setErrorType);
    ofNullable(scannerContext).ifPresent(builder::setScannerContext);
    builder.setHasScannerContext(activityDto.isHasScannerContext());
    List<String> warnings = extractWarningMessages(activityDto);
    builder.setWarningCount(warnings.size());
    warnings.forEach(builder::addWarnings);

    List<String> infoMessages = extractInfoMessages(activityDto);
    builder.addAllInfoMessages(infoMessages);

    return builder.build();
  }

  private static Ce.Task.Builder setComponent(Ce.Task.Builder builder, @Nullable String componentUuid, DtoCache componentDtoCache) {
    ComponentDto componentDto = componentDtoCache.getComponent(componentUuid);
    if (componentDto == null) {
      return builder;
    }
    builder.setComponentKey(componentDto.getKey());
    builder.setComponentName(componentDto.name());
    builder.setComponentQualifier(componentDto.qualifier());
    return builder;
  }

  private static Ce.Task.Builder setBranchOrPullRequest(Ce.Task.Builder builder, String taskUuid, DtoCache componentDtoCache) {
    componentDtoCache.getBranchKey(taskUuid).ifPresent(
      b -> {
        Common.BranchType branchType = componentDtoCache.getBranchType(taskUuid)
          .orElseThrow(() -> new IllegalStateException(format("Could not find branch type of task '%s'", taskUuid)));
        switch (branchType) {
          case BRANCH:
            builder.setBranchType(branchType);
            builder.setBranch(b);
            break;
          default:
            throw new IllegalStateException(String.format("Unknown branch type '%s'", branchType));
        }
      });
    componentDtoCache.getPullRequest(taskUuid).ifPresent(builder::setPullRequest);
    return builder;
  }

  private static List<String> extractWarningMessages(CeActivityDto dto) {
    return dto.getCeTaskMessageDtos().stream()
      .filter(ceTaskMessageDto -> ceTaskMessageDto.getType().isWarning())
      .map(CeTaskMessageDto::getMessage)
      .toList();
  }

  private static List<String> extractInfoMessages(CeActivityDto activityDto) {
    return activityDto.getCeTaskMessageDtos().stream()
      .filter(ceTaskMessageDto -> MessageType.INFO.equals(ceTaskMessageDto.getType()))
      .sorted(Comparator.comparing(CeTaskMessageDto::getCreatedAt))
      .map(CeTaskMessageDto::getMessage)
      .toList();
  }

  private static class DtoCache {
    private final Map<String, ComponentDto> componentsByUuid;
    private final Multimap<String, CeTaskCharacteristicDto> characteristicsByTaskUuid;
    private final Map<String, UserDto> usersByUuid;

    private DtoCache(Map<String, ComponentDto> componentsByUuid, Multimap<String, CeTaskCharacteristicDto> characteristicsByTaskUuid, Map<String, UserDto> usersByUuid) {
      this.componentsByUuid = componentsByUuid;
      this.characteristicsByTaskUuid = characteristicsByTaskUuid;
      this.usersByUuid = usersByUuid;
    }

    static DtoCache forQueueDtos(DbClient dbClient, DbSession dbSession, Collection<CeQueueDto> ceQueueDtos) {
      Map<String, ComponentDto> componentsByUuid = dbClient.componentDao().selectByUuids(dbSession, componentUuidsOfCeQueues(ceQueueDtos))
        .stream()
        .collect(Collectors.toMap(ComponentDto::uuid, Function.identity()));
      Multimap<String, CeTaskCharacteristicDto> characteristicsByTaskUuid = dbClient.ceTaskCharacteristicsDao()
        .selectByTaskUuids(dbSession, ceQueueDtos.stream().map(CeQueueDto::getUuid).toList())
        .stream().collect(MoreCollectors.index(CeTaskCharacteristicDto::getTaskUuid));
      Set<String> submitterUuids = ceQueueDtos.stream().map(CeQueueDto::getSubmitterUuid).filter(Objects::nonNull).collect(Collectors.toSet());
      Map<String, UserDto> usersByUuid = dbClient.userDao().selectByUuids(dbSession, submitterUuids).stream().collect(Collectors.toMap(UserDto::getUuid, Function.identity()));
      return new DtoCache(componentsByUuid, characteristicsByTaskUuid, usersByUuid);
    }

    private static Set<String> componentUuidsOfCeQueues(Collection<CeQueueDto> ceQueueDtos) {
      return ceQueueDtos.stream()
        .filter(Objects::nonNull)
        .map(CeQueueDto::getComponentUuid)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    }

    static DtoCache forActivityDtos(DbClient dbClient, DbSession dbSession, Collection<CeActivityDto> ceActivityDtos) {
      Map<String, ComponentDto> componentsByUuid = dbClient.componentDao().selectByUuids(
        dbSession,
        getComponentUuidsOfCeActivities(ceActivityDtos))
        .stream()
        .collect(Collectors.toMap(ComponentDto::uuid, Function.identity()));
      Multimap<String, CeTaskCharacteristicDto> characteristicsByTaskUuid = dbClient.ceTaskCharacteristicsDao()
        .selectByTaskUuids(dbSession, ceActivityDtos.stream().map(CeActivityDto::getUuid).toList())
        .stream().collect(MoreCollectors.index(CeTaskCharacteristicDto::getTaskUuid));
      Set<String> submitterUuids = ceActivityDtos.stream().map(CeActivityDto::getSubmitterUuid).filter(Objects::nonNull).collect(Collectors.toSet());
      Map<String, UserDto> usersByUuid = dbClient.userDao().selectByUuids(dbSession, submitterUuids).stream().collect(Collectors.toMap(UserDto::getUuid, Function.identity()));
      return new DtoCache(componentsByUuid, characteristicsByTaskUuid, usersByUuid);
    }

    private static Set<String> getComponentUuidsOfCeActivities(Collection<CeActivityDto> ceActivityDtos) {
      return ceActivityDtos.stream()
        .filter(Objects::nonNull)
        .map(CeActivityDto::getComponentUuid)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    }

    @CheckForNull
    ComponentDto getComponent(@Nullable String uuid) {
      if (uuid == null) {
        return null;
      }
      return componentsByUuid.get(uuid);
    }

    Optional<String> getBranchKey(String taskUuid) {
      return characteristicsByTaskUuid.get(taskUuid).stream()
        .filter(c -> c.getKey().equals(BRANCH))
        .map(CeTaskCharacteristicDto::getValue)
        .findAny();
    }

    Optional<Common.BranchType> getBranchType(String taskUuid) {
      return characteristicsByTaskUuid.get(taskUuid).stream()
        .filter(c -> c.getKey().equals(BRANCH_TYPE))
        .map(c -> Common.BranchType.valueOf(c.getValue()))
        .findAny();
    }

    Optional<String> getPullRequest(String taskUuid) {
      return characteristicsByTaskUuid.get(taskUuid).stream()
        .filter(c -> c.getKey().equals(PULL_REQUEST))
        .map(CeTaskCharacteristicDto::getValue)
        .findAny();
    }

    Optional<UserDto> getUser(@Nullable String userUuid) {
      if (userUuid == null) {
        return Optional.empty();
      }
      return Optional.ofNullable(usersByUuid.get(userUuid));
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
