/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.purge;

public final class PurgeSnapshotQuery {
  private Long id;
  private Long rootProjectId;
  private Long rootSnapshotId;
  private Long resourceId;
  private String[] scopes;
  private String[] qualifiers;
  private String[] status;
  private Boolean islast;
  private Boolean notPurged;
  private Boolean withVersionEvent;

  private PurgeSnapshotQuery() {
  }

  public static PurgeSnapshotQuery create() {
    return new PurgeSnapshotQuery();
  }

  public Long getId() {
    return id;
  }

  public PurgeSnapshotQuery setId(Long l) {
    this.id = l;
    return this;
  }

  public Long getRootProjectId() {
    return rootProjectId;
  }

  public PurgeSnapshotQuery setRootProjectId(Long rootProjectId) {
    this.rootProjectId = rootProjectId;
    return this;
  }

  public String[] getScopes() {
    return scopes;// NOSONAR May expose internal representation by returning reference to mutable object
  }

  public PurgeSnapshotQuery setScopes(String[] scopes) {
    this.scopes = scopes; // NOSONAR May expose internal representation by incorporating reference to mutable object
    return this;
  }

  public String[] getQualifiers() {
    return qualifiers;// NOSONAR May expose internal representation by returning reference to mutable object
  }

  public PurgeSnapshotQuery setQualifiers(String[] qualifiers) {
    this.qualifiers = qualifiers;// NOSONAR May expose internal representation by incorporating reference to mutable object
    return this;
  }

  public String[] getStatus() {
    return status;// NOSONAR May expose internal representation by returning reference to mutable object
  }

  public PurgeSnapshotQuery setStatus(String[] status) {
    this.status = status; // NOSONAR org.sonar.db.purge.PurgeSnapshotQuery.setStatus(String[]) may expose internal representation
    return this;
  }

  public Boolean getIslast() {
    return islast;
  }

  public PurgeSnapshotQuery setIslast(Boolean islast) {
    this.islast = islast;
    return this;
  }

  public Boolean getNotPurged() {
    return notPurged;
  }

  public PurgeSnapshotQuery setNotPurged(Boolean notPurged) {
    this.notPurged = notPurged;
    return this;
  }

  public Long getRootSnapshotId() {
    return rootSnapshotId;
  }

  public PurgeSnapshotQuery setRootSnapshotId(Long rootSnapshotId) {
    this.rootSnapshotId = rootSnapshotId;
    return this;
  }

  public Long getResourceId() {
    return resourceId;
  }

  public PurgeSnapshotQuery setResourceId(Long l) {
    this.resourceId = l;
    return this;
  }

  public Boolean getWithVersionEvent() {
    return withVersionEvent;
  }

  public PurgeSnapshotQuery setWithVersionEvent(Boolean withVersionEvent) {
    this.withVersionEvent = withVersionEvent;
    return this;
  }
}
