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
package org.sonar.ce.task.projectanalysis.analysis;

import javax.annotation.concurrent.Immutable;
import org.sonar.db.organization.OrganizationDto;

import static java.util.Objects.requireNonNull;

@Immutable
public class Organization {
  private final String uuid;
  private final String key;
  private final String name;
  private final String defaultQualityGateUuid;

  private Organization(String uuid, String key, String name, String defaultQualityGateUuid) {
    this.uuid = requireNonNull(uuid, "uuid can't be null");
    this.key = requireNonNull(key, "key can't be null");
    this.name = requireNonNull(name, "name can't be null");
    this.defaultQualityGateUuid = requireNonNull(defaultQualityGateUuid, "defaultQualityGateUuid can't be null");
  }

  public String getUuid() {
    return uuid;
  }

  public String getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  public String getDefaultQualityGateUuid() {
    return defaultQualityGateUuid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Organization that = (Organization) o;
    return uuid.equals(that.uuid);
  }

  @Override
  public int hashCode() {
    return uuid.hashCode();
  }

  @Override
  public String toString() {
    return "Organization{" +
      "uuid='" + uuid + '\'' +
      ", key='" + key + '\'' +
      ", name='" + name + '\'' +
      ", defaultQualityGateUuid='" + defaultQualityGateUuid + '\'' +
      '}';
  }

  public OrganizationDto toDto() {
    return new OrganizationDto()
      .setName(name)
      .setKey(key)
      .setUuid(uuid)
      .setDefaultQualityGateUuid(defaultQualityGateUuid);
  }

  public static Organization from(OrganizationDto organizationDto) {
    return new Organization(organizationDto.getUuid(), organizationDto.getKey(), organizationDto.getName(), organizationDto.getDefaultQualityGateUuid());
  }

}
