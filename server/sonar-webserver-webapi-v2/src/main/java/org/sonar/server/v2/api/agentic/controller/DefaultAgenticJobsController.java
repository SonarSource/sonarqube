/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.v2.api.agentic.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.agent.AgentJobDto;
import org.sonar.db.agent.AgentJobQuery;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.agentic.request.AgenticJobsSearchRestRequest;
import org.sonar.server.v2.api.agentic.response.AgenticJobRestResponse;
import org.sonar.server.v2.api.agentic.response.AgenticJobsSearchRestResponse;
import org.sonar.server.v2.api.model.RestPage;
import org.sonar.server.v2.api.response.PageRestResponse;

import static org.sonar.api.utils.DateUtils.parseEndingDateOrDateTime;
import static org.sonar.api.utils.DateUtils.parseStartingDateOrDateTime;

public class DefaultAgenticJobsController implements AgenticJobsController {

  private static final Set<String> VALID_AGENT_TYPES = Set.of("HUNTER", "REMEDIATION");
  private static final String STATUS_PENDING = "PENDING";
  private static final String STATUS_FAILED = "FAILED";

  private final UserSession userSession;
  private final DbClient dbClient;

  public DefaultAgenticJobsController(UserSession userSession, DbClient dbClient) {
    this.userSession = userSession;
    this.dbClient = dbClient;
  }

  @Override
  public AgenticJobsSearchRestResponse search(AgenticJobsSearchRestRequest request, RestPage restPage) {
    userSession.checkLoggedIn().checkIsSystemAdministrator();

    AgentJobQuery query = toQuery(request);
    try (DbSession dbSession = dbClient.openSession(false)) {
      int total = dbClient.agentJobDao().countByQuery(dbSession, query);
      List<AgentJobDto> jobs = dbClient.agentJobDao().selectByQuery(dbSession, query, Pagination.forPage(restPage.pageIndex()).andSize(restPage.pageSize()));
      List<AgenticJobRestResponse> restJobs = jobs.stream().map(DefaultAgenticJobsController::toRestJob).toList();
      return new AgenticJobsSearchRestResponse(restJobs, new PageRestResponse(restPage.pageIndex(), restPage.pageSize(), total));
    }
  }

  private static AgentJobQuery toQuery(AgenticJobsSearchRestRequest request) {
    AgentJobQuery query = new AgentJobQuery();
    if (request.id() != null) {
      query.setJobIds(splitCsv(request.id()));
    }
    if (request.status() != null) {
      query.setStatuses(splitCsv(request.status()).stream().map(AgenticJobStatus::toDbStatus).toList());
    }
    if (request.type() != null) {
      List<String> types = splitCsv(request.type());
      types.stream().filter(type -> !VALID_AGENT_TYPES.contains(type)).findFirst()
        .ifPresent(invalid -> {
          throw BadRequestException.create("Invalid type value: " + invalid);
        });
      query.setAgentTypes(types);
    }
    Optional.ofNullable(parseStartingDateOrDateTime(request.createdAfter()))
      .map(date -> date.toInstant().toEpochMilli())
      .ifPresent(query::setMinCreatedAt);
    Optional.ofNullable(parseEndingDateOrDateTime(request.createdBefore()))
      .map(date -> date.toInstant().toEpochMilli())
      .ifPresent(query::setMaxCreatedAt);
    return query;
  }

  private static List<String> splitCsv(String csv) {
    return Arrays.stream(csv.split(","))
      .map(String::trim)
      .filter(s -> !s.isEmpty())
      .toList();
  }

  private static AgenticJobRestResponse toRestJob(AgentJobDto dto) {
    return new AgenticJobRestResponse(
      dto.getId(),
      dto.getProjectId(),
      dto.getProjectKey(),
      dto.getProjectName(),
      dto.getAgentType(),
      dto.getAnalysisType(),
      AgenticJobStatus.toApiStatus(dto.getStatus()),
      dto.getBranch(),
      dto.getRepositoryUrl(),
      dto.getRevision(),
      dto.getWorkflowType(),
      dto.getFindingsCount(),
      dto.getCreatedAt(),
      dto.getUpdatedAt(),
      dto.getStartedAt(),
      dto.getFinishedAt(),
      toFailureReason(dto),
      toErrorKey(dto));
  }

  @Nullable
  private static String toFailureReason(AgentJobDto dto) {
    if (!STATUS_FAILED.equals(dto.getStatus())) {
      return null;
    }
    return dto.getSubStatus() != null ? dto.getSubStatus() : dto.getErrorKey();
  }

  @Nullable
  private static String toErrorKey(AgentJobDto dto) {
    if (!STATUS_FAILED.equals(dto.getStatus())) {
      return null;
    }
    return dto.getErrorKey();
  }

  /**
   * Maps between the DB lifecycle vocabulary ({@code PENDING}/{@code RUNNING}/{@code SUCCEEDED}/{@code FAILED}) and the
   * SonarQube-facing API vocabulary ({@code PENDING}/{@code IN_PROGRESS}/{@code COMPLETED}/{@code FAILED}), consistent
   * with the existing {@code api/v2/remediation-agent/jobs} endpoint.
   */
  static final class AgenticJobStatus {

    private AgenticJobStatus() {
    }

    static String toDbStatus(String apiStatus) {
      return switch (apiStatus) {
        case STATUS_PENDING -> STATUS_PENDING;
        case "IN_PROGRESS" -> "RUNNING";
        case "COMPLETED" -> "SUCCEEDED";
        case STATUS_FAILED -> STATUS_FAILED;
        default -> throw BadRequestException.create("Invalid status value: " + apiStatus);
      };
    }

    static String toApiStatus(String dbStatus) {
      return switch (dbStatus) {
        case STATUS_PENDING -> STATUS_PENDING;
        case "RUNNING" -> "IN_PROGRESS";
        case "SUCCEEDED" -> "COMPLETED";
        case STATUS_FAILED -> STATUS_FAILED;
        default -> dbStatus;
      };
    }
  }
}
