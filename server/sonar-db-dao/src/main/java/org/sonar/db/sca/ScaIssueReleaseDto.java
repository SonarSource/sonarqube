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
package org.sonar.db.sca;

/**
 * Represents a many-to-many join between Software Composition Analysis (SCA) issue and a SCA release.
 *
 * @param uuid             primary key
 * @param scaIssueUuid               the UUID of the SCA issue
 * @param scaReleaseUuid             the UUID of the SCA release
 * @param severity                   the severity of the issue
 * @param createdAt                  timestamp of creation
 * @param updatedAt                  timestamp of most recent update
 */
public record ScaIssueReleaseDto(
  String uuid,
  String scaIssueUuid,
  String scaReleaseUuid,
  ScaSeverity severity,
  long createdAt,
  long updatedAt) {

  /**
   * This constructor makes it a little harder to get the issue and release uuids backward,
   * if you have the DTOs around to use it.
   */
  public ScaIssueReleaseDto(String uuid, ScaIssueDto scaIssueDto, ScaReleaseDto scaReleaseDto, ScaSeverity severity, long createdAt, long updatedAt) {
    this(uuid, scaIssueDto.uuid(), scaReleaseDto.uuid(), severity, createdAt, updatedAt);
  }

  public int severitySortKey() {
    return severity.databaseSortKey();
  }

  public Builder toBuilder() {
    return new Builder()
      .setUuid(this.uuid)
      .setScaIssueUuid(this.scaIssueUuid)
      .setScaReleaseUuid(this.scaReleaseUuid)
      .setSeverity(this.severity)
      .setCreatedAt(this.createdAt)
      .setUpdatedAt(this.updatedAt);
  }

  /**
   * Returns an object whose .equals and .hashCode would match that of another ScaIssueReleaseDto's
   * identity() if the two ScaIssueReleaseDto would count as duplicates within the sca_issues_releases
   * table.
   * This is different from the DTOs themselves being equal because some fields do not count in
   * the identity of the row, and can be updated while preserving the identity. The method just
   * returns Object and not a type, because it exists just to call .equals and .hashCode on.
   *
   * @return an object to be used for hashing and comparing ScaReleaseDto instances for identity
   */
  public Identity identity() {
    return new IdentityImpl(this);
  }

  public interface Identity {
  }

  private record IdentityImpl(String scaIssueUuid, String scaReleaseUuid) implements Identity {
    IdentityImpl(ScaIssueReleaseDto dto) {
      this(dto.scaIssueUuid(), dto.scaReleaseUuid());
    }
  }

  public static class Builder {
    private String uuid;
    private String scaIssueUuid;
    private String scaReleaseUuid;
    private ScaSeverity severity;
    private long createdAt;
    private long updatedAt;

    public Builder setUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder setScaIssueUuid(String scaIssueUuid) {
      this.scaIssueUuid = scaIssueUuid;
      return this;
    }

    public Builder setScaReleaseUuid(String scaReleaseUuid) {
      this.scaReleaseUuid = scaReleaseUuid;
      return this;
    }

    public Builder setSeverity(ScaSeverity severity) {
      this.severity = severity;
      return this;
    }

    public Builder setCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder setUpdatedAt(long updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public ScaIssueReleaseDto build() {
      return new ScaIssueReleaseDto(
        uuid, scaIssueUuid, scaReleaseUuid, severity, createdAt, updatedAt);
    }
  }
}
