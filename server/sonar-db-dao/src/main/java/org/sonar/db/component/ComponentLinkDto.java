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
package org.sonar.db.component;

import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * Component links should be merge in a 'links' column (using protobuf for instance) of the projects table.
 * But to do this we'll have to wait for the measure filters page (where links are displayed) to be rewritten in JS/WS (because it's in Rails for the moment).
 */
public class ComponentLinkDto {

  public static final String TYPE_HOME_PAGE = "homepage";
  public static final String TYPE_CI = "ci";
  public static final String TYPE_ISSUE_TRACKER = "issue";
  public static final String TYPE_SOURCES = "scm";
  public static final String TYPE_SOURCES_DEV = "scm_dev";

  public static final List<String> PROVIDED_TYPES = ImmutableList.of(TYPE_HOME_PAGE, TYPE_CI, TYPE_ISSUE_TRACKER, TYPE_SOURCES, TYPE_SOURCES_DEV);

  private Long id;
  private String componentUuid;
  private String type;
  private String name;
  private String href;

  public String getName() {
    return name;
  }

  public ComponentLinkDto setName(String name) {
    this.name = name;
    return this;
  }

  public String getComponentUuid() {
    return componentUuid;
  }

  public ComponentLinkDto setComponentUuid(String componentUuid) {
    this.componentUuid = componentUuid;
    return this;
  }

  public String getHref() {
    return href;
  }

  public ComponentLinkDto setHref(String href) {
    this.href = href;
    return this;
  }

  public Long getId() {
    return id;
  }

  public String getIdAsString() {
    return String.valueOf(id);
  }

  public ComponentLinkDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getType() {
    return type;
  }

  public ComponentLinkDto setType(String type) {
    this.type = type;
    return this;
  }
}
