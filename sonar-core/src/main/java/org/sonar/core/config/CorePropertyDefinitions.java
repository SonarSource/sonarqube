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
package org.sonar.core.config;

import java.util.ArrayList;
import java.util.List;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import static java.util.Arrays.asList;
import static org.sonar.api.PropertyType.BOOLEAN;

public class CorePropertyDefinitions {

  public static final String LEAK_PERIOD = "sonar.leak.period";
  public static final String LEAK_PERIOD_MODE_DATE = "date";
  public static final String LEAK_PERIOD_MODE_VERSION = "version";
  public static final String LEAK_PERIOD_MODE_DAYS = "days";
  public static final String LEAK_PERIOD_MODE_PREVIOUS_VERSION = "previous_version";
  public static final String LEAK_PERIOD_MODE_MANUAL_BASELINE = "manual_baseline";
  public static final String SONAR_ANALYSIS = "sonar.analysis.";

  private static final String DEFAULT_LEAK_PERIOD = LEAK_PERIOD_MODE_PREVIOUS_VERSION;

  private static final String CATEGORY_ORGANIZATIONS = "organizations";
  public static final String ORGANIZATIONS_ANYONE_CAN_CREATE = "sonar.organizations.anyoneCanCreate";
  public static final String ORGANIZATIONS_CREATE_PERSONAL_ORG = "sonar.organizations.createPersonalOrg";
  public static final String DISABLE_NOTIFICATION_ON_BUILT_IN_QPROFILES = "sonar.builtInQualityProfiles.disableNotificationOnUpdate";

  private CorePropertyDefinitions() {
    // only static stuff
  }

  public static List<PropertyDefinition> all() {
    List<PropertyDefinition> defs = new ArrayList<>();
    defs.addAll(IssueExclusionProperties.all());
    defs.addAll(ExclusionProperties.all());
    defs.addAll(SecurityProperties.all());
    defs.addAll(DebtProperties.all());
    defs.addAll(PurgeProperties.all());
    defs.addAll(EmailSettings.definitions());
    defs.addAll(ScannerProperties.all());

    defs.addAll(asList(
      PropertyDefinition.builder(CoreProperties.MODULE_LEVEL_ARCHIVED_SETTINGS)
        .name("Archived Sub-Projects Settings")
        .description("DEPRECATED - List of the properties that were previously configured at sub-project / module level. " +
          "These properties are not used anymore and should now be configured at project level. When you've made the " +
          "necessary changes, clear this setting to prevent analysis from showing a warning about it.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_MODULES)
        .onlyOnQualifiers(Qualifiers.PROJECT)
        .type(PropertyType.TEXT)
        .build(),
      PropertyDefinition.builder(CoreProperties.SERVER_BASE_URL)
        .name("Server base URL")
        .description("HTTP URL of this SonarQube server, such as <i>http://yourhost.yourdomain/sonar</i>. This value is used i.e. to create links in emails.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .build(),

      PropertyDefinition.builder("sonar.authenticator.downcase")
        .name("Downcase login")
        .description("Downcase login during user authentication, typically for Active Directory")
        .type(BOOLEAN)
        .defaultValue(String.valueOf(false))
        .hidden()
        .build(),
      PropertyDefinition.builder(DISABLE_NOTIFICATION_ON_BUILT_IN_QPROFILES)
        .name("Avoid quality profiles notification")
        .description("Avoid sending email notification on each update of built-in quality profiles to quality profile administrators.")
        .defaultValue(Boolean.toString(false))
        .category(CoreProperties.CATEGORY_GENERAL)
        .type(BOOLEAN)
        .build(),

      // WEB LOOK&FEEL
      PropertyDefinition.builder(WebConstants.SONAR_LF_LOGO_URL)
        .deprecatedKey("sonar.branding.image")
        .name("Logo URL")
        .description("URL to logo image. Any standard format is accepted.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_LOOKNFEEL)
        .build(),
      PropertyDefinition.builder(WebConstants.SONAR_LF_LOGO_WIDTH_PX)
        .deprecatedKey("sonar.branding.image.width")
        .name("Width of image in pixels")
        .description("Width in pixels, given that the height of the the image is constrained to 30px.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_LOOKNFEEL)
        .build(),
      PropertyDefinition.builder(WebConstants.SONAR_LF_ENABLE_GRAVATAR)
        .name("Enable support of gravatars")
        .description("Gravatars are profile pictures of users based on their email.")
        .type(BOOLEAN)
        .defaultValue(String.valueOf(false))
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_LOOKNFEEL)
        .build(),
      PropertyDefinition.builder(WebConstants.SONAR_LF_GRAVATAR_SERVER_URL)
        .name("Gravatar URL")
        .description("Optional URL of custom Gravatar service. Accepted variables are {EMAIL_MD5} for MD5 hash of email and {SIZE} for the picture size in pixels.")
        .defaultValue("https://secure.gravatar.com/avatar/{EMAIL_MD5}.jpg?s={SIZE}&d=identicon")
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_LOOKNFEEL)
        .build(),
      PropertyDefinition.builder(WebConstants.SONAR_LF_ABOUT_TEXT)
        .name("About page text")
        .description("Optional text that is displayed on the About page. Supports html.")
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
      PropertyDefinition.builder(LEAK_PERIOD)
        .name("New Code Period")
        .deprecatedKey("sonar.timemachine.period1")
        .description("Period used to compare measures and track new issues. Values are : " +
          "<ul class='bullet'><li>Number of days before  analysis, for example 5.</li>" +
          "<li>A custom date. Format is yyyy-MM-dd, for example 2010-12-25</li>" +
          "<li>'previous_version' to compare to the previous version in the project history</li>" +
          "<li>A version, for example '1.2' or 'BASELINE'</li></ul>" +
          "<p>When specifying a number of days or a date, the snapshot selected for comparison is the first one available inside the corresponding time range. </p>" +
          "<p>This property has no effect when a baseline is manually set on a long-living branch, such as the main branch.<p/>" +
          "<p>Changing this property only takes effect after subsequent project inspections.<p/>")
        .defaultValue(DEFAULT_LEAK_PERIOD)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DIFFERENTIAL_VIEWS)
        .onQualifiers(Qualifiers.PROJECT)
        .build(),

      // CPD
      PropertyDefinition.builder(CoreProperties.CPD_CROSS_PROJECT)
        .defaultValue(Boolean.toString(false))
        .name("Cross project duplication detection")
        .description("DEPRECATED - By default, SonarQube detects duplications at project level. This means that a block "
          + "duplicated on two different projects won't be reported. Setting this parameter to \"true\" "
          + "allows to detect duplicates across projects. Note that activating "
          + "this property will significantly increase each SonarQube analysis time, "
          + "and therefore badly impact the performances of report processing as more and more projects "
          + "are getting involved in this cross project duplication mechanism.")
        .onQualifiers(Qualifiers.PROJECT)
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_DUPLICATIONS)
        .type(BOOLEAN)
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
        .name("Allow any authenticated user to create organizations.")
        .defaultValue(Boolean.toString(false))
        .category(CATEGORY_ORGANIZATIONS)
        .type(BOOLEAN)
        .hidden()
        .build(),
      PropertyDefinition.builder(ORGANIZATIONS_CREATE_PERSONAL_ORG)
        .name("Create an organization for each new user.")
        .defaultValue(Boolean.toString(false))
        .category(CATEGORY_ORGANIZATIONS)
        .type(BOOLEAN)
        .hidden()
        .build()));
    return defs;
  }
}
