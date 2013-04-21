/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.database.configuration;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.database.BaseIdentifiable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * IMPORTANT : This class can't be moved to org.sonar.jpa.dao for backward-compatibility reasons.
 * This class is still used in some plugins.
 *
 * @since 1.10
 */
@Entity
@Table(name = "properties")
public class Property extends BaseIdentifiable {

  @Column(name = "prop_key", updatable = true, nullable = true)
  private String key;

  @Column(name = "text_value", updatable = true, nullable = true, length = 167772150)
  @Lob
  private char[] value;

  @Column(name = "resource_id", updatable = true, nullable = true)
  private Integer resourceId;

  @Column(name = "user_id", updatable = true, nullable = true)
  private Integer userId;

  public Property(String key, String value) {
    this(key, value, null);
  }

  public Property(String key, String value, Integer resourceId) {
    this.key = key;
    if (value != null) {
      this.value = value.toCharArray();

    } else {
      this.value = null;
    }
    this.resourceId = resourceId;
  }

  public Property() {
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    if (value != null) {
      return new String(value);
    }
    return null;
  }

  public void setValue(String value) {
    if (value != null) {
      this.value = value.toCharArray();
    } else {
      this.value = null;
    }
  }

  public Integer getResourceId() {
    return resourceId;
  }

  public void setResourceId(Integer resourceId) {
    this.resourceId = resourceId;
  }

  public Integer getUserId() {
    return userId;
  }

  public Property setUserId(Integer userId) {
    this.userId = userId;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Property property = (Property) o;
    if (!key.equals(property.key)) {
      return false;
    }
    if (resourceId != null ? !resourceId.equals(property.resourceId) : property.resourceId != null) {
      return false;
    }
    return !(userId != null ? !userId.equals(property.userId) : property.userId != null);

  }

  @Override
  public int hashCode() {
    int result = key.hashCode();
    result = 31 * result + (resourceId != null ? resourceId.hashCode() : 0);
    result = 31 * result + (userId != null ? userId.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }
}
