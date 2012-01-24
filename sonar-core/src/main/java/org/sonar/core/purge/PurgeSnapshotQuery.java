/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.purge;

import java.util.Date;

public final class PurgeSnapshotQuery {
  private Long rootProjectId;
  private Date beforeBuildDate;
  private String[] scopes;
  private String[] qualifiers;
  private String[] status;

  private PurgeSnapshotQuery() {
  }

  public static PurgeSnapshotQuery create() {
    return new PurgeSnapshotQuery();
  }

  public Long getRootProjectId() {
    return rootProjectId;
  }

  public PurgeSnapshotQuery setRootProjectId(Long rootProjectId) {
    this.rootProjectId = rootProjectId;
    return this;
  }

  public Date getBeforeBuildDate() {
    return beforeBuildDate;
  }

  public PurgeSnapshotQuery setBeforeBuildDate(Date beforeBuildDate) {
    this.beforeBuildDate = beforeBuildDate;
    return this;
  }

  public String[] getScopes() {
    return scopes;
  }

  public PurgeSnapshotQuery setScopes(String[] scopes) {
    this.scopes = scopes;
    return this;
  }

  public String[] getQualifiers() {
    return qualifiers;
  }

  public PurgeSnapshotQuery setQualifiers(String[] qualifiers) {
    this.qualifiers = qualifiers;
    return this;
  }

  public String[] getStatus() {
    return status;
  }

  public PurgeSnapshotQuery setStatus(String[] status) {
    this.status = status;
    return this;
  }
}
