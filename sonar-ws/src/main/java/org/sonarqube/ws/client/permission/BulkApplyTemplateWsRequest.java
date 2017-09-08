/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.permission;

import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;

import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;

public class BulkApplyTemplateWsRequest {
  private String templateId;
  private String organization;
  private String templateName;
  private String query;
  private Collection<String> qualifiers = singleton(Qualifiers.PROJECT);
  private String visibility;
  private String analyzedBefore;
  private boolean onProvisionedOnly = false;
  private Collection<String> projects;

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

  public Collection<String> getQualifiers() {
    return qualifiers;
  }

  public BulkApplyTemplateWsRequest setQualifiers(Collection<String> qualifiers) {
    this.qualifiers = requireNonNull(qualifiers);
    return this;
  }

  @CheckForNull
  public String getVisibility() {
    return visibility;
  }

  public BulkApplyTemplateWsRequest setVisibility(@Nullable String visibility) {
    this.visibility = visibility;
    return this;
  }

  @CheckForNull
  public String getAnalyzedBefore() {
    return analyzedBefore;
  }

  public BulkApplyTemplateWsRequest setAnalyzedBefore(@Nullable String analyzedBefore) {
    this.analyzedBefore = analyzedBefore;
    return this;
  }

  public boolean isOnProvisionedOnly() {
    return onProvisionedOnly;
  }

  public BulkApplyTemplateWsRequest setOnProvisionedOnly(boolean onProvisionedOnly) {
    this.onProvisionedOnly = onProvisionedOnly;
    return this;
  }

  @CheckForNull
  public Collection<String> getProjects() {
    return projects;
  }

  public BulkApplyTemplateWsRequest setProjects(@Nullable Collection<String> projects) {
    this.projects = projects;
    return this;
  }
}
