/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.plugins.core;

import org.sonar.api.*;
import org.sonar.api.checks.NoSonarFilter;
import org.sonar.api.resources.Java;
import org.sonar.plugins.core.batch.ExcludedResourceFilter;
import org.sonar.plugins.core.charts.DistributionAreaChart;
import org.sonar.plugins.core.charts.DistributionBarChart;
import org.sonar.plugins.core.charts.XradarChart;
import org.sonar.plugins.core.clouds.Clouds;
import org.sonar.plugins.core.colorizers.JavaColorizerFormat;
import org.sonar.plugins.core.coverageviewer.CoverageViewerDefinition;
import org.sonar.plugins.core.defaultsourceviewer.DefaultSourceViewer;
import org.sonar.plugins.core.duplicationsviewer.DuplicationsViewerDefinition;
import org.sonar.plugins.core.hotspots.Hotspots;
import org.sonar.plugins.core.metrics.UserManagedMetrics;
import org.sonar.plugins.core.purges.*;
import org.sonar.plugins.core.security.ApplyProjectRolesDecorator;
import org.sonar.plugins.core.sensors.*;
import org.sonar.plugins.core.testdetailsviewer.TestsViewerDefinition;
import org.sonar.plugins.core.ui.pageselector.GwtPageSelector;
import org.sonar.plugins.core.violationsviewer.ViolationsViewerDefinition;

import java.util.ArrayList;
import java.util.List;

@Properties({
    @Property(
        key = CoreProperties.CORE_COVERAGE_PLUGIN_PROPERTY,
        defaultValue = "cobertura",
        name = "Code coverage plugin",
        description = "Key of the code coverage plugin to use.",
        project = true,
        global = true),
    @Property(
        key = CoreProperties.CORE_IMPORT_SOURCES_PROPERTY,
        defaultValue = "" + CoreProperties.CORE_IMPORT_SOURCES_DEFAULT_VALUE,
        name = "Import sources",
        description = "Set to false if sources should not be displayed, e.g. for security reasons.",
        project = true,
        module = true,
        global = true),
    @Property(
        key = CoreProperties.CORE_TENDENCY_DEPTH_PROPERTY,
        defaultValue = "" + CoreProperties.CORE_TENDENCY_DEPTH_DEFAULT_VALUE,
        name = "Tendency period",
        description = TendencyDecorator.PROP_DAYS_DESCRIPTION,
        project = false,
        global = true),
    @Property(
        key = CoreProperties.CORE_SKIPPED_MODULES_PROPERTY,
        name = "Exclude modules",
        description = "Maven artifact ids of modules to exclude (comma-separated).",
        project = true,
        global = false),
    @Property(
        key = CoreProperties.CORE_RULE_WEIGHTS_PROPERTY,
        defaultValue = CoreProperties.CORE_RULE_WEIGHTS_DEFAULT_VALUE,
        name = "Rules weight",
        description = "A weight is associated to each priority to calculate the Rules Compliance Index.",
        project = false,
        global = true),
    @Property(
        key = CoreProperties.CORE_FORCE_AUTHENTICATION_PROPERTY,
        defaultValue = "" + CoreProperties.CORE_FORCE_AUTHENTICATION_DEFAULT_VALUE,
        name = "Force user authentication",
        description = "Forcing user authentication stops un-logged users to access Sonar.",
        project = false,
        global = true),
    @Property(
        key = CoreProperties.CORE_ALLOW_USERS_TO_SIGNUP_PROPERTY,
        defaultValue = "" + CoreProperties.CORE_ALLOW_USERS_TO_SIGNUP_DEAULT_VALUE,
        name = "Allow users to sign up online",
        description = "Users can sign up online.",
        project = false,
        global = true),
    @Property(
        key = CoreProperties.CORE_DEFAULT_GROUP,
        defaultValue = CoreProperties.CORE_DEFAULT_GROUP_DEFAULT_VALUE,
        name = "Default user group",
        description = "Any new users will automatically join this group.",
        project = false,
        global = true
    )
})
public class CorePlugin implements Plugin {

  public String getKey() {
    return CoreProperties.CORE_PLUGIN;
  }

  public String getName() {
    return "General";
  }

  public String getDescription() {
    return "";
  }

  public List getExtensions() {
    List extensions = new ArrayList();

    // languages
    extensions.add(Java.class);

    // metrics
    extensions.add(UserManagedMetrics.class);

    // pages
    extensions.add(GwtPageSelector.class);
    extensions.add(DefaultSourceViewer.class);
    extensions.add(CoverageViewerDefinition.class);
    extensions.add(ViolationsViewerDefinition.class);
    extensions.add(DuplicationsViewerDefinition.class);
    extensions.add(TestsViewerDefinition.class);
    extensions.add(Clouds.class);
    extensions.add(Hotspots.class);

    // chart
    extensions.add(XradarChart.class);
    extensions.add(DistributionBarChart.class);
    extensions.add(DistributionAreaChart.class);

    // colorizers
    extensions.add(JavaColorizerFormat.class);

    // batch
    extensions.add(JavaSourceImporter.class);
    extensions.add(ProfileSensor.class);
    extensions.add(ProjectLinksSensor.class);
    extensions.add(AsynchronousMeasuresSensor.class);
    extensions.add(UnitTestDecorator.class);
    extensions.add(VersionEventsSensor.class);
    extensions.add(CheckAlertThresholds.class);
    extensions.add(GenerateAlertEvents.class);
    extensions.add(ViolationsDecorator.class);
    extensions.add(WeightedViolationsDecorator.class);
    extensions.add(ViolationsDensityDecorator.class);
    extensions.add(TendencyDecorator.class);
    extensions.add(LineCoverageDecorator.class);
    extensions.add(CoverageDecorator.class);
    extensions.add(BranchCoverageDecorator.class);
    extensions.add(UncoveredComplexityDecorator.class);
    extensions.add(ApplyProjectRolesDecorator.class);
    extensions.add(ExcludedResourceFilter.class);
    extensions.add(CommentDensityDecorator.class);
    extensions.add(NoSonarFilter.class);
    extensions.add(DirectoriesDecorator.class);
    extensions.add(FilesDecorator.class);


    // purges
    extensions.add(PurgeOrphanResources.class);
    extensions.add(PurgeEntities.class);
    extensions.add(PurgeRuleMeasures.class);
    extensions.add(PurgeUnprocessed.class);
    extensions.add(PurgeDeletedResources.class);
    extensions.add(PurgeDeprecatedLast.class);
    extensions.add(UnflagLastDoublons.class);
    extensions.add(PurgeDisabledResources.class);
    extensions.add(PurgeResourceRoles.class);
    extensions.add(PurgeEventOrphans.class);
    extensions.add(PurgePropertyOrphans.class);

    return extensions;
  }

  @Override
  public String toString() {
    return getKey();
  }
}
