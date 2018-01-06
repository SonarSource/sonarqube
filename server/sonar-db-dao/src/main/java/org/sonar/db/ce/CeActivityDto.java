/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

public class CeActivityDto {

  private static final int MAX_SIZE_ERROR_MESSAGE = 1000;

  public enum Status {
    SUCCESS, FAILED, CANCELED
  }

  private String uuid;
  private String componentUuid;
  private String analysisUuid;
  private Status status;
  private String taskType;
  private boolean isLast;
  private String isLastKey;
  private String submitterLogin;
  private String workerUuid;
  private int executionCount;
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
   * </p>
   */
  private boolean hasScannerContext;

  CeActivityDto() {
    // required for MyBatis
  }

  public CeActivityDto(CeQueueDto queueDto) {
    this.uuid = queueDto.getUuid();
    this.taskType = queueDto.getTaskType();
    this.componentUuid = queueDto.getComponentUuid();
    this.isLastKey = format("%s%s", taskType, Strings.nullToEmpty(componentUuid));
    this.submitterLogin = queueDto.getSubmitterLogin();
    this.workerUuid = queueDto.getWorkerUuid();
    this.executionCount = queueDto.getExecutionCount();
    this.submittedAt = queueDto.getCreatedAt();
    this.startedAt = queueDto.getStartedAt();
  }

  public String getUuid() {
    return uuid;
  }

  public CeActivityDto setUuid(String s) {
    checkArgument(s.length() <= 40, "Value is too long for column CE_ACTIVITY.UUID: %s", s);
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
    checkArgument(s == null || s.length() <= 40, "Value is too long for column CE_ACTIVITY.COMPONENT_UUID: %s", s);
    this.componentUuid = s;
    return this;
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
    return this;
  }

  public String getIsLastKey() {
    return isLastKey;
  }

  @CheckForNull
  public String getSubmitterLogin() {
    return submitterLogin;
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

  public int getExecutionCount() {
    return executionCount;
  }

  public CeActivityDto setExecutionCount(int executionCount) {
    this.executionCount = executionCount;
    return this;
  }

  @CheckForNull
  public String getErrorMessage() {
    return errorMessage;
  }

  public CeActivityDto setErrorMessage(@Nullable String errorMessage) {
    this.errorMessage = ensureNotTooBig(errorMessage, MAX_SIZE_ERROR_MESSAGE);
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
  public String getErrorStacktrace() {
    return errorStacktrace;
  }

  @CheckForNull
  public CeActivityDto setErrorStacktrace(@Nullable String errorStacktrace) {
    this.errorStacktrace = errorStacktrace;
    return this;
  }

  public boolean isHasScannerContext() {
    return hasScannerContext;
  }

  protected void setHasScannerContext(boolean hasScannerContext) {
    this.hasScannerContext = hasScannerContext;
  }

  @Override
  public String toString() {
    return "CeActivityDto{" +
      "uuid='" + uuid + '\'' +
      ", componentUuid='" + componentUuid + '\'' +
      ", analysisUuid='" + analysisUuid + '\'' +
      ", status=" + status +
      ", taskType='" + taskType + '\'' +
      ", isLast=" + isLast +
      ", isLastKey='" + isLastKey + '\'' +
      ", submitterLogin='" + submitterLogin + '\'' +
      ", workerUuid='" + workerUuid + '\'' +
      ", executionCount=" + executionCount +
      ", submittedAt=" + submittedAt +
      ", startedAt=" + startedAt +
      ", executedAt=" + executedAt +
      ", createdAt=" + createdAt +
      ", updatedAt=" + updatedAt +
      ", executionTimeMs=" + executionTimeMs +
      ", errorMessage='" + errorMessage + '\'' +
      ", errorStacktrace='" + errorStacktrace + '\'' +
      ", hasScannerContext=" + hasScannerContext +
      '}';
  }
}
