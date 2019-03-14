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
package org.sonar.api;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.platform.Server;

/**
 * Non-exhaustive list of constants of core properties.
 *
 * @since 1.11
 */
public interface CoreProperties {

  /**
   * @since 3.0
   */
  String ENCRYPTION_SECRET_KEY_PATH = "sonar.secretKeyPath";

  /**
   * @since 2.11
   */
  String CATEGORY_GENERAL = "general";

  /**
   * @since 4.0
   */
  String SUBCATEGORY_DATABASE_CLEANER = "databaseCleaner";

  /**
   * @since 7.6
   */
  String SUBCATEGORY_MODULES = "subProjects";

  /**
   * @since 4.0
   */
  String SUBCATEGORY_DUPLICATIONS = "duplications";

  /**
   * @since 6.6
   */
  String SUBCATEGORY_BRANCHES = "Branches";

  /**
   * @since 4.0
   */
  String SUBCATEGORY_DIFFERENTIAL_VIEWS = "differentialViews";

  /**
   * @since 5.1
   */
  String SUBCATEGORY_LOOKNFEEL = "looknfeel";

  /**
   * @since 5.1
   */
  String SUBCATEGORY_ISSUES = "issues";

  /**
   * @since 4.0
   */
  String SUBCATEGORY_L10N = "localization";

  /**
   * @since 7.2
   */
  String CATEGORY_EXTERNAL_ISSUES = "externalIssues";

  /**
   * @since 2.11
   */
  String CATEGORY_CODE_COVERAGE = "codeCoverage";

  /**
   * @since 2.11
   */
  String CATEGORY_SECURITY = "security";

  /**
   * @since 2.11
   * @deprecated since 6.0
   */
  @Deprecated
  String CATEGORY_JAVA = "java";

  /**
   * @since 3.3
   */
  String CATEGORY_EXCLUSIONS = "exclusions";

  /**
   * @since 4.0
   */
  String SUBCATEGORY_FILES_EXCLUSIONS = "files";

  /**
   * @since 4.0
   */
  String SUBCATEGORY_DUPLICATIONS_EXCLUSIONS = "duplications";

  /**
   * @since 4.0
   */
  String SUBCATEGORY_COVERAGE_EXCLUSIONS = "coverage";

  /**
   * @since 6.1
   */
  String SUBCATEGORY_EMAIL = "email";

  /**
   * @since 3.7
   */
  String CATEGORY_LICENSES = "licenses";

  /**
   * @since 4.0
   */
  String CATEGORY_TECHNICAL_DEBT = "technicalDebt";

  /* Global settings */
  String SONAR_HOME = "SONAR_HOME";

  /**
   * @deprecated since 6.7. This feature is deprecated in favor of the new branch feature.
   * @see <a href="https://redirect.sonarsource.com/doc/branches.html">https://redirect.sonarsource.com/doc/branches.html/a>
   */
  @Deprecated
  String PROJECT_BRANCH_PROPERTY = "sonar.branch";
  String PROJECT_VERSION_PROPERTY = "sonar.projectVersion";
  String BUILD_STRING_PROPERTY = "sonar.buildString";

  /**
   * @since 2.6
   */
  String PROJECT_KEY_PROPERTY = "sonar.projectKey";

  /**
   * @since 2.6
   */
  String PROJECT_NAME_PROPERTY = "sonar.projectName";

  /**
   * @since 2.6
   */
  String PROJECT_DESCRIPTION_PROPERTY = "sonar.projectDescription";

  /**
   * To determine value of this property use {@link FileSystem#encoding()}.
   *
   * @since 2.6
   */
  String ENCODING_PROPERTY = "sonar.sourceEncoding";

  /**
   * Value format is yyyy-MM-dd
   */
  String PROJECT_DATE_PROPERTY = "sonar.projectDate";

  /**
   * @since 6.6
   */
  String LONG_LIVED_BRANCHES_REGEX = "sonar.branch.longLivedBranches.regex";

  /* Exclusions */
  String PROJECT_INCLUSIONS_PROPERTY = "sonar.inclusions";
  String PROJECT_EXCLUSIONS_PROPERTY = "sonar.exclusions";

  /* Coverage exclusions */
  String PROJECT_COVERAGE_EXCLUSIONS_PROPERTY = "sonar.coverage.exclusions";

  /**
   * @since 3.3
   */
  String PROJECT_TEST_INCLUSIONS_PROPERTY = "sonar.test.inclusions";
  String PROJECT_TEST_EXCLUSIONS_PROPERTY = "sonar.test.exclusions";
  String GLOBAL_EXCLUSIONS_PROPERTY = "sonar.global.exclusions";
  String GLOBAL_TEST_EXCLUSIONS_PROPERTY = "sonar.global.test.exclusions";

  /* Sonar Core */

  String CORE_FORCE_AUTHENTICATION_PROPERTY = "sonar.forceAuthentication";
  boolean CORE_FORCE_AUTHENTICATION_DEFAULT_VALUE = false;

  /**
   * @deprecated since 6.3. This feature is not supported anymore
   * @see <a href="https://jira.sonarsource.com/browse/SONAR-7762">SONAR-7762/a>
   */
  @Deprecated
  String CORE_ALLOW_USERS_TO_SIGNUP_PROPERTY = "sonar.allowUsersToSignUp";

  /**
   * @deprecated since 6.4. The default group is hardcoded to 'sonar-users'
   * @see <a href="https://jira.sonarsource.com/browse/SONAR-9014">SONAR-9014/a>
   */
  @Deprecated
  String CORE_DEFAULT_GROUP = "sonar.defaultGroup";

  /**
   * @deprecated since 6.4. The default group is hardcoded to 'sonar-users'
   * @see <a href="https://jira.sonarsource.com/browse/SONAR-9014">SONAR-9014/a>
   */
  @Deprecated
  String CORE_DEFAULT_GROUP_DEFAULT_VALUE = "sonar-users";

  boolean CORE_ALLOW_USERS_TO_SIGNUP_DEAULT_VALUE = false;

  /**
   * @deprecated since 2.14. See http://jira.sonarsource.com/browse/SONAR-3153. Replaced by {@link #CORE_AUTHENTICATOR_REALM}.
   */
  @Deprecated
  String CORE_AUTHENTICATOR_CLASS = "sonar.authenticator.class";

  /**
   * @since 2.14
   * @deprecated since 7.1, this setting should not be used by plugin
   */
  @Deprecated
  String CORE_AUTHENTICATOR_REALM = "sonar.security.realm";

  /**
   * @deprecated since 7.1, this setting should not be used by plugin
   */
  @Deprecated
  String CORE_AUTHENTICATOR_IGNORE_STARTUP_FAILURE = "sonar.authenticator.ignoreStartupFailure";

  /**
   * @deprecated since 6.3. This feature is not supported anymore
   * @see <a href="https://jira.sonarsource.com/browse/SONAR-8208">SONAR-8208/a>
   */
  @Deprecated
  String CORE_AUTHENTICATOR_CREATE_USERS = "sonar.authenticator.createUsers";

  /**
   * @since 3.6
   * @deprecated since 5.4. This feature is not supported anymore. See http://jira.sonarsource.com/browse/SONAR-7219
   */
  @Deprecated
  String CORE_AUTHENTICATOR_UPDATE_USER_ATTRIBUTES = "sonar.security.updateUserAttributes";

  String SERVER_ID = "sonar.core.id";

  // format is yyyy-MM-dd'T'HH:mm:ssZ
  String SERVER_STARTTIME = "sonar.core.startTime";

  /**
   * This property defines the SonarQubeServer base url, such as <i>http://yourhost.yourdomain/sonar</i>.
   * When this property is not set, the base url of the SonarQube server is provided by {@link Server#getURL()}.
   *
   * @since 2.10
   */
  String SERVER_BASE_URL = "sonar.core.serverBaseURL";

  /**
   * @see #SERVER_BASE_URL
   * @since 2.10
   * @deprecated since 5.6. This constant default value is incorrect if a host and/or a port and/or a context have been configured.
   *             The correct default value when {@link #SERVER_BASE_URL} is not set is provided by {@link Server#getURL()}.
   */
  @Deprecated
  String SERVER_BASE_URL_DEFAULT_VALUE = "http://localhost:9000";

  /**
   * @since 2.11
   * @deprecated since 6.7
   */
  @Deprecated
  String CPD_CROSS_PROJECT = "sonar.cpd.cross_project";

  /**
   * @since 3.5
   */
  String CPD_EXCLUSIONS = "sonar.cpd.exclusions";

  /**
   * @since 2.11
   * @deprecated in 6.7. See {@link Server#getPermanentServerId()}
   */
  @Deprecated
  String ORGANISATION = "sonar.organisation";

  /**
   * @since 2.11
   * @deprecated in 6.7. See {@link Server#getPermanentServerId()}
   */
  @Deprecated
  String PERMANENT_SERVER_ID = "sonar.server_id";

  /**
   * @since 2.11
   * @deprecated in 6.7. See {@link Server#getPermanentServerId()}
   */
  @Deprecated
  String SERVER_ID_IP_ADDRESS = "sonar.server_id.ip_address";

  /**
   * @since 3.3
   */
  String LINKS_HOME_PAGE = "sonar.links.homepage";

  /**
   * @since 3.3
   */
  String LINKS_CI = "sonar.links.ci";

  /**
   * @since 3.3
   */
  String LINKS_ISSUE_TRACKER = "sonar.links.issue";

  /**
   * @since 3.3
   */
  String LINKS_SOURCES = "sonar.links.scm";

  /**
   * @since 3.3
   * @deprecated since 7.1, developer connection link is no more feed
   */
  @Deprecated
  String LINKS_SOURCES_DEV = "sonar.links.scm_dev";

  /**
   * @since 3.4
   */
  String LOGIN = "sonar.login";

  /**
   * @since 3.4
   */
  String PASSWORD = "sonar.password";

  /**
   * @since 3.5
   * @deprecated since 7.6
   */
  @Deprecated
  String TASK = "sonar.task";

  /**
   * @since 3.6
   * @deprecated since 7.6
   */
  @Deprecated
  String SCAN_TASK = "scan";

  /**
   * @since 3.6
   */
  // TODO remove?
  String PROFILING_LOG_PROPERTY = "sonar.showProfiling";

  /**
   * @since 4.0
   * @deprecated replaced in 5.2 by the permission 'provisioning'
   */
  @Deprecated
  String CORE_PREVENT_AUTOMATIC_PROJECT_CREATION = "sonar.preventAutoProjectCreation";

  /**
   * @since 4.0
   */
  String WORKING_DIRECTORY = "sonar.working.directory";

  String WORKING_DIRECTORY_DEFAULT_VALUE = ".sonar";

  /**
   * @since 5.2
   */
  String GLOBAL_WORKING_DIRECTORY = "sonar.globalWorking.directory";
  String GLOBAL_WORKING_DIRECTORY_DEFAULT_VALUE = "";

  /**
   * @since 4.2
   * @deprecated no more used since 5.5
   */
  @Deprecated
  String CORE_AUTHENTICATOR_LOCAL_USERS = "sonar.security.localUsers";

  /**
   * @since 4.0
   * @deprecated no more used since 6.3. See https://jira.sonarsource.com/browse/SONAR-8610
   */
  @Deprecated
  String HOURS_IN_DAY = "sonar.technicalDebt.hoursInDay";

  /**
   * @since 4.5
   * @deprecated no used anymore since 5.2
   */
  @Deprecated
  String SIZE_METRIC = "sonar.technicalDebt.sizeMetric";

  /**
   * @since 4.5
   */
  String DEVELOPMENT_COST = "sonar.technicalDebt.developmentCost";

  /**
   * @since 4.5
   */
  String DEVELOPMENT_COST_DEF_VALUE = "30";

  /**
   * @since 4.5
   */
  String RATING_GRID = "sonar.technicalDebt.ratingGrid";

  /**
   * @since 4.5
   */
  String RATING_GRID_DEF_VALUES = "0.05,0.1,0.2,0.5";

  /**
   * @since 4.5
   */
  String LANGUAGE_SPECIFIC_PARAMETERS = "languageSpecificParameters";

  /**
   * @since 4.5
   */
  String LANGUAGE_SPECIFIC_PARAMETERS_LANGUAGE_KEY = "language";

  /**
   * @since 4.5
   */
  String LANGUAGE_SPECIFIC_PARAMETERS_MAN_DAYS_KEY = "man_days";

  /**
   * @since 4.5
   */
  String LANGUAGE_SPECIFIC_PARAMETERS_SIZE_METRIC_KEY = "size_metric";

  /**
   * @since 5.0
   */
  String CATEGORY_SCM = "scm";

  /**
   * @since 5.0
   */
  String SCM_DISABLED_KEY = "sonar.scm.disabled";

  /**
   * @since 7.6
   */
  String SCM_EXCLUSIONS_DISABLED_KEY = "sonar.scm.exclusions.disabled";

  /**
   * @since 5.0
   */
  String SCM_PROVIDER_KEY = "sonar.scm.provider";

  /**
   * @since 5.1
   * @deprecated since 6.3. No longer taken into consideration as all files are always imported.
   */
  @Deprecated
  String IMPORT_UNKNOWN_FILES_KEY = "sonar.import_unknown_files";

  /**
   * @since 5.1
   */
  String DEFAULT_ISSUE_ASSIGNEE = "sonar.issues.defaultAssigneeLogin";

  /**
   * @since 7.6
   */
  String MODULE_LEVEL_ARCHIVED_SETTINGS = "sonar.subproject.settings.archived";
}
