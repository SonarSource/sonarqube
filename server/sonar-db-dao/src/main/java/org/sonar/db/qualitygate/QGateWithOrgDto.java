/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
 * This Dto is a join between QualityGates and Organizations.
 *
 * Tables : QUALITY_GATES joined with ORG_QUALITY_GATES
 */
public class QGateWithOrgDto extends QualityGateDto {
  private String organizationUuid;

  public String getOrganizationUuid() {
    return organizationUuid;
  }

  public void setOrganizationUuid(String organizationUuid) {
    this.organizationUuid = organizationUuid;
  }

  @Override
  public QGateWithOrgDto setUuid(String uuid) {
    super.setUuid(uuid);
    return this;
  }

  @Override
  public QGateWithOrgDto setId(Long id) {
    super.setId(id);
    return this;
  }

  @Override
  public QGateWithOrgDto setName(String name) {
    super.setName(name);
    return this;
  }

  @Override
  public QGateWithOrgDto setBuiltIn(boolean builtIn) {
    super.setBuiltIn(builtIn);
    return this;
  }

  @Override
  public QGateWithOrgDto setCreatedAt(Date createdAt) {
    super.setCreatedAt(createdAt);
    return this;
  }

  @Override
  public QGateWithOrgDto setUpdatedAt(Date updatedAt) {
    super.setUpdatedAt(updatedAt);
    return this;
  }
}
