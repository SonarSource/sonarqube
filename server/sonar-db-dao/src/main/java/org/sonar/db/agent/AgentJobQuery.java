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
package org.sonar.db.agent;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.DatabaseUtils;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Db query used for the {@code AGENT_JOBS} table.
 */
public class AgentJobQuery {

  /**
   * Oracle caps a bind list ({@code IN (...)}) at 1000 elements ({@code ORA-01795}). {@code jobIds} is the only
   * filter here backed by unbounded user input (statuses/agentTypes are small closed vocabularies), so it's the
   * only one that needs this guard — see {@link #isShortCircuitedByJobIds()}.
   */
  public static final int MAX_JOB_IDS = DatabaseUtils.PARTITION_SIZE_FOR_ORACLE;

  // a public implementation of List must be used in MyBatis - potential concurrency exceptions otherwise
  @Nullable
  private ArrayList<String> jobIds;
  @Nullable
  private ArrayList<String> statuses;
  @Nullable
  private ArrayList<String> agentTypes;
  @Nullable
  private Long minCreatedAt;
  @Nullable
  private Long maxCreatedAt;

  public AgentJobQuery() {
    // no-op: all filters are optional and set via the fluent setters below
  }

  @CheckForNull
  public List<String> getJobIds() {
    return jobIds;
  }

  public AgentJobQuery setJobIds(@Nullable List<String> jobIds) {
    this.jobIds = jobIds == null ? null : newArrayList(jobIds);
    return this;
  }

  /**
   * Mirrors {@code CeTaskQuery.isShortCircuitedByEntityUuids()}: rather than partitioning {@code jobIds} across
   * several {@code IN} queries (which would need to re-merge and re-sort/re-paginate results — not worth the
   * complexity here), callers should skip the query entirely and treat the result as empty when this is true. An
   * explicitly empty list is also short-circuited, since it represents "resolved to no ids" rather than "no filter".
   */
  public boolean isShortCircuitedByJobIds() {
    return jobIds != null && (jobIds.isEmpty() || jobIds.size() > MAX_JOB_IDS);
  }

  @CheckForNull
  public List<String> getStatuses() {
    return statuses;
  }

  public AgentJobQuery setStatuses(@Nullable List<String> statuses) {
    this.statuses = statuses == null ? null : newArrayList(statuses);
    return this;
  }

  @CheckForNull
  public List<String> getAgentTypes() {
    return agentTypes;
  }

  public AgentJobQuery setAgentTypes(@Nullable List<String> agentTypes) {
    this.agentTypes = agentTypes == null ? null : newArrayList(agentTypes);
    return this;
  }

  @CheckForNull
  public Long getMinCreatedAt() {
    return minCreatedAt;
  }

  public AgentJobQuery setMinCreatedAt(@Nullable Long minCreatedAt) {
    this.minCreatedAt = minCreatedAt;
    return this;
  }

  @CheckForNull
  public Long getMaxCreatedAt() {
    return maxCreatedAt;
  }

  public AgentJobQuery setMaxCreatedAt(@Nullable Long maxCreatedAt) {
    this.maxCreatedAt = maxCreatedAt;
    return this;
  }
}
