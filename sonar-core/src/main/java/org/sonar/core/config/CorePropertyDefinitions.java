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
package org.sonar.core.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import static org.sonar.api.database.DatabaseProperties.PROP_PASSWORD;

public class CorePropertyDefinitions {

  /* Time machine periods */
  public static final String TIMEMACHINE_PERIOD_PREFIX = "sonar.timemachine.period";
  public static final String TIMEMACHINE_MODE_PREVIOUS_ANALYSIS = "previous_analysis";
  public static final String TIMEMACHINE_MODE_DATE = "date";
  public static final String TIMEMACHINE_MODE_VERSION = "version";
  public static final String TIMEMACHINE_MODE_DAYS = "days";
  public static final String TIMEMACHINE_MODE_PREVIOUS_VERSION = "previous_version";

  private static final String TIMEMACHINE_DEFAULT_PERIOD_1 = TIMEMACHINE_MODE_PREVIOUS_VERSION;
  private static final String TIMEMACHINE_DEFAULT_PERIOD_2 = TIMEMACHINE_MODE_PREVIOUS_ANALYSIS;
  private static final String TIMEMACHINE_DEFAULT_PERIOD_3 = "30";
  private static final String TIMEMACHINE_DEFAULT_PERIOD_4 = "";
  private static final String TIMEMACHINE_DEFAULT_PERIOD_5 = "";

  private static final String CATEGORY_ORGANIZATIONS = "organizations";
  public static final String ORGANIZATIONS_ANYONE_CAN_CREATE = "sonar.organizations.anyoneCanCreate";

  private CorePropertyDefinitions() {
    // only static stuff
  }

  public static List<PropertyDefinition> all() {
    List<PropertyDefinition> defs = Lists.newArrayList();
    defs.addAll(IssueExclusionProperties.all());
    defs.addAll(ExclusionProperties.all());
    defs.addAll(SecurityProperties.all());
    defs.addAll(DebtProperties.all());
    defs.addAll(PurgeProperties.all());
    defs.addAll(EmailSettings.definitions());
    defs.addAll(WebhookProperties.all());

    defs.addAll(ImmutableList.of(
      PropertyDefinition.builder(PROP_PASSWORD)
        .type(PropertyType.PASSWORD)
        .hidden()
        .build(),
      PropertyDefinition.builder(CoreProperties.SERVER_BASE_URL)
        .name("Server base URL")
        .description("HTTP URL of this SonarQube server, such as <i>http://yourhost.yourdomain/sonar</i>. This value is used i.e. to create links in emails.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .build(),

      PropertyDefinition.builder(CoreProperties.LINKS_HOME_PAGE)
        .name("Project Home Page")
        .description("HTTP URL of the home page of the project.")
        .hidden()
        .build(),
      PropertyDefinition.builder(CoreProperties.LINKS_CI)
        .name("CI server")
        .description("HTTP URL of the continuous integration server.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .build(),
      PropertyDefinition.builder(CoreProperties.LINKS_ISSUE_TRACKER)
        .name("Issue Tracker")
        .description("HTTP URL of the issue tracker.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .hidden()
        .build(),
      PropertyDefinition.builder(CoreProperties.LINKS_SOURCES)
        .name("SCM server")
        .description("HTTP URL of the server which hosts the sources of the project.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .build(),
      PropertyDefinition.builder(CoreProperties.LINKS_SOURCES_DEV)
        .name("SCM connection for developers")
        .description("HTTP URL used by developers to connect to the SCM server for the project.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .hidden()
        .build(),
      PropertyDefinition.builder(CoreProperties.ANALYSIS_MODE)
        .name("Analysis mode")
        .type(PropertyType.SINGLE_SELECT_LIST)
        .options(Arrays.asList(CoreProperties.ANALYSIS_MODE_ANALYSIS, CoreProperties.ANALYSIS_MODE_PREVIEW, CoreProperties.ANALYSIS_MODE_INCREMENTAL))
        .category(CoreProperties.CATEGORY_GENERAL)
        .defaultValue(CoreProperties.ANALYSIS_MODE_ANALYSIS)
        .hidden()
        .build(),
      PropertyDefinition.builder(CoreProperties.PREVIEW_INCLUDE_PLUGINS)
        .name("Plugins accepted for Preview mode")
        .description("Comma-separated list of plugin keys. Those plugins will be used during preview analyses.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .defaultValue(CoreProperties.PREVIEW_INCLUDE_PLUGINS_DEFAULT_VALUE)
        .build(),
      PropertyDefinition.builder(CoreProperties.PREVIEW_EXCLUDE_PLUGINS)
        .name("Plugins excluded for Preview mode")
        .description("Comma-separated list of plugin keys. Those plugins will not be used during preview analyses.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .defaultValue(CoreProperties.PREVIEW_EXCLUDE_PLUGINS_DEFAULT_VALUE)
        .build(),
      PropertyDefinition.builder(CoreProperties.CORE_AUTHENTICATOR_REALM)
        .name("Security Realm")
        .hidden()
        .build(),
      PropertyDefinition.builder("sonar.authenticator.downcase")
        .name("Downcase login")
        .description("Downcase login during user authentication, typically for Active Directory")
        .type(PropertyType.BOOLEAN)
        .defaultValue(String.valueOf(false))
        .hidden()
        .build(),
      PropertyDefinition.builder(CoreProperties.CORE_AUTHENTICATOR_CREATE_USERS)
        .name("Create user accounts")
        .description("Create accounts when authenticating users via an external system")
        .type(PropertyType.BOOLEAN)
        .defaultValue(String.valueOf(true))
        .hidden()
        .build(),
      PropertyDefinition.builder(CoreProperties.CORE_AUTHENTICATOR_IGNORE_STARTUP_FAILURE)
        .name("Ignore failures during authenticator startup")
        .type(PropertyType.BOOLEAN)
        .defaultValue(String.valueOf(false))
        .hidden()
        .build(),
      PropertyDefinition.builder("sonar.enableFileVariation")
        .name("Enable file variation")
        .hidden()
        .type(PropertyType.BOOLEAN)
        .defaultValue(String.valueOf(false))
        .build(),
      PropertyDefinition.builder(CoreProperties.SCM_DISABLED_KEY)
        .name("Disable the SCM Sensor")
        .description("Disable the retrieval of blame information from Source Control Manager")
        .category(CoreProperties.CATEGORY_SCM)
        .type(PropertyType.BOOLEAN)
        .onQualifiers(Qualifiers.PROJECT)
        .defaultValue(String.valueOf(false))
        .build(),
      PropertyDefinition.builder(CoreProperties.SCM_PROVIDER_KEY)
        .name("Key of the SCM provider for this project")
        .description("Force the provider to be used to get SCM information for this project. By default auto-detection is done. Example: svn, git.")
        .category(CoreProperties.CATEGORY_SCM)
        .onlyOnQualifiers(Qualifiers.PROJECT)
        .build(),

      // WEB LOOK&FEEL
      PropertyDefinition.builder("sonar.lf.logoUrl")
        .deprecatedKey("sonar.branding.image")
        .name("Logo URL")
        .description("URL to logo image. Any standard format is accepted.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_LOOKNFEEL)
        .build(),
      PropertyDefinition.builder("sonar.lf.logoWidthPx")
        .deprecatedKey("sonar.branding.image.width")
        .name("Width of image in pixels")
        .description("Width in pixels, given that the height of the the image is constrained to 30px")
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_LOOKNFEEL)
        .build(),
      PropertyDefinition.builder("sonar.lf.enableGravatar")
        .name("Enable support of gravatars")
        .description("Gravatars are profile pictures of users based on their email.")
        .type(PropertyType.BOOLEAN)
        .defaultValue(String.valueOf(true))
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_LOOKNFEEL)
        .build(),
      PropertyDefinition.builder("sonar.lf.gravatarServerUrl")
        .name("Gravatar URL")
        .description("Optional URL of custom Gravatar service. Accepted variables are {EMAIL_MD5} for MD5 hash of email and {SIZE} for the picture size in pixels.")
        .defaultValue("https://secure.gravatar.com/avatar/{EMAIL_MD5}.jpg?s={SIZE}&d=identicon")
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_LOOKNFEEL)
        .build(),
      PropertyDefinition.builder("sonar.lf.landingText")
        .name("Landing page text")
        .description("Optional text that is display on the landing page. Supports html.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_LOOKNFEEL)
        .type(PropertyType.TEXT)
        .build(),

      // ISSUES
      PropertyDefinition.builder(CoreProperties.DEFAULT_ISSUE_ASSIGNEE)
        .name("Default Assignee")
        .description("New issues will be assigned to this user each time it is not possible to determine the user who is the author of the issue.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_ISSUES)
        .onQualifiers(Qualifiers.PROJECT)
        .type(PropertyType.USER_LOGIN)
        .build(),

      // SCANNER
      PropertyDefinition.builder(TIMEMACHINE_PERIOD_PREFIX + 1)
        .name("Leak Period")
        .description("Period used to compare measures and track new issues. Values are : <ul class='bullet'><li>Number of days before " +
          "analysis, for example 5.</li><li>A custom date. Format is yyyy-MM-dd, for example 2010-12-25</li><li>'previous_analysis' to " +
          "compare to previous analysis</li><li>'previous_version' to compare to the previous version in the project history</li>" +
          "<li>A version, for example '1.2' or 'BASELINE'</li></ul>" +
          "<p>When specifying a number of days or a date, the snapshot selected for comparison is " +
          " the first one available inside the corresponding time range. </p>" +
          "<p>Changing this property only takes effect after subsequent project inspections.<p/>")
        .defaultValue(TIMEMACHINE_DEFAULT_PERIOD_1)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DIFFERENTIAL_VIEWS)
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.VIEW)
        .build(),

      PropertyDefinition.builder(TIMEMACHINE_PERIOD_PREFIX + 2)
        .name("Period 2")
        .description("See the property 'Leak Period'")
        .defaultValue(TIMEMACHINE_DEFAULT_PERIOD_2)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DIFFERENTIAL_VIEWS)
        .build(),

      PropertyDefinition.builder(TIMEMACHINE_PERIOD_PREFIX + 3)
        .name("Period 3")
        .description("See the property 'Leak Period'")
        .defaultValue(TIMEMACHINE_DEFAULT_PERIOD_3)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DIFFERENTIAL_VIEWS)
        .build(),

      PropertyDefinition.builder(TIMEMACHINE_PERIOD_PREFIX + 4)
        .name("Period 4")
        .description("Period used to compare measures and track new issues. This property is specific to the project. Values are : " +
          "<ul class='bullet'><li>Number of days before analysis, for example 5.</li><li>A custom date. Format is yyyy-MM-dd, " +
          "for example 2010-12-25</li><li>'previous_analysis' to compare to previous analysis</li>" +
          "<li>'previous_version' to compare to the previous version in the project history</li><li>A version, for example '1.2' or 'BASELINE'</li></ul>" +
          "<p>When specifying a number of days or a date, the snapshot selected for comparison is the first one available inside the corresponding time range. </p>" +
          "<p>Changing this property only takes effect after subsequent project inspections.<p/>")
        .defaultValue(TIMEMACHINE_DEFAULT_PERIOD_4)
        .onlyOnQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DIFFERENTIAL_VIEWS)
        .build(),

      PropertyDefinition.builder(TIMEMACHINE_PERIOD_PREFIX + 5)
        .name("Period 5")
        .description("See the property 'Period 4'")
        .defaultValue(TIMEMACHINE_DEFAULT_PERIOD_5)
        .onlyOnQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DIFFERENTIAL_VIEWS)
        .build(),

      // CPD
      PropertyDefinition.builder(CoreProperties.CPD_CROSS_PROJECT)
        .defaultValue(Boolean.toString(CoreProperties.CPD_CROSS_PROJECT_DEFAULT_VALUE))
        .name("Cross project duplication detection")
        .description("By default, SonarQube detects duplications at sub-project level. This means that a block "
          + "duplicated on two sub-projects of the same project won't be reported. Setting this parameter to \"true\" "
          + "allows to detect duplicates across sub-projects and more generally across projects. Note that activating "
          + "this property will slightly increase each SonarQube analysis time.")
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DUPLICATIONS)
        .type(PropertyType.BOOLEAN)
        .build(),
      PropertyDefinition.builder(CoreProperties.CPD_EXCLUSIONS)
        .defaultValue("")
        .name("Duplication Exclusions")
        .description("Patterns used to exclude some source files from the duplication detection mechanism. " +
          "See below to know how to use wildcards to specify this property.")
        .onQualifiers(Qualifiers.PROJECT, Qualifiers.MODULE)
        .category(CoreProperties.CATEGORY_EXCLUSIONS)
        .subCategory(CoreProperties.SUBCATEGORY_DUPLICATIONS_EXCLUSIONS)
        .multiValues(true)
        .build(),

      // ORGANIZATIONS
      PropertyDefinition.builder(ORGANIZATIONS_ANYONE_CAN_CREATE)
        .name("Allow any authenticated user to create organizations")
        .defaultValue(Boolean.toString(false))
        .category(CATEGORY_ORGANIZATIONS)
        .type(PropertyType.BOOLEAN)
        .build()));
    return defs;
  }
}
