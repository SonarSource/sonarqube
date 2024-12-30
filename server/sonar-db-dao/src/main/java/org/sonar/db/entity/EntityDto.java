/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.entity;

import java.util.Objects;
import javax.annotation.CheckForNull;
import org.sonar.db.component.ComponentQualifiers;

/**
 * Represents a project, an application, a portfolio or a sub-portfolio.
 * Entities are stored either in the projects or portfolios tables.
 */
public class EntityDto {

  protected String organizationUuid;
  protected String kee;
  protected String uuid;
  protected String name;
  protected String qualifier;
  protected String description;
  protected boolean isPrivate;

  // This field should be null for anything that is not subportfolio
  protected String authUuid;

  public String getAuthUuid() {
    if (ComponentQualifiers.SUBVIEW.equals(qualifier)) {
      return authUuid;
    }
    return uuid;
  }

  public String getOrganizationUuid() {
    return organizationUuid;
  }

  public String getKey() {
    return kee;
  }

  public String getKee() {
    return kee;
  }

  public String getUuid() {
    return uuid;
  }

  /**
   * Can be TRK, APP, VW or SVW
   */
  public String getQualifier() {
    return qualifier;
  }

  public String getName() {
    return name;
  }

  @CheckForNull
  public String getDescription() {
    return description;
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public boolean isPortfolio() {
    return ComponentQualifiers.VIEW.equals(qualifier) || ComponentQualifiers.SUBVIEW.equals(qualifier);
  }

  public boolean isProject() {
    return ComponentQualifiers.PROJECT.equals(qualifier);
  }

  public boolean isProjectOrApp() {
    return ComponentQualifiers.APP.equals(qualifier) || isProject();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EntityDto entityDto)) {
      return false;
    }
    return Objects.equals(uuid, entityDto.uuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid);
  }

  public <T extends EntityDto> T setPrivate(boolean aPrivate) {
    isPrivate = aPrivate;
    return (T) this;
  }
}
