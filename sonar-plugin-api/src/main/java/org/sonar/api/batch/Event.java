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
package org.sonar.api.batch;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.database.BaseIdentifiable;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.database.model.Snapshot;

import javax.persistence.*;

import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonar.api.utils.DateUtils.longToDate;

/**
 * @since 1.10
 */
@Entity
@Table(name = "events")
public class Event extends BaseIdentifiable {
  public static final String CATEGORY_VERSION = "Version";
  public static final String CATEGORY_ALERT = "Alert";
  public static final String CATEGORY_PROFILE = "Profile";

  @Column(name = "name", updatable = true, nullable = true, length = 400)
  private String name;

  @Column(name = "description", updatable = true, nullable = true, length = 4000)
  private String description;

  @Column(name = "category", updatable = true, nullable = true, length = 50)
  private String category;

  @Column(name = "event_date", updatable = true, nullable = false)
  private Long date;

  @Column(name = "created_at", updatable = true, nullable = false)
  private Long createdAt;

  @Column(name = "event_data", updatable = true, nullable = true)
  private String data;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "snapshot_id", updatable = true, nullable = true)
  private Snapshot snapshot;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "component_uuid", referencedColumnName = "uuid", insertable = false, updatable = false, nullable = false)
  private ResourceModel resource;

  @Column(name = "component_uuid", updatable = true, nullable = true)
  private String componentUuid;

  public Event() {
  }

  public Event(String name, String description, String category) {
    this.name = name;
    this.description = description;
    this.category = category;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public boolean isVersionCategory() {
    return CATEGORY_VERSION.equalsIgnoreCase(category);
  }

  public boolean isProfileCategory() {
    return CATEGORY_PROFILE.equalsIgnoreCase(category);
  }

  public Date getDate() {
    return longToDate(date);
  }

  public void setDate(Date date) {
    this.date = date.getTime();
  }

  public Snapshot getSnapshot() {
    return snapshot;
  }

  public Date getCreatedAt() {
    return new Date(createdAt);
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt.getTime();
  }

  public final void setSnapshot(Snapshot snapshot) {
    this.snapshot = checkNotNull(snapshot, "it is not possible to set a null snapshot linked to an event");
    this.date = snapshot.getCreatedAtMs();
//    this.resourceId = snapshot.getResourceId();
  }

  public ResourceModel getResource() {
    return resource;
  }

  public void setResource(ResourceModel resource) {
    this.resource = resource;
  }

  public String getComponentUuid() {
    return componentUuid;
  }

  public void setComponentUuid(String componentUuid) {
    this.componentUuid = componentUuid;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("name", name)
      .append("categ", category)
      .append("date", date)
      .append("snapshot", snapshot)
      .append("resource", resource)
      .toString();
  }
}
