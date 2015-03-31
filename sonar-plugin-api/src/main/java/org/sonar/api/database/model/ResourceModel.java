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
package org.sonar.api.database.model;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.database.BaseIdentifiable;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Resource;

import javax.annotation.Nullable;
import javax.persistence.*;

import java.io.Serializable;
import java.util.Date;

/**
 * Class to map resource with hibernate model
 */
@Entity
@Table(name = "projects")
public class ResourceModel extends BaseIdentifiable implements Cloneable, Serializable {

  public static final String SCOPE_PROJECT = "PRJ";
  public static final String QUALIFIER_PROJECT_TRUNK = "TRK";

  public static final int DESCRIPTION_COLUMN_SIZE = 2000;
  public static final int NAME_COLUMN_SIZE = 256;
  public static final int KEY_SIZE = 400;
  public static final int PATH_SIZE = 2000;

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

  @Column(name = "kee", updatable = true, nullable = false, length = KEY_SIZE)
  private String key;

  @Column(name = "deprecated_kee", updatable = true, nullable = true, length = KEY_SIZE)
  private String deprecatedKey;

  @Column(name = "language", updatable = true, nullable = true, length = 20)
  private String languageKey;

  @Column(name = "root_id", updatable = true, nullable = true)
  private Integer rootId;

  @Column(name = "path", updatable = true, nullable = true, length = PATH_SIZE)
  private String path;

  @Column(name = "copy_resource_id", updatable = true, nullable = true)
  private Integer copyResourceId;

  @Column(name = "person_id", updatable = true, nullable = true)
  private Integer personId;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "created_at", updatable = true, nullable = true)
  private Date createdAt;

  @Column(name = "uuid", updatable = false, nullable = true, length = 50)
  private String uuid;

  @Column(name = "project_uuid", updatable = true, nullable = true, length = 50)
  private String projectUuid;

  @Column(name = "module_uuid", updatable = true, nullable = true, length = 50)
  private String moduleUuid;

  @Column(name = "module_uuid_path", updatable = true, nullable = true, length = 4000)
  private String moduleUuidPath;

  /**
   * Default constructor
   */
  public ResourceModel() {
    this.createdAt = new Date();
  }

  public ResourceModel(String scope, String key, String qualifier, Integer rootId, String name) {
    this(scope, key, qualifier, rootId, null, name);
  }

  /**
   * <p>Creates a resource model</p>
   *
   * @param scope     the scope the rule will apply on
   * @param key       the rule key. This is the name of the resource, including the path
   * @param qualifier the resource qualifier
   * @param rootId    the rootId for the resource
   * @param path      the path of the resource
   * @param name      the short name of the resource
   */
  public ResourceModel(String scope, String key, String qualifier, Integer rootId, @Nullable String path, String name) {
    // call this to have the "createdAt" field initialized
    this();
    this.scope = scope;
    this.key = key;
    this.rootId = rootId;
    this.path = path;
    this.name = name;
    this.qualifier = qualifier;
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

  public String getDeprecatedKey() {
    return deprecatedKey;
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
    checkSize(key);
    this.key = key;
  }

  private void checkSize(String key) {
    if (key.length() > KEY_SIZE) {
      throw new IllegalArgumentException("Resource key is too long, max is " + KEY_SIZE + " characters. Got : " + key);
    }
  }

  /**
   * @throws IllegalArgumentException if the key is longer than KEY_SIZE
   */
  public void setDeprecatedKey(String deprecatedKey) {
    checkSize(deprecatedKey);
    this.deprecatedKey = deprecatedKey;
  }

  public Integer getRootId() {
    return rootId;
  }

  public void setRootId(Integer rootId) {
    this.rootId = rootId;
  }

  public String getPath() {
    return path;
  }

  public ResourceModel setPath(@Nullable String path) {
    if (path != null && path.length() > PATH_SIZE) {
      throw new IllegalArgumentException("Resource path is too long, max is " + PATH_SIZE + " characters. Got : " + path);
    }
    this.path = path;
    return this;
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

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public void setProjectUuid(String projectUuid) {
    this.projectUuid = projectUuid;
  }

  public String getModuleUuid() {
    return moduleUuid;
  }

  public void setModuleUuid(String moduleUuid) {
    this.moduleUuid = moduleUuid;
  }

  public String getModuleUuidPath() {
    return moduleUuidPath;
  }

  public void setModuleUuidPath(String moduleUuidPath) {
    this.moduleUuidPath = moduleUuidPath;
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
      .append("deprecatedKey", deprecatedKey)
      .append("scope", scope)
      .append("qualifier", qualifier)
      .append("name", name)
      .append("longName", longName)
      .append("lang", languageKey)
      .append("enabled", enabled)
      .append("rootId", rootId)
      .append("path", path)
      .append("copyResourceId", copyResourceId)
      .append("personId", personId)
      .append("createdAt", createdAt)
      .toString();
  }

  @Override
  public Object clone() {
    ResourceModel clone = new ResourceModel(getScope(), getKey(), getQualifier(), getRootId(), getPath(), getName());
    clone.setDescription(getDescription());
    clone.setDeprecatedKey(getDeprecatedKey());
    clone.setEnabled(getEnabled());
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
    model.setKey(resource.getEffectiveKey());
    model.setPath(resource.getPath());
    Language lang = resource.getLanguage();
    if (lang != null) {
      model.setLanguageKey(lang.getKey());
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
