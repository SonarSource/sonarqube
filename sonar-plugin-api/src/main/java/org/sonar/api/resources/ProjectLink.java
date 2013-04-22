/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.api.resources;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.database.BaseIdentifiable;
import org.sonar.api.database.model.ResourceModel;

import javax.persistence.*;

/**
 * @since 1.10
 */
@Entity(name = "ProjectLink")
@Table(name = "project_links")
public class ProjectLink extends BaseIdentifiable {

  public static final int NAME_COLUMN_SIZE = 128;
  public static final int HREF_COLUMN_SIZE = 2048;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", updatable = false, nullable = false)
  private ResourceModel resource;

  @Column(name = "link_type", updatable = true, nullable = true, length = 20)
  private String key;

  @Column(name = "name", updatable = true, nullable = true, length = NAME_COLUMN_SIZE)
  private String name;

  @Column(name = "href", updatable = true, nullable = false, length = HREF_COLUMN_SIZE)
  private String href;

  public ProjectLink() {
  }

  public ProjectLink(String key, String name, String href) {
    this.key = key;
    setName(name);
    setHref(href);
  }

  public ResourceModel getResource() {
    return resource;
  }

  public void setResource(ResourceModel resource) {
    this.resource = resource;
  }

  public String getName() {
    return name;
  }

  public final void setName(String name) {
    this.name = StringUtils.abbreviate(name, NAME_COLUMN_SIZE);
  }

  public String getHref() {
    return href;
  }

  public final void setHref(String href) {
    if (href == null) {
      throw new IllegalArgumentException("ProjectLink.href can not be null");
    }
    this.href = StringUtils.abbreviate(href, HREF_COLUMN_SIZE);
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ProjectLink that = (ProjectLink) o;
    if (!key.equals(that.key)) {
      return false;
    }
    return resource.equals(that.resource);

  }

  @Override
  public int hashCode() {
    int result = resource != null ? resource.hashCode() : 0;
    result = 31 * result + key.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }

  public void copyFieldsFrom(ProjectLink link) {
    this.name = link.getName();
    this.href = link.getHref();
  }
}