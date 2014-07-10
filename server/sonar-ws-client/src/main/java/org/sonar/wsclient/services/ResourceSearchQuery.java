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
package org.sonar.wsclient.services;

/**
 * @since 3.4
 */
public class ResourceSearchQuery extends Query<ResourceSearchResult> {

  private int page = -1;
  private int pageSize = -1;
  private String[] qualifiers = null;
  private String text;

  private ResourceSearchQuery() {
  }

  public static ResourceSearchQuery create(String text) {
    return new ResourceSearchQuery().setText(text);
  }

  public int getPage() {
    return page;
  }

  public ResourceSearchQuery setPage(int page) {
    this.page = page;
    return this;
  }

  public int getPageSize() {
    return pageSize;
  }

  public ResourceSearchQuery setPageSize(int pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  public String[] getQualifiers() {
    return qualifiers;
  }

  public ResourceSearchQuery setQualifiers(String... qualifiers) {
    this.qualifiers = qualifiers;
    return this;
  }

  public String getText() {
    return text;
  }

  public ResourceSearchQuery setText(String text) {
    this.text = text;
    return this;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder();
    url.append("/api/resources/search?");
    appendUrlParameter(url, "s", text);
    if (page > 0) {
      appendUrlParameter(url, "p", page);
    }
    if (pageSize > 0) {
      appendUrlParameter(url, "ps", pageSize);
    }
    appendUrlParameter(url, "q", qualifiers);
    return url.toString();
  }

  @Override
  public Class<ResourceSearchResult> getModelClass() {
    return ResourceSearchResult.class;
  }
}
