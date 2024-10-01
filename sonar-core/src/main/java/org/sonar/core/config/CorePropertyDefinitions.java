/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.core.extension.PluginRiskConsent;

import static java.util.Arrays.asList;
import static org.sonar.api.PropertyType.*;
import static org.sonar.core.extension.PluginRiskConsent.NOT_ACCEPTED;

public class CorePropertyDefinitions {

  public static final String SONAR_ANALYSIS = "sonar.analysis.";
  public static final String SONAR_PROJECTCREATION_MAINBRANCHNAME = "sonar.projectCreation.mainBranchName";
  public static final String SONAR_ANALYSIS_DETECTEDSCM = "sonar.analysis.detectedscm";
  public static final String SONAR_ANALYSIS_DETECTEDCI = "sonar.analysis.detectedci";

  public static final String DISABLE_NOTIFICATION_ON_BUILT_IN_QPROFILES = "sonar.builtInQualityProfiles.disableNotificationOnUpdate";

  public static final String PLUGINS_RISK_CONSENT = "sonar.plugins.risk.consent";
  public static final String SUBCATEGORY_PROJECT_CREATION = "subProjectCreation";

  public static final String SYSTEM_MEASURES_MIGRATION_ENABLED = "system.measures.migration.enabled";

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
        .type(TEXT)
        .build(),
      PropertyDefinition.builder(CoreProperties.SERVER_BASE_URL)
        .name("Server base URL")
        .description("HTTP(S) URL of this SonarQube server, such as <i>https://yourhost.yourdomain/sonar</i>. This value is used outside SonarQube itself, e.g. for PR decoration, emails, etc.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .build(),
      PropertyDefinition.builder(SONAR_PROJECTCREATION_MAINBRANCHNAME)
        .name("Default main branch name")
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(SUBCATEGORY_PROJECT_CREATION)
        .description("Each project has a main branch at creation. This setting defines the instance-wide default main branch name. "
          + " A user can override this when creating a project. This setting does not apply to projects imported from a DevOps platform.")
        .type(STRING)
        .defaultValue("main")
        .build(),
      PropertyDefinition.builder(CoreProperties.ENCRYPTION_SECRET_KEY_PATH)
        .name("Encryption secret key path")
        .description("Path to a file that contains encryption secret key that is used to encrypting other settings.")
        .type(STRING)
        .hidden()
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
      PropertyDefinition.builder(PLUGINS_RISK_CONSENT)
        .name("State of user plugins risk consent")
        .description("Determine whether user is required to accept plugins risk consent")
        .defaultValue(NOT_ACCEPTED.name())
        .options(Arrays.stream(PluginRiskConsent.values()).map(Enum::name).collect(Collectors.toList()))
        .hidden()
        .type(SINGLE_SELECT_LIST)
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

      // ISSUES
      PropertyDefinition.builder(CoreProperties.DEVELOPER_AGGREGATED_INFO_DISABLED)
        .name("Disable developer aggregated information")
        .description("Don't show issue facets aggregating information per developer")
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_ISSUES)
        .onQualifiers(Qualifiers.PROJECT)
        .type(BOOLEAN)
        .defaultValue(Boolean.toString(false))
        .build(),

      PropertyDefinition.builder(CoreProperties.DEFAULT_ISSUE_ASSIGNEE)
        .name("Default Assignee")
        .description("New issues will be assigned to this user each time it is not possible to determine the user who is the author of the issue.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_ISSUES)
        .onQualifiers(Qualifiers.PROJECT)
        .type(PropertyType.USER_LOGIN)
        .build(),

      // QUALITY GATE
      PropertyDefinition.builder(CoreProperties.QUALITY_GATE_IGNORE_SMALL_CHANGES)
        .name("Ignore duplication and coverage on small changes")
        .description("Quality Gate conditions about duplications in new code and coverage on new code are ignored until the number of new lines is at least 20.")
        .category(CoreProperties.CATEGORY_GENERAL)
        .subCategory(CoreProperties.SUBCATEGORY_QUALITY_GATE)
        .onQualifiers(Qualifiers.PROJECT)
        .type(BOOLEAN)
        .defaultValue(Boolean.toString(true))
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
        .build()));

    return defs;
  }
}
