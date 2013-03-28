/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api;

/**
 * CoreProperties is used to group various properties of Sonar as well
 * as default values of configuration in a single place
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
   * @since 2.11
   */
  String CATEGORY_CODE_COVERAGE = "codeCoverage";

  /**
   * @since 2.11
   */
  String CATEGORY_DUPLICATIONS = "duplications";

  /**
   * @since 2.11
   */
  String CATEGORY_SECURITY = "security";

  /**
   * @since 2.11
   */
  String CATEGORY_L10N = "localization";

  /**
   * @since 2.11
   */
  String CATEGORY_JAVA = "java";

  /**
   * @since 2.11
   */
  String CATEGORY_DIFFERENTIAL_VIEWS = "differentialViews";

  /**
   * @since 3.3
   */
  String CATEGORY_EXCLUSIONS = "exclusions";

  /* Global settings */
  String SONAR_HOME = "SONAR_HOME";
  String PROJECT_BRANCH_PROPERTY = "sonar.branch";
  String PROJECT_VERSION_PROPERTY = "sonar.projectVersion";

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
   * To determine value of this property use {@link org.sonar.api.resources.ProjectFileSystem#getSourceCharset()}.
   *
   * @since 2.6
   */
  String ENCODING_PROPERTY = "sonar.sourceEncoding";

  /**
   * Value format is yyyy-MM-dd
   */
  String PROJECT_DATE_PROPERTY = "sonar.projectDate";

  String PROJECT_LANGUAGE_PROPERTY = "sonar.language";
  String DYNAMIC_ANALYSIS_PROPERTY = "sonar.dynamicAnalysis";

  /* Exclusions */
  String PROJECT_INCLUSIONS_PROPERTY = "sonar.inclusions";
  String PROJECT_EXCLUSIONS_PROPERTY = "sonar.exclusions";

  /**
   * @since 3.3
   */
  String PROJECT_TEST_INCLUSIONS_PROPERTY = "sonar.test.inclusions";
  String PROJECT_TEST_EXCLUSIONS_PROPERTY = "sonar.test.exclusions";
  String GLOBAL_EXCLUSIONS_PROPERTY = "sonar.global.exclusions";
  String GLOBAL_TEST_EXCLUSIONS_PROPERTY = "sonar.global.test.exclusions";
  String GLOBAL_TEST_EXCLUSIONS_DEFAULT = "**/package-info.java";

  /**
   * @deprecated since 2.5. See discussion from http://jira.codehaus.org/browse/SONAR-1873
   */
  @Deprecated
  String REUSE_RULES_CONFIGURATION_PROPERTY = "sonar.reuseExistingRulesConfiguration";

  /* Sonar Core */
  String CORE_VIOLATION_LOCALE_PROPERTY = "sonar.violationLocale";
  String CORE_VIOLATION_LOCALE_DEFAULT_VALUE = "en";
  String CORE_IMPORT_SOURCES_PROPERTY = "sonar.importSources";
  boolean CORE_IMPORT_SOURCES_DEFAULT_VALUE = true;
  String CORE_SKIPPED_MODULES_PROPERTY = "sonar.skippedModules";
  String CORE_RULE_WEIGHTS_PROPERTY = "sonar.core.rule.weight";
  String CORE_RULE_WEIGHTS_DEFAULT_VALUE = "INFO=0;MINOR=1;MAJOR=3;CRITICAL=5;BLOCKER=10";

  /**
   * @deprecated since 3.6. See http://jira.codehaus.org/browse/SONAR-4145
   */
  @Deprecated
  String CORE_TENDENCY_DEPTH_PROPERTY = "tendency.depth";

  /**
   * @deprecated since 2.5. See http://jira.codehaus.org/browse/SONAR-4145
   */
  @Deprecated
  int CORE_TENDENCY_DEPTH_DEFAULT_VALUE = 30;

  String CORE_FORCE_AUTHENTICATION_PROPERTY = "sonar.forceAuthentication";
  boolean CORE_FORCE_AUTHENTICATION_DEFAULT_VALUE = false;
  String CORE_ALLOW_USERS_TO_SIGNUP_PROPERTY = "sonar.allowUsersToSignUp";
  String CORE_DEFAULT_GROUP = "sonar.defaultGroup";
  String CORE_DEFAULT_GROUP_DEFAULT_VALUE = "sonar-users";
  boolean CORE_ALLOW_USERS_TO_SIGNUP_DEAULT_VALUE = false;

  /**
   * @deprecated since 2.14. See http://jira.codehaus.org/browse/SONAR-3153. Replaced by {@link #CORE_AUTHENTICATOR_REALM}.
   */
  @Deprecated
  String CORE_AUTHENTICATOR_CLASS = "sonar.authenticator.class";

  /**
   * @since 2.14
   */
  String CORE_AUTHENTICATOR_REALM = "sonar.security.realm";

  String CORE_AUTHENTICATOR_IGNORE_STARTUP_FAILURE = "sonar.authenticator.ignoreStartupFailure";
  String CORE_AUTHENTICATOR_CREATE_USERS = "sonar.authenticator.createUsers";
  String SERVER_VERSION = "sonar.core.version";
  String SERVER_ID = "sonar.core.id";

  // format is yyyy-MM-dd'T'HH:mm:ssZ
  String SERVER_STARTTIME = "sonar.core.startTime";

  /**
   * @deprecated since 3.6. See http://jira.codehaus.org/browse/SONAR-4145
   */
  @Deprecated
  String SKIP_TENDENCIES_PROPERTY = "sonar.skipTendencies";

  /**
   * @deprecated since 3.6. See http://jira.codehaus.org/browse/SONAR-4145
   */
  @Deprecated
  boolean SKIP_TENDENCIES_DEFAULT_VALUE = false;

  String BATCH_INCLUDE_PLUGINS = "sonar.includePlugins";
  String BATCH_EXCLUDE_PLUGINS = "sonar.excludePlugins";

  /**
   * @since 3.4
   */
  String DRY_RUN_INCLUDE_PLUGINS = "sonar.dryRun.includePlugins";
  String DRY_RUN_INCLUDE_PLUGINS_DEFAULT_VALUE = "";

  /**
   * @since 3.4
   */
  String DRY_RUN_EXCLUDE_PLUGINS = "sonar.dryRun.excludePlugins";
  String DRY_RUN_EXCLUDE_PLUGINS_DEFAULT_VALUE = "devcockpit,pdfreport,report,scmactivity,views";

  /**
   * @since 2.10
   */
  String SERVER_BASE_URL = "sonar.core.serverBaseURL";

  /**
   * @see #SERVER_BASE_URL
   * @since 2.10
   */
  String SERVER_BASE_URL_DEFAULT_VALUE = "http://localhost:9000";

  /* CPD */
  String CPD_PLUGIN = "cpd";

  /**
   * @deprecated in 3.1
   */
  @Deprecated
  String CPD_MINIMUM_TOKENS_PROPERTY = "sonar.cpd.minimumTokens";

  /**
   * @deprecated in 3.1
   */
  @Deprecated
  int CPD_MINIMUM_TOKENS_DEFAULT_VALUE = 100;

  /**
   * @deprecated in 3.1
   */
  @Deprecated
  String CPD_IGNORE_LITERALS_PROPERTY = "sonar.cpd.ignore_literals";

  /**
   * @deprecated in 3.1
   */
  @Deprecated
  String CPD_IGNORE_LITERALS_DEFAULT_VALUE = "true";

  /**
   * @deprecated in 3.1
   */
  @Deprecated
  String CPD_IGNORE_IDENTIFIERS_PROPERTY = "sonar.cpd.ignore_identifiers";

  /**
   * @deprecated in 3.1
   */
  @Deprecated
  String CPD_IGNORE_IDENTIFIERS_DEFAULT_VALUE = "false";

  String CPD_SKIP_PROPERTY = "sonar.cpd.skip";

  /**
   * @since 2.11
   * @deprecated in 3.1
   */
  @Deprecated
  String CPD_ENGINE = "sonar.cpd.engine";

  /**
   * @see #CPD_ENGINE
   * @since 2.11
   * @deprecated in 3.1
   */
  @Deprecated
  String CPD_ENGINE_DEFAULT_VALUE = "sonar";

  /**
   * @since 2.11
   */
  String CPD_CROSS_RPOJECT = "sonar.cpd.cross_project";

  /**
   * @see #CPD_CROSS_RPOJECT
   * @since 2.11
   */
  boolean CPD_CROSS_RPOJECT_DEFAULT_VALUE = false;

  /**
   * @since 3.5
   */
  String CPD_EXCLUSIONS = "sonar.cpd.exclusions";

  /* Design */

  /**
   * Indicates whether Java bytecode analysis should be skipped.
   *
   * @since 2.0
   */
  String DESIGN_SKIP_DESIGN_PROPERTY = "sonar.skipDesign";
  boolean DESIGN_SKIP_DESIGN_DEFAULT_VALUE = false;

  /**
   * Indicates whether Package Design Analysis should be skipped.
   *
   * @since 2.9
   */
  String DESIGN_SKIP_PACKAGE_DESIGN_PROPERTY = "sonar.skipPackageDesign";
  boolean DESIGN_SKIP_PACKAGE_DESIGN_DEFAULT_VALUE = false;

  /* Google Analytics */
  String GOOGLE_ANALYTICS_PLUGIN = "google-analytics";
  String GOOGLE_ANALYTICS_ACCOUNT_PROPERTY = "sonar.google-analytics.account";

  /* Time machine periods */
  String TIMEMACHINE_PREFIX = "sonar.timemachine";
  String TIMEMACHINE_PERIOD_SUFFIX = "period";
  String TIMEMACHINE_PERIOD_PREFIX = TIMEMACHINE_PREFIX + "." + TIMEMACHINE_PERIOD_SUFFIX;
  String TIMEMACHINE_MODE_PREVIOUS_ANALYSIS = "previous_analysis";
  String TIMEMACHINE_MODE_DATE = "date";
  String TIMEMACHINE_MODE_VERSION = "version";
  String TIMEMACHINE_MODE_DAYS = "days";
  String TIMEMACHINE_MODE_PREVIOUS_VERSION = "previous_version";
  String TIMEMACHINE_DEFAULT_PERIOD_1 = TIMEMACHINE_MODE_PREVIOUS_ANALYSIS;
  String TIMEMACHINE_DEFAULT_PERIOD_2 = "5";
  String TIMEMACHINE_DEFAULT_PERIOD_3 = "30";
  String TIMEMACHINE_DEFAULT_PERIOD_4 = "";
  String TIMEMACHINE_DEFAULT_PERIOD_5 = "";

  /**
   * @since 2.11
   */
  String ORGANISATION = "sonar.organisation";

  /**
   * @since 2.11
   */
  String PERMANENT_SERVER_ID = "sonar.server_id";

  /**
   * @since 2.11
   */
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
   */
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
   * @since 3.4
   */
  String DRY_RUN = "sonar.dryRun";

  /**
   * @since 3.5
   */
  String TASK = "sonar.task";

  /**
   * @since 3.6
   */
  String SCAN_TASK = "scan";

  /**
   * @deprecated replaced in v3.4 by properties specific to languages, for example sonar.java.coveragePlugin
   *             See http://jira.codehaus.org/browse/SONARJAVA-39 for more details.
   */
  @Deprecated
  String CORE_COVERAGE_PLUGIN_PROPERTY = "sonar.core.codeCoveragePlugin";
}
