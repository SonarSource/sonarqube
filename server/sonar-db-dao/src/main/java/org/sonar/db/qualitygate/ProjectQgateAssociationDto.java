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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * @since 4.3
 */
public class ProjectQgateAssociationDto {

  private Long id;
  private String name;
  private String gateId;

  public Long getId() {
    return id;
  }

  public ProjectQgateAssociationDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public ProjectQgateAssociationDto setName(String name) {
    this.name = name;
    return this;
  }

  @CheckForNull
  public String getGateId() {
    return gateId;
  }

  public ProjectQgateAssociationDto setGateId(@Nullable String gateId) {
    this.gateId = gateId;
    return this;
  }

  public ProjectQgateAssociation toQgateAssociation() {
    return new ProjectQgateAssociation()
      .setId(id)
      .setName(name)
      .setMember(gateId != null);
  }
}
