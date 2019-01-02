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
package org.sonar.db.organization;

public class OrganizationWithNclocDto {
  private String id;
  private String kee;
  private String name;
  private long ncloc;

  public String getId() {
    return id;
  }

  public OrganizationWithNclocDto setId(String id) {
    this.id = id;
    return this;
  }

  public String getKee() {
    return kee;
  }

  public OrganizationWithNclocDto setKee(String kee) {
    this.kee = kee;
    return this;
  }

  public String getName() {
    return name;
  }

  public OrganizationWithNclocDto setName(String name) {
    this.name = name;
    return this;
  }

  public long getNcloc() {
    return ncloc;
  }

  public OrganizationWithNclocDto setNcloc(long ncloc) {
    this.ncloc = ncloc;
    return this;
  }
}
