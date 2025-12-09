/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.v2;

public class WebApiEndpoints {
  public static final String JSON_MERGE_PATCH_CONTENT_TYPE = "application/merge-patch+json";
  public static final String INTERNAL = "x-sonar-internal";

  public static final String SYSTEM_DOMAIN = "/system";
  public static final String LIVENESS_ENDPOINT = SYSTEM_DOMAIN + "/liveness";
  public static final String HEALTH_ENDPOINT = SYSTEM_DOMAIN + "/health";
  public static final String DATABASE_MIGRATIONS_ENDPOINT = SYSTEM_DOMAIN + "/migrations-status";
  public static final String EMAIL_CONFIGURATION_ENDPOINT = SYSTEM_DOMAIN + "/email-configurations";

  public static final String USERS_MANAGEMENT_DOMAIN = "/users-management";
  public static final String USER_ENDPOINT = USERS_MANAGEMENT_DOMAIN + "/users";

  public static final String AUTHORIZATIONS_DOMAIN = "/authorizations";
  public static final String GROUPS_ENDPOINT = AUTHORIZATIONS_DOMAIN + "/groups";
  public static final String GROUP_MEMBERSHIPS_ENDPOINT = AUTHORIZATIONS_DOMAIN + "/group-memberships";

  public static final String CLEAN_CODE_POLICY_DOMAIN = "/clean-code-policy";
  public static final String RULES_ENDPOINT = CLEAN_CODE_POLICY_DOMAIN + "/rules";
  public static final String MODE_ENDPOINT = CLEAN_CODE_POLICY_DOMAIN + "/mode";

  public static final String DOP_TRANSLATION_DOMAIN = "/dop-translation";
  public static final String GITLAB_CONFIGURATION_ENDPOINT = DOP_TRANSLATION_DOMAIN + "/gitlab-configurations";
  public static final String GITHUB_CONFIGURATION_ENDPOINT = DOP_TRANSLATION_DOMAIN + "/github-configurations";
  public static final String BOUND_PROJECTS_ENDPOINT = DOP_TRANSLATION_DOMAIN + "/bound-projects";
  public static final String PROJECT_BINDINGS_ENDPOINT = DOP_TRANSLATION_DOMAIN + "/project-bindings";
  public static final String DOP_SETTINGS_ENDPOINT = DOP_TRANSLATION_DOMAIN + "/dop-settings";
  public static final String GITLAB_PERMISSIONS_MAPPING_ENDPOINT = DOP_TRANSLATION_DOMAIN + "/gitlab-permission-mappings";

  public static final String ANALYSIS_DOMAIN = "/analysis";
  public static final String VERSION_ENDPOINT = ANALYSIS_DOMAIN + "/version";
  public static final String JRE_ENDPOINT = ANALYSIS_DOMAIN + "/jres";
  public static final String ACTIVE_RULES_ENDPOINT = ANALYSIS_DOMAIN + "/active_rules";
  public static final String SCANNER_ENGINE_ENDPOINT = ANALYSIS_DOMAIN + "/engine";

  public static final String SCA_DOMAIN = "/sca";
  public static final String SCA_RELEASES_ENDPOINT = SCA_DOMAIN + "/releases";
  public static final String SCA_ISSUES_RELEASES_ENDPOINT = SCA_DOMAIN + "/issues-releases";
  public static final String SCA_SBOM_REPORTS_ENDPOINT = SCA_DOMAIN + "/sbom-reports";
  public static final String SCA_CLIS_ENDPOINT = SCA_DOMAIN + "/clis";
  public static final String SCA_FEATURE_ENABLEMENT_ENDPOINT = SCA_DOMAIN + "/feature-enablements";

  public static final String MARKETPLACE_DOMAIN = "/marketplace";
  public static final String AZURE_DOMAIN = "/azure";
  public static final String AZURE_BILLING_ENDPOINT = MARKETPLACE_DOMAIN + AZURE_DOMAIN + "/billing";

  private WebApiEndpoints() {
  }
}
