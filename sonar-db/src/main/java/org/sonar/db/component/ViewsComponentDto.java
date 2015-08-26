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
package org.sonar.db.component;

public class ViewsComponentDto {
  private Long id;
  private String name;
  private String uuid;
  private String kee;
  private String scope;
  private String qualifier;
  private Long copyResourceId;
  private String moduleUuid;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getKee() {
    return kee;
  }

  public void setKee(String kee) {
    this.kee = kee;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public String getQualifier() {
    return qualifier;
  }

  public void setQualifier(String qualifier) {
    this.qualifier = qualifier;
  }

  public Long getCopyResourceId() {
    return copyResourceId;
  }

  public void setCopyResourceId(Long copyResourceId) {
    this.copyResourceId = copyResourceId;
  }

  public String getModuleUuid() {
    return moduleUuid;
  }

  public void setModuleUuid(String moduleUuid) {
    this.moduleUuid = moduleUuid;
  }

  @Override
  public String toString() {
    return "ViewsComponentDto{" +
        "id=" + id +
        ", name='" + name + '\'' +
        ", uuid='" + uuid + '\'' +
        ", kee='" + kee + '\'' +
        ", scope='" + scope + '\'' +
        ", qualifier='" + qualifier + '\'' +
        ", copyResourceId='" + copyResourceId + '\'' +
        ", moduleUuid='" + moduleUuid + '\'' +
        '}';
  }
}
