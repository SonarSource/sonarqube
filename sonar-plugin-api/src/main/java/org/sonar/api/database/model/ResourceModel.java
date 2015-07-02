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

import java.io.Serializable;
import java.util.Date;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.database.BaseIdentifiable;

public class ResourceModel extends BaseIdentifiable implements Cloneable, Serializable {

  public static final String SCOPE_PROJECT = "PRJ";
  public static final String QUALIFIER_PROJECT_TRUNK = "TRK";

  public static final int DESCRIPTION_COLUMN_SIZE = 2000;
  public static final int NAME_COLUMN_SIZE = 256;
  public static final int KEY_SIZE = 400;
  public static final int PATH_SIZE = 2000;

  private String name;
  private String longName;
  private String description;
  private Boolean enabled = Boolean.TRUE;
  private String scope;
  private String qualifier;
  private String key;
  private String deprecatedKey;
  private String languageKey;
  private Integer rootId;
  private String path;
  private Integer copyResourceId;
  private Integer personId;
  private Date createdAt;
  private String uuid;
  private String projectUuid;
  private String moduleUuid;
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

  private static void checkSize(String key) {
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

}
