/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.persistence.template;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class LoadedTemplateDto {

  public static final String DASHBOARD_TYPE = "DASHBOARD";

  private Long id;
  private String key;
  private String type;

  /**
   * Default constructor
   */
  public LoadedTemplateDto() {
  }

  /**
   * @param key
   * @param type
   */
  public LoadedTemplateDto(String key, String type) {
    super();
    this.key = key;
    this.type = type;
  }

  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * @param id
   *          the id to set
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * @return the key
   */
  public String getKey() {
    return key;
  }

  /**
   * @param key
   *          the key to set
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @param type
   *          the type to set
   */
  public void setType(String type) {
    this.type = type;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this, new String[] { "id" });
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    return EqualsBuilder.reflectionEquals(this, obj, new String[] { "id" });
  }

}
