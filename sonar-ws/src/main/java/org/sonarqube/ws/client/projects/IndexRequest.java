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
package org.sonarqube.ws.client.projects;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/projects/index">Further information about this action online (including a response example)</a>
 * @since 2.10
 */
@Generated("sonar-ws-generator")
public class IndexRequest {

  private String desc;
  private String format;
  private String libs;
  private String project;
  private String search;
  private String subprojects;
  private String versions;
  private String views;

  /**
   * @deprecated since 6.3
   */
  @Deprecated
  public IndexRequest setDesc(String desc) {
    this.desc = desc;
    return this;
  }

  public String getDesc() {
    return desc;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"json"</li>
   * </ul>
   */
  public IndexRequest setFormat(String format) {
    this.format = format;
    return this;
  }

  public String getFormat() {
    return format;
  }

  /**
   * @deprecated since 6.3
   */
  @Deprecated
  public IndexRequest setLibs(String libs) {
    this.libs = libs;
    return this;
  }

  public String getLibs() {
    return libs;
  }

  /**
   * Example value: "my_project"
   */
  public IndexRequest setProject(String project) {
    this.project = project;
    return this;
  }

  public String getProject() {
    return project;
  }

  /**
   * Example value: "Sonar"
   */
  public IndexRequest setSearch(String search) {
    this.search = search;
    return this;
  }

  public String getSearch() {
    return search;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"true"</li>
   *   <li>"false"</li>
   *   <li>"yes"</li>
   *   <li>"no"</li>
   * </ul>
   */
  public IndexRequest setSubprojects(String subprojects) {
    this.subprojects = subprojects;
    return this;
  }

  public String getSubprojects() {
    return subprojects;
  }

  /**
   * @deprecated since 6.3
   */
  @Deprecated
  public IndexRequest setVersions(String versions) {
    this.versions = versions;
    return this;
  }

  public String getVersions() {
    return versions;
  }

  /**
   * @deprecated since 6.3
   */
  @Deprecated
  public IndexRequest setViews(String views) {
    this.views = views;
    return this;
  }

  public String getViews() {
    return views;
  }
}
