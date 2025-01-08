/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.migrationlog;

public class MigrationLogDto {

  private String uuid;

  private String step;

  private Long durationInMs;

  private boolean success;

  private Long startedAt;

  private String targetVersion;

  public MigrationLogDto() {
    // default constructor
  }

  public String getUuid() {
    return uuid;
  }

  public MigrationLogDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getStep() {
    return step;
  }

  public MigrationLogDto setStep(String step) {
    this.step = step;
    return this;
  }

  public Long getDurationInMs() {
    return durationInMs;
  }

  public MigrationLogDto setDurationInMs(Long durationInMs) {
    this.durationInMs = durationInMs;
    return this;
  }

  public boolean isSuccess() {
    return success;
  }

  public MigrationLogDto setSuccess(boolean success) {
    this.success = success;
    return this;
  }

  public Long getStartedAt() {
    return startedAt;
  }

  public MigrationLogDto setStartedAt(Long startedAt) {
    this.startedAt = startedAt;
    return this;
  }

  public String getTargetVersion() {
    return targetVersion;
  }

  public MigrationLogDto setTargetVersion(String targetVersion) {
    this.targetVersion = targetVersion;
    return this;
  }

}
