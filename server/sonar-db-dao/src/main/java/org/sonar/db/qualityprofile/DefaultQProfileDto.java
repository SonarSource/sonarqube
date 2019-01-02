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
package org.sonar.db.qualityprofile;

public class DefaultQProfileDto {

  private String organizationUuid;
  private String language;
  private String qProfileUuid;

  public String getOrganizationUuid() {
    return organizationUuid;
  }

  public DefaultQProfileDto setOrganizationUuid(String s) {
    this.organizationUuid = s;
    return this;
  }

  public String getLanguage() {
    return language;
  }

  public DefaultQProfileDto setLanguage(String s) {
    this.language = s;
    return this;
  }

  public String getQProfileUuid() {
    return qProfileUuid;
  }

  public DefaultQProfileDto setQProfileUuid(String s) {
    this.qProfileUuid = s;
    return this;
  }

  public static DefaultQProfileDto from(QProfileDto profile) {
    return new DefaultQProfileDto()
      .setOrganizationUuid(profile.getOrganizationUuid())
      .setLanguage(profile.getLanguage())
      .setQProfileUuid(profile.getKee());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("DefaultQProfileDto{");
    sb.append("organizationUuid='").append(organizationUuid).append('\'');
    sb.append(", language='").append(language).append('\'');
    sb.append(", qProfileUuid='").append(qProfileUuid).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
