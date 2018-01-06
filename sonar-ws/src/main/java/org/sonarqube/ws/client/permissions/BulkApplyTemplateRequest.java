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
package org.sonarqube.ws.client.permissions;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/bulk_apply_template">Further information about this action online (including a response example)</a>
 * @since 5.5
 */
@Generated("sonar-ws-generator")
public class BulkApplyTemplateRequest {

  private String analyzedBefore;
  private String onProvisionedOnly;
  private String organization;
  private List<String> projects;
  private String q;
  private List<String> qualifiers;
  private String templateId;
  private String templateName;
  private String visibility;

  /**
   * Example value: "2017-10-19 or 2017-10-19T13:00:00+0200"
   */
  public BulkApplyTemplateRequest setAnalyzedBefore(String analyzedBefore) {
    this.analyzedBefore = analyzedBefore;
    return this;
  }

  public String getAnalyzedBefore() {
    return analyzedBefore;
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
  public BulkApplyTemplateRequest setOnProvisionedOnly(String onProvisionedOnly) {
    this.onProvisionedOnly = onProvisionedOnly;
    return this;
  }

  public String getOnProvisionedOnly() {
    return onProvisionedOnly;
  }

  /**
   * This is part of the internal API.
   * Example value: "my-org"
   */
  public BulkApplyTemplateRequest setOrganization(String organization) {
    this.organization = organization;
    return this;
  }

  public String getOrganization() {
    return organization;
  }

  /**
   * Example value: "my_project,another_project"
   */
  public BulkApplyTemplateRequest setProjects(List<String> projects) {
    this.projects = projects;
    return this;
  }

  public List<String> getProjects() {
    return projects;
  }

  /**
   * Example value: "apac"
   */
  public BulkApplyTemplateRequest setQ(String q) {
    this.q = q;
    return this;
  }

  public String getQ() {
    return q;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"TRK"</li>
   * </ul>
   */
  public BulkApplyTemplateRequest setQualifiers(List<String> qualifiers) {
    this.qualifiers = qualifiers;
    return this;
  }

  public List<String> getQualifiers() {
    return qualifiers;
  }

  /**
   * Example value: "AU-Tpxb--iU5OvuD2FLy"
   */
  public BulkApplyTemplateRequest setTemplateId(String templateId) {
    this.templateId = templateId;
    return this;
  }

  public String getTemplateId() {
    return templateId;
  }

  /**
   * Example value: "Default Permission Template for Projects"
   */
  public BulkApplyTemplateRequest setTemplateName(String templateName) {
    this.templateName = templateName;
    return this;
  }

  public String getTemplateName() {
    return templateName;
  }

  /**
   * This is part of the internal API.
   * Possible values:
   * <ul>
   *   <li>"private"</li>
   *   <li>"public"</li>
   * </ul>
   */
  public BulkApplyTemplateRequest setVisibility(String visibility) {
    this.visibility = visibility;
    return this;
  }

  public String getVisibility() {
    return visibility;
  }
}
