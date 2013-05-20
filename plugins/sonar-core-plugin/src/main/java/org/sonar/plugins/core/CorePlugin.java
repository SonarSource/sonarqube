/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.plugins.core;

import com.google.common.collect.ImmutableList;
import org.sonar.api.*;
import org.sonar.api.checks.NoSonarFilter;
import org.sonar.api.notifications.NotificationDispatcherMetadata;
import org.sonar.api.resources.Java;
import org.sonar.core.timemachine.Periods;
import org.sonar.plugins.core.batch.IndexProjectPostJob;
import org.sonar.plugins.core.charts.DistributionAreaChart;
import org.sonar.plugins.core.charts.DistributionBarChart;
import org.sonar.plugins.core.charts.XradarChart;
import org.sonar.plugins.core.colorizers.JavaColorizerFormat;
import org.sonar.plugins.core.dashboards.*;
import org.sonar.plugins.core.issue.*;
import org.sonar.plugins.core.measurefilters.MyFavouritesFilter;
import org.sonar.plugins.core.measurefilters.ProjectFilter;
import org.sonar.plugins.core.notifications.alerts.NewAlerts;
import org.sonar.plugins.core.notifications.reviews.ChangesInReviewAssignedToMeOrCreatedByMe;
import org.sonar.plugins.core.notifications.reviews.NewFalsePositiveReview;
import org.sonar.plugins.core.notifications.violations.NewViolationsOnFirstDifferentialPeriod;
import org.sonar.plugins.core.security.ApplyProjectRolesDecorator;
import org.sonar.plugins.core.sensors.*;
import org.sonar.plugins.core.timemachine.*;
import org.sonar.plugins.core.web.Lcom4Viewer;
import org.sonar.plugins.core.web.TestsViewer;
import org.sonar.plugins.core.widgets.*;
import org.sonar.plugins.core.widgets.issues.ActionPlansWidget;
import org.sonar.plugins.core.widgets.issues.FalsePositiveIssuesWidget;
import org.sonar.plugins.core.widgets.issues.MyUnresolvedIssuesWidget;
import org.sonar.plugins.core.widgets.issues.UnresolvedIssuesPerAssigneeWidget;
import org.sonar.plugins.core.widgets.reviews.*;

import java.util.List;

@Properties({
  @Property(
    key = CoreProperties.TASK,
    name = "Task to be executed",
    defaultValue = CoreProperties.SCAN_TASK,
    module = false,
    project = false,
    global = false),
  @Property(
    key = CoreProperties.SERVER_BASE_URL,
    defaultValue = CoreProperties.SERVER_BASE_URL_DEFAULT_VALUE,
    name = "Server base URL",
    description = "HTTP URL of this Sonar server, such as <i>http://yourhost.yourdomain/sonar</i>. This value is used i.e. to create links in emails.",
    project = false,
    global = true,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.LINKS_HOME_PAGE,
    defaultValue = "",
    name = "Project Home Page",
    description = "HTTP URL of the home page of the project.",
    project = false,
    global = false,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.LINKS_CI,
    defaultValue = "",
    name = "CI server",
    description = "HTTP URL of the continuous integration server.",
    project = false,
    global = false,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.LINKS_ISSUE_TRACKER,
    defaultValue = "",
    name = "Issue Tracker",
    description = "HTTP URL of the issue tracker.",
    project = false,
    global = false,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.LINKS_SOURCES,
    defaultValue = "",
    name = "SCM server",
    description = "HTTP URL of the server which hosts the sources of the project.",
    project = false,
    global = false,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.LINKS_SOURCES_DEV,
    defaultValue = "",
    name = "SCM connection for developers",
    description = "HTTP URL used by developers to connect to the SCM server for the project.",
    project = false,
    global = false,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.PROJECT_LANGUAGE_PROPERTY,
    defaultValue = Java.KEY,
    name = "Default language",
    description = "Default language of the source code to analyse",
    project = false,
    global = true,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.PROJECT_INCLUSIONS_PROPERTY,
    name = "Source File Inclusions",
    project = true,
    global = true,
    multiValues = true,
    category = CoreProperties.CATEGORY_EXCLUSIONS),
  @Property(
    key = CoreProperties.PROJECT_TEST_INCLUSIONS_PROPERTY,
    name = "Test File Inclusions",
    project = true,
    global = true,
    multiValues = true,
    category = CoreProperties.CATEGORY_EXCLUSIONS),
  @Property(
    key = CoreProperties.GLOBAL_EXCLUSIONS_PROPERTY,
    name = "Global Source File Exclusions",
    multiValues = true,
    category = CoreProperties.CATEGORY_EXCLUSIONS),
  @Property(
    key = CoreProperties.GLOBAL_TEST_EXCLUSIONS_PROPERTY,
    name = "Global Test File Exclusions",
    multiValues = true,
    category = CoreProperties.CATEGORY_EXCLUSIONS,
    defaultValue = CoreProperties.GLOBAL_TEST_EXCLUSIONS_DEFAULT),
  @Property(
    key = CoreProperties.PROJECT_EXCLUSIONS_PROPERTY,
    name = "Source File Exclusions",
    project = true,
    global = true,
    multiValues = true,
    category = CoreProperties.CATEGORY_EXCLUSIONS),
  @Property(
    key = CoreProperties.PROJECT_TEST_EXCLUSIONS_PROPERTY,
    name = "Test File Exclusions",
    project = true,
    global = true,
    multiValues = true,
    category = CoreProperties.CATEGORY_EXCLUSIONS),
  @Property(
    key = CoreProperties.CORE_IMPORT_SOURCES_PROPERTY,
    defaultValue = "" + CoreProperties.CORE_IMPORT_SOURCES_DEFAULT_VALUE,
    name = "Import sources",
    description = "Set to false if sources should not be imported and therefore not available in the Web UI (e.g. for security reasons).",
    project = true,
    module = true,
    global = true,
    category = CoreProperties.CATEGORY_SECURITY,
    type = PropertyType.BOOLEAN),
  @Property(
    key = CoreProperties.CORE_SKIPPED_MODULES_PROPERTY,
    name = "Exclude modules",
    description = "Maven artifact ids of modules to exclude.",
    project = true,
    global = false,
    multiValues = true,
    category = CoreProperties.CATEGORY_EXCLUSIONS),
  @Property(
    key = CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY,
    defaultValue = "" + CoreProperties.CORE_FORCE_AUTHENTICATION_DEFAULT_VALUE,
    name = "Force user authentication",
    description = "Forcing user authentication stops un-logged users to access Sonar.",
    project = false,
    global = true,
    category = CoreProperties.CATEGORY_SECURITY,
    type = PropertyType.BOOLEAN),
  @Property(
    key = CoreProperties.CORE_ALLOW_USERS_TO_SIGNUP_PROPERTY,
    defaultValue = "" + CoreProperties.CORE_ALLOW_USERS_TO_SIGNUP_DEAULT_VALUE,
    name = "Allow users to sign up online",
    description = "Users can sign up online.",
    project = false,
    global = true,
    category = CoreProperties.CATEGORY_SECURITY,
    type = PropertyType.BOOLEAN),
  @Property(
    key = CoreProperties.CORE_DEFAULT_GROUP,
    defaultValue = CoreProperties.CORE_DEFAULT_GROUP_DEFAULT_VALUE,
    name = "Default user group",
    description = "Any new users will automatically join this group.",
    project = false,
    global = true,
    category = CoreProperties.CATEGORY_SECURITY),
  @Property(
    key = CoreProperties.CORE_VIOLATION_LOCALE_PROPERTY,
    defaultValue = "en",
    name = "Locale used for violation messages",
    description = "Locale to be used when generating violation messages. It's up to each rule engine to support this global internationalization property",
    project = true,
    global = true,
    category = CoreProperties.CATEGORY_L10N),
  @Property(
    key = "sonar.timemachine.period1",
    name = "Period 1",
    description = "Period used to compare measures and track new violations. Values are : <ul class='bullet'><li>Number of days before " +
      "analysis, for example 5.</li><li>A custom date. Format is yyyy-MM-dd, for example 2010-12-25</li><li>'previous_analysis' to " +
      "compare to previous analysis</li><li>'previous_version' to compare to the previous version in the project history</li></ul>" +
      "<p>When specifying a number of days or a date, the snapshot selected for comparison is " +
      " the first one available inside the corresponding time range. </p>" +
      "<p>Changing this property only takes effect after subsequent project inspections.<p/>",
    project = false,
    global = true,
    defaultValue = CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_1,
    category = CoreProperties.CATEGORY_DIFFERENTIAL_VIEWS),
  @Property(
    key = "sonar.timemachine.period2",
    name = "Period 2",
    description = "See the property 'Period 1'",
    project = false,
    global = true,
    defaultValue = CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_2,
    category = CoreProperties.CATEGORY_DIFFERENTIAL_VIEWS),
  @Property(
    key = "sonar.timemachine.period3",
    name = "Period 3",
    description = "See the property 'Period 1'",
    project = false,
    global = true,
    defaultValue = CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_3,
    category = CoreProperties.CATEGORY_DIFFERENTIAL_VIEWS),
  @Property(
    key = "sonar.timemachine.period4",
    name = "Period 4",
    description = "Period used to compare measures and track new violations. This property is specific to the project. Values are : " +
      "<ul class='bullet'><li>Number of days before analysis, for example 5.</li><li>A custom date. Format is yyyy-MM-dd, " +
      "for example 2010-12-25</li><li>'previous_analysis' to compare to previous analysis</li>" +
      "<li>'previous_version' to compare to the previous version in the project history</li><li>A version, for example 1.2</li></ul>" +
      "<p>When specifying a number of days or a date, the snapshot selected for comparison is the first one available inside the corresponding time range. </p>" +
      "<p>Changing this property only takes effect after subsequent project inspections.<p/>",
    project = true,
    global = false,
    defaultValue = CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_4,
    category = CoreProperties.CATEGORY_DIFFERENTIAL_VIEWS),
  @Property(
    key = "sonar.timemachine.period5",
    name = "Period 5",
    description = "See the property 'Period 4'",
    project = true,
    global = false,
    defaultValue = CoreProperties.TIMEMACHINE_DEFAULT_PERIOD_5,
    category = CoreProperties.CATEGORY_DIFFERENTIAL_VIEWS),
  @Property(
    key = CoreProperties.DRY_RUN,
    defaultValue = "false",
    name = "Dry Run",
    type = PropertyType.BOOLEAN,
    global = false, project = false,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.DRY_RUN_INCLUDE_PLUGINS,
    name = "Plugins accepted for dry run",
    defaultValue = CoreProperties.DRY_RUN_INCLUDE_PLUGINS_DEFAULT_VALUE,
    global = true, project = false,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = CoreProperties.DRY_RUN_EXCLUDE_PLUGINS,
    name = "Plugins excluded for dry run",
    global = true, project = false,
    defaultValue = CoreProperties.DRY_RUN_EXCLUDE_PLUGINS_DEFAULT_VALUE,
    category = CoreProperties.CATEGORY_GENERAL),
  @Property(
    key = "sonar.dryRun.export.path",
    defaultValue = "dryRun.json",
    name = "Dry Run Results Export File",
    type = PropertyType.STRING,
    global = false, project = false),
  @Property(
    key = "sonar.report.export.path",
    defaultValue = "sonar-report.json",
    name = "Report Results Export File",
    type = PropertyType.STRING,
    global = false, project = false),

  // SERVER-SIDE TECHNICAL PROPERTIES

  @Property(
    key = CoreProperties.CORE_AUTHENTICATOR_REALM,
    name = "Security Realm",
    project = false,
    global = false
  ),
  @Property(
    key = "sonar.security.savePassword",
    name = "Save external password",
    project = false,
    global = false
  ),
  @Property(
    key = "sonar.authenticator.downcase",
    name = "Downcase login",
    description = "Downcase login during user authentication, typically for Active Directory",
    project = false,
    global = false,
    defaultValue = "false",
    type = PropertyType.BOOLEAN),
  @Property(
    key = CoreProperties.CORE_AUTHENTICATOR_CREATE_USERS,
    name = "Create user accounts",
    description = "Create accounts when authenticating users via an external system",
    project = false,
    global = false,
    defaultValue = "true",
    type = PropertyType.BOOLEAN),
  @Property(
    key = CoreProperties.CORE_AUTHENTICATOR_UPDATE_USER_ATTRIBUTES,
    name = "Update user attributes",
    description = "When using the LDAP or OpenID plugin, at each login, the user attributes (name, email, ...) are re-synchronized",
    project = false,
    global = false,
    defaultValue = "true",
    type = PropertyType.BOOLEAN),
  @Property(
    key = CoreProperties.CORE_AUTHENTICATOR_IGNORE_STARTUP_FAILURE,
    name = "Ignore failures during authenticator startup",
    defaultValue = "false",
    project = false,
    global = false,
    type = PropertyType.BOOLEAN),
  @Property(
    key = "sonar.enableFileVariation",
    name = "Enable file variation",
    global = false,
    defaultValue = "false",
    category = CoreProperties.CATEGORY_GENERAL)
})
public final class CorePlugin extends SonarPlugin {

  @SuppressWarnings("unchecked")
  public List getExtensions() {
    return ImmutableList.of(
      DefaultResourceTypes.class,
      UserManagedMetrics.class,
      Periods.class,

      // pages
      Lcom4Viewer.class,
      TestsViewer.class,

      // measure filters
      ProjectFilter.class,
      MyFavouritesFilter.class,

      // widgets
      AlertsWidget.class,
      CoverageWidget.class,
      ItCoverageWidget.class,
      CommentsDuplicationsWidget.class,
      DescriptionWidget.class,
      ComplexityWidget.class,
      RulesWidget.class,
      RulesWidget2.class,
      SizeWidget.class,
      EventsWidget.class,
      CustomMeasuresWidget.class,
      TimelineWidget.class,
      TimeMachineWidget.class,
      HotspotMetricWidget.class,
      ReviewsMetricsWidget.class,
      TreemapWidget.class,
      MeasureFilterListWidget.class,
      MeasureFilterTreemapWidget.class,
      WelcomeWidget.class,

      // dashboards
      ProjectDefaultDashboard.class,
      ProjectHotspotDashboard.class,
      ProjectReviewsDashboard.class,
      ProjectIssuesDashboard.class,
      ProjectTimeMachineDashboard.class,
      GlobalDefaultDashboard.class,

      // chart
      XradarChart.class,
      DistributionBarChart.class,
      DistributionAreaChart.class,

      // colorizers
      JavaColorizerFormat.class,

      // issues
      IssueHandlers.class,
      IssueFilters.class,
      CountOpenIssuesDecorator.class,
      CountFalsePositivesDecorator.class,
      WeightedIssuesDecorator.class,
      IssuesDensityDecorator.class,
      InitialOpenIssuesSensor.class,
      InitialOpenIssuesStack.class,
      HotspotMostViolatedResourcesWidget.class,
      HotspotMostViolatedRulesWidget.class,
      MyReviewsWidget.class,
      MyUnresolvedIssuesWidget.class,
      ProjectReviewsWidget.class,
      FalsePositiveReviewsWidget.class,
      FalsePositiveIssuesWidget.class,
      ReviewsPerDeveloperWidget.class,
      PlannedReviewsWidget.class,
      UnplannedReviewsWidget.class,
      ActionPlansWidget.class,
      org.sonar.plugins.core.widgets.ActionPlansWidget.class,
      UnresolvedIssuesPerAssigneeWidget.class,

      // batch
      ProfileSensor.class,
      ProfileEventsSensor.class,
      ProjectLinksSensor.class,
      UnitTestDecorator.class,
      VersionEventsSensor.class,
      CheckAlertThresholds.class,
      GenerateAlertEvents.class,
      ViolationsDecorator.class,
      IssueTrackingDecorator.class,
      WeightedViolationsDecorator.class,
      ViolationsDensityDecorator.class,
      LineCoverageDecorator.class,
      CoverageDecorator.class,
      BranchCoverageDecorator.class,
      ItLineCoverageDecorator.class,
      ItCoverageDecorator.class,
      ItBranchCoverageDecorator.class,
      OverallLineCoverageDecorator.class,
      OverallCoverageDecorator.class,
      OverallBranchCoverageDecorator.class,
      ApplyProjectRolesDecorator.class,
      CommentDensityDecorator.class,
      NoSonarFilter.class,
      DirectoriesDecorator.class,
      FilesDecorator.class,
      ReviewNotifications.class,
      ReviewWorkflowDecorator.class,
      ManualMeasureDecorator.class,
      ManualViolationInjector.class,
      ViolationSeverityUpdater.class,
      IndexProjectPostJob.class,
      ReviewsMeasuresDecorator.class,

      // time machine
      TendencyDecorator.class,
      VariationDecorator.class,
      ViolationTrackingDecorator.class,
      IssueTracking.class,
      ViolationPersisterDecorator.class,
      NewViolationsDecorator.class,
      TimeMachineConfigurationPersister.class,
      NewCoverageFileAnalyzer.class,
      NewItCoverageFileAnalyzer.class,
      NewOverallCoverageFileAnalyzer.class,
      NewCoverageAggregator.class,

      // notifications
      // Notify incoming violations on my favourite projects
      NewViolationsOnFirstDifferentialPeriod.class,
      NotificationDispatcherMetadata.create("NewViolationsOnFirstDifferentialPeriod")
        .setProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION, String.valueOf(true))
        .setProperty(NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION, String.valueOf(true)),
      // Notify alerts on my favourite projects
      NewAlerts.class,
      NotificationDispatcherMetadata.create("NewAlerts")
        .setProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION, String.valueOf(true))
        .setProperty(NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION, String.valueOf(true)),
      // Notify reviews changes
      ChangesInReviewAssignedToMeOrCreatedByMe.class,
      NotificationDispatcherMetadata.create("ChangesInReviewAssignedToMeOrCreatedByMe")
        .setProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION, String.valueOf(true))
        .setProperty(NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION, String.valueOf(true)),
      // Notify new false positive resolution
      NewFalsePositiveReview.class,
      NotificationDispatcherMetadata.create("NewFalsePositiveReview")
        .setProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION, String.valueOf(true))
        .setProperty(NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION, String.valueOf(true))
    );
  }
}
