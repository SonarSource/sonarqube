/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonarqube.ws.client.permission;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class BulkApplyTemplateWsRequest {
  private String templateId;
  private String organization;
  private String templateName;
  private String query;
  private String qualifier;

  @CheckForNull
  public String getTemplateId() {
    return templateId;
  }

  public BulkApplyTemplateWsRequest setTemplateId(@Nullable String templateId) {
    this.templateId = templateId;
    return this;
  }

  @CheckForNull
  public String getOrganization() {
    return organization;
  }

  public BulkApplyTemplateWsRequest setOrganization(@Nullable String s) {
    this.organization = s;
    return this;
  }

  @CheckForNull
  public String getTemplateName() {
    return templateName;
  }

  public BulkApplyTemplateWsRequest setTemplateName(@Nullable String templateName) {
    this.templateName = templateName;
    return this;
  }

  @CheckForNull
  public String getQuery() {
    return query;
  }

  public BulkApplyTemplateWsRequest setQuery(@Nullable String query) {
    this.query = query;
    return this;
  }

  @CheckForNull
  public String getQualifier() {
    return qualifier;
  }

  public BulkApplyTemplateWsRequest setQualifier(@Nullable String qualifier) {
    this.qualifier = qualifier;
    return this;
  }
}
