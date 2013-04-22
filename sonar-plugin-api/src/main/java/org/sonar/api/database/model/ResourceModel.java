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
package org.sonar.api.database.model;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.annotations.BatchSize;
import org.sonar.api.database.BaseIdentifiable;
import org.sonar.api.resources.ProjectLink;
import org.sonar.api.resources.Resource;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Class to map resource with hibernate model
 */
@Entity
@Table(name = "projects")
public class ResourceModel extends BaseIdentifiable implements Cloneable {

  public static final String SCOPE_PROJECT = "PRJ";
  public static final String QUALIFIER_PROJECT_TRUNK = "TRK";

  public static final int DESCRIPTION_COLUMN_SIZE = 2000;
  public static final int NAME_COLUMN_SIZE = 256;
  public static final int KEY_SIZE = 400;

  @Column(name = "name", updatable = true, nullable = true, length = NAME_COLUMN_SIZE)
  private String name;

  @Column(name = "long_name", updatable = true, nullable = true, length = NAME_COLUMN_SIZE)
  private String longName;

  @Column(name = "description", updatable = true, nullable = true, length = DESCRIPTION_COLUMN_SIZE)
  private String description;

  @Column(name = "enabled", updatable = true, nullable = false)
  private Boolean enabled = Boolean.TRUE;

  @Column(name = "scope", updatable = true, nullable = false, length = 3)
  private String scope;

  @Column(name = "qualifier", updatable = true, nullable = false, length = 10)
  private String qualifier;

  @Column(name = "kee", updatable = false, nullable = false, length = KEY_SIZE)
  private String key;

  @Column(name = "language", updatable = true, nullable = true, length = 20)
  private String languageKey;

  @Column(name = "root_id", updatable = true, nullable = true)
  private Integer rootId;

  @Column(name = "copy_resource_id", updatable = true, nullable = true)
  private Integer copyResourceId;

  @Column(name = "person_id", updatable = true, nullable = true)
  private Integer personId;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "created_at", updatable = true, nullable = true)
  private Date createdAt;

  @OneToMany(mappedBy = "resource", fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REMOVE})
  @BatchSize(size = 8)
  private List<ProjectLink> projectLinks = new ArrayList<ProjectLink>();

  /**
   * Default constructor
   */
  public ResourceModel() {
    this.createdAt = new Date();
  }

  /**
   * <p>Creates a resource model</p>
   *
   * @param scope     the scope the rule will apply on
   * @param key       the rule key. This is the name of the resource, including the path
   * @param qualifier the resource qualifier
   * @param rootId    the rootId for the resource
   * @param name      the short name of the resource
   */
  public ResourceModel(String scope, String key, String qualifier, Integer rootId, String name) {
    // call this to have the "createdAt" field initialized
    this();
    this.scope = scope;
    this.key = key;
    this.rootId = rootId;
    this.name = name;
    this.qualifier = qualifier;
  }

  /**
   * Only available at project level.
   */
  public List<ProjectLink> getProjectLinks() {
    return projectLinks;
  }

  public void setProjectLinks(List<ProjectLink> projectLinks) {
    this.projectLinks = projectLinks;
  }

  /**
   * @return a project link given its key if exists, null otherwise
   */
  public ProjectLink getProjectLink(String key) {
    for (ProjectLink projectLink : projectLinks) {
      if (key.equals(projectLink.getKey())) {
        return projectLink;
      }
    }
    return null;
  }

  /**
   * Only available at project level.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the resource description, truncated to DESCRIPTION_COLUMN_SIZE
   */
  public void setDescription(String description) {
    this.description = StringUtils.abbreviate(description, DESCRIPTION_COLUMN_SIZE);
  }

  public String getName() {
    return name;
  }

  /**
   * Sets the resource name, truncated to NAME_COLUMN_SIZE
   */
  public void setName(String name) {
    this.name = StringUtils.abbreviate(name, NAME_COLUMN_SIZE);
    if (this.longName == null) {
      this.longName = this.name;
    }
  }

  public String getLongName() {
    return longName;
  }

  /**
   * Sets the long name of the resource, truncated to NAME_COLUMN_SIZE
   */
  public void setLongName(String s) {
    if (StringUtils.isBlank(s)) {
      this.longName = name;
    } else {
      this.longName = StringUtils.abbreviate(s, NAME_COLUMN_SIZE);
    }
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public String getKey() {
    return key;
  }

  public String getLanguageKey() {
    return languageKey;
  }

  public void setLanguageKey(String lang) {
    this.languageKey = lang;
  }

  public Integer getCopyResourceId() {
    return copyResourceId;
  }

  public void setCopyResourceId(Integer i) {
    this.copyResourceId = i;
  }

  /**
   * @since 2.14
   */
  public Integer getPersonId() {
    return personId;
  }

  /**
   * @since 2.14
   */
  public ResourceModel setPersonId(Integer i) {
    this.personId = i;
    return this;
  }

  /**
   * @throws IllegalArgumentException if the key is longer than KEY_SIZE
   */
  public void setKey(String key) {
    if (key.length() > KEY_SIZE) {
      throw new IllegalArgumentException("Resource key is too long, max is " + KEY_SIZE + " characters. Got : " + key);
    }
    this.key = key;
  }

  public Integer getRootId() {
    return rootId;
  }

  public void setRootId(Integer rootId) {
    this.rootId = rootId;
  }

  public String getQualifier() {
    return qualifier;
  }

  public void setQualifier(String qualifier) {
    this.qualifier = qualifier;
  }

  public Date getCreatedAt() {
    return createdAt; // NOSONAR May expose internal representation by returning reference to mutable object
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt; // NOSONAR May expose internal representation by returning reference to mutable object
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ResourceModel)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    ResourceModel other = (ResourceModel) obj;
    return new EqualsBuilder()
        .append(key, other.key)
        .append(enabled, other.enabled)
        .append(rootId, other.rootId)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(key)
        .append(enabled)
        .append(rootId)
        .toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("id", getId())
        .append("key", key)
        .append("scope", scope)
        .append("qualifier", qualifier)
        .append("name", name)
        .append("longName", longName)
        .append("lang", languageKey)
        .append("enabled", enabled)
        .append("rootId", rootId)
        .append("copyResourceId", copyResourceId)
        .append("personId", personId)
        .append("createdAt", createdAt)
        .toString();
  }

  @Override
  public Object clone() {
    ResourceModel clone = new ResourceModel(getScope(), getKey(), getQualifier(), getRootId(), getName());
    clone.setDescription(getDescription());
    clone.setEnabled(getEnabled());
    clone.setProjectLinks(getProjectLinks());
    clone.setLanguageKey(getLanguageKey());
    clone.setCopyResourceId(getCopyResourceId());
    clone.setLongName(getLongName());
    clone.setPersonId(getPersonId());
    clone.setCreatedAt(getCreatedAt());
    return clone;
  }

  /**
   * Maps a resource to a resource model and returns the resource
   */
  public static ResourceModel build(Resource resource) {
    ResourceModel model = new ResourceModel();
    model.setEnabled(Boolean.TRUE);
    model.setDescription(resource.getDescription());
    model.setKey(resource.getKey());
    if (resource.getLanguage() != null) {
      model.setLanguageKey(resource.getLanguage().getKey());
    }
    if (StringUtils.isNotBlank(resource.getName())) {
      model.setName(resource.getName());
    } else {
      model.setName(resource.getKey());
    }
    model.setLongName(resource.getLongName());
    model.setQualifier(resource.getQualifier());
    model.setScope(resource.getScope());
    return model;
  }

}
