/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.qualitygate;

import java.util.Date;

/**
 * @since 4.3
 */
public class QualityGateDto {

  private Long id;
  private String name;
  private String uuid;
  private boolean isBuiltIn;
  private Date createdAt;
  private Date updatedAt;

  public String getUuid() {
    return uuid;
  }

  public QualityGateDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public Long getId() {
    return id;
  }

  public QualityGateDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public QualityGateDto setName(String name) {
    this.name = name;
    return this;
  }

  public boolean isBuiltIn() {
    return isBuiltIn;
  }

  public QualityGateDto setBuiltIn(boolean builtIn) {
    isBuiltIn = builtIn;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public QualityGateDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public QualityGateDto setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}
