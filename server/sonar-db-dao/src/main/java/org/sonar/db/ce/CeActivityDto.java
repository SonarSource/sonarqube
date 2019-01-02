/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.ce;

import com.google.common.base.Strings;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

public class CeActivityDto {

  private static final int MAX_SIZE_ERROR_MESSAGE = 1000;

  public enum Status {
    SUCCESS, FAILED, CANCELED
  }

  private String uuid;
  /**
   * Can be {@code null} when task is not associated to any data in table PROJECTS, but must always be non {@code null}
   * at the same time as {@link #mainComponentUuid}.
   * <p>
   * The component uuid of a any component (project or not) is its own UUID.
   */
  private String componentUuid;
  /**
   * Can be {@code null} when task is not associated to any data in table PROJECTS, but must always be non {@code null}
   * at the same time as {@link #componentUuid}.
   * <p>
   * The main component uuid of the main branch of project is its own UUID. For other branches of a project, it is the
   * project UUID of the main branch of that project ({@link ComponentDto#getMainBranchProjectUuid()}).
   */
  private String mainComponentUuid;
  private String analysisUuid;
  private Status status;
  private String taskType;
  private boolean isLast;
  private String isLastKey;
  private boolean mainIsLast;
  private String mainIsLastKey;
  private String submitterUuid;
  private String workerUuid;
  private long submittedAt;
  private Long startedAt;
  private Long executedAt;
  private long createdAt;
  private long updatedAt;
  private Long executionTimeMs;
  /**
   * The error message of the activity. Shall be non null only when status is FAILED. When status is FAILED, can be null
   * (eg. for activity created before the column has been introduced).
   * <p>
   * This property is populated when inserting <strong>AND when reading</strong>
   * </p>
   */
  private String errorMessage;
  /**
   * The error stacktrace (if any). Shall be non null only when status is FAILED. When status is FAILED, can be null
   * because exception such as MessageException do not have a stacktrace (ie. functional exceptions).
   * <p>
   * This property can be populated when inserting but <strong>is populated only when reading by a specific UUID.</strong>
   * </p>
   *
   * @see CeActivityDao#selectByUuid(DbSession, String)
   */
  private String errorStacktrace;

  /**
   * Optional free-text type of error. It may be set only when {@link #errorMessage} is not null.
   */
  @Nullable
  private String errorType;

  /**
   * Flag indicating whether the analysis of the current activity has a scanner context or not.
   * <p>
   * This property can not be populated when inserting but <strong>is populated when reading</strong>.
   */
  private boolean hasScannerContext;
  /**
   * Count of warnings attached to the current activity.
   * <p>
   * This property can not be populated when inserting but <strong>is populated when retrieving the activity by UUID</strong>.
   */
  private int warningCount = 0;

  CeActivityDto() {
    // required for MyBatis
  }

  public CeActivityDto(CeQueueDto queueDto) {
    this.uuid = queueDto.getUuid();
    this.taskType = queueDto.getTaskType();
    this.componentUuid = queueDto.getComponentUuid();
    this.mainComponentUuid = queueDto.getMainComponentUuid();
    this.isLastKey = format("%s%s", taskType, Strings.nullToEmpty(componentUuid));
    this.mainIsLastKey = format("%s%s", taskType, Strings.nullToEmpty(mainComponentUuid));
    this.submitterUuid = queueDto.getSubmitterUuid();
    this.workerUuid = queueDto.getWorkerUuid();
    this.submittedAt = queueDto.getCreatedAt();
    this.startedAt = queueDto.getStartedAt();
  }

  public String getUuid() {
    return uuid;
  }

  public CeActivityDto setUuid(@Nullable String s) {
    validateUuid(s, "UUID");
    this.uuid = s;
    return this;
  }

  public String getTaskType() {
    return taskType;
  }

  public CeActivityDto setTaskType(String s) {
    this.taskType = s;
    return this;
  }

  @CheckForNull
  public String getComponentUuid() {
    return componentUuid;
  }

  public CeActivityDto setComponentUuid(@Nullable String s) {
    validateUuid(s, "COMPONENT_UUID");
    this.componentUuid = s;
    return this;
  }

  @CheckForNull
  public String getMainComponentUuid() {
    return mainComponentUuid;
  }

  public CeActivityDto setMainComponentUuid(@Nullable String s) {
    validateUuid(s, "MAIN_COMPONENT_UUID");
    this.mainComponentUuid = s;
    return this;
  }

  private static void validateUuid(@Nullable String s, String columnName) {
    checkArgument(s == null || s.length() <= 40, "Value is too long for column CE_ACTIVITY.%s: %s", columnName, s);
  }

  public Status getStatus() {
    return status;
  }

  public CeActivityDto setStatus(Status s) {
    this.status = s;
    return this;
  }

  public boolean getIsLast() {
    return isLast;
  }

  CeActivityDto setIsLast(boolean b) {
    this.isLast = b;
    this.mainIsLast = b;
    return this;
  }

  public String getIsLastKey() {
    return isLastKey;
  }

  public boolean getMainIsLast() {
    return mainIsLast;
  }

  public String getMainIsLastKey() {
    return mainIsLastKey;
  }

  @CheckForNull
  public String getSubmitterUuid() {
    return submitterUuid;
  }

  public long getSubmittedAt() {
    return submittedAt;
  }

  public CeActivityDto setSubmittedAt(long submittedAt) {
    this.submittedAt = submittedAt;
    return this;
  }

  @CheckForNull
  public Long getStartedAt() {
    return startedAt;
  }

  public CeActivityDto setStartedAt(@Nullable Long l) {
    this.startedAt = l;
    return this;
  }

  @CheckForNull
  public Long getExecutedAt() {
    return executedAt;
  }

  public CeActivityDto setExecutedAt(@Nullable Long l) {
    this.executedAt = l;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public CeActivityDto setCreatedAt(long l) {
    this.createdAt = l;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public CeActivityDto setUpdatedAt(long l) {
    this.updatedAt = l;
    return this;
  }

  @CheckForNull
  public Long getExecutionTimeMs() {
    return executionTimeMs;
  }

  public CeActivityDto setExecutionTimeMs(@Nullable Long l) {
    checkArgument(l == null || l >= 0, "Execution time must be positive: %s", l);
    this.executionTimeMs = l;
    return this;
  }

  @CheckForNull
  public String getAnalysisUuid() {
    return analysisUuid;
  }

  public CeActivityDto setAnalysisUuid(@Nullable String s) {
    this.analysisUuid = s;
    return this;
  }

  @CheckForNull
  public String getWorkerUuid() {
    return workerUuid;
  }

  public CeActivityDto setWorkerUuid(String workerUuid) {
    this.workerUuid = workerUuid;
    return this;
  }

  @CheckForNull
  public String getErrorMessage() {
    return errorMessage;
  }

  public CeActivityDto setErrorMessage(@Nullable String errorMessage) {
    this.errorMessage = ensureNotTooBig(removeCharZeros(errorMessage), MAX_SIZE_ERROR_MESSAGE);
    return this;
  }

  @CheckForNull
  public String getErrorType() {
    return errorType;
  }

  public CeActivityDto setErrorType(@Nullable String s) {
    this.errorType = ensureNotTooBig(s, 20);
    return this;
  }

  @CheckForNull
  public String getErrorStacktrace() {
    return errorStacktrace;
  }

  @CheckForNull
  public CeActivityDto setErrorStacktrace(@Nullable String errorStacktrace) {
    this.errorStacktrace = removeCharZeros(errorStacktrace);
    return this;
  }

  public boolean isHasScannerContext() {
    return hasScannerContext;
  }

  protected CeActivityDto setHasScannerContext(boolean hasScannerContext) {
    this.hasScannerContext = hasScannerContext;
    return this;
  }

  public int getWarningCount() {
    return warningCount;
  }

  protected CeActivityDto setWarningCount(int warningCount) {
    checkArgument(warningCount >= 0);
    this.warningCount = warningCount;
    return this;
  }

  @Override
  public String toString() {
    return "CeActivityDto{" +
      "uuid='" + uuid + '\'' +
      ", componentUuid='" + componentUuid + '\'' +
      ", mainComponentUuid='" + mainComponentUuid + '\'' +
      ", analysisUuid='" + analysisUuid + '\'' +
      ", status=" + status +
      ", taskType='" + taskType + '\'' +
      ", isLast=" + isLast +
      ", isLastKey='" + isLastKey + '\'' +
      ", mainIsLast=" + mainIsLast +
      ", mainIsLastKey='" + mainIsLastKey + '\'' +
      ", submitterUuid='" + submitterUuid + '\'' +
      ", workerUuid='" + workerUuid + '\'' +
      ", submittedAt=" + submittedAt +
      ", startedAt=" + startedAt +
      ", executedAt=" + executedAt +
      ", createdAt=" + createdAt +
      ", updatedAt=" + updatedAt +
      ", executionTimeMs=" + executionTimeMs +
      ", errorMessage='" + errorMessage + '\'' +
      ", errorStacktrace='" + errorStacktrace + '\'' +
      ", hasScannerContext=" + hasScannerContext +
      ", warningCount=" + warningCount +
      '}';
  }

  @CheckForNull
  private static String ensureNotTooBig(@Nullable String str, int maxSize) {
    if (str == null) {
      return null;
    }
    if (str.length() <= maxSize) {
      return str;
    }
    return str.substring(0, maxSize);
  }

  @CheckForNull
  private static String removeCharZeros(@Nullable String str) {
    if (str == null || str.isEmpty()) {
      return str;
    }
    return str.codePoints()
      .filter(c -> c != "\u0000".codePointAt(0))
      .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
      .toString();
  }
}
