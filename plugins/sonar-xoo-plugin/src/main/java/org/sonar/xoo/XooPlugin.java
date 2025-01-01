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
package org.sonar.xoo;

import org.sonar.api.Plugin;
import org.sonar.api.PropertyType;
import org.sonar.api.SonarProduct;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinition.ConfigScope;
import org.sonar.xoo.coverage.ItCoverageSensor;
import org.sonar.xoo.coverage.OverallCoverageSensor;
import org.sonar.xoo.coverage.UtCoverageSensor;
import org.sonar.xoo.extensions.XooExcludeFileFilter;
import org.sonar.xoo.extensions.XooIssueFilter;
import org.sonar.xoo.extensions.XooPostJob;
import org.sonar.xoo.extensions.XooProjectBuilder;
import org.sonar.xoo.global.DeprecatedGlobalSensor;
import org.sonar.xoo.global.GlobalProjectSensor;
import org.sonar.xoo.lang.CpdTokenizerSensor;
import org.sonar.xoo.lang.LineMeasureSensor;
import org.sonar.xoo.lang.MeasureSensor;
import org.sonar.xoo.lang.SignificantCodeSensor;
import org.sonar.xoo.lang.SymbolReferencesSensor;
import org.sonar.xoo.lang.SyntaxHighlightingSensor;
import org.sonar.xoo.rule.AnalysisErrorSensor;
import org.sonar.xoo.rule.ChecksSensor;
import org.sonar.xoo.rule.CreateIssueByInternalKeySensor;
import org.sonar.xoo.rule.CustomMessageSensor;
import org.sonar.xoo.rule.HasTagSensor;
import org.sonar.xoo.rule.MarkAsUnchangedSensor;
import org.sonar.xoo.rule.MultilineHotspotSensor;
import org.sonar.xoo.rule.MultilineIssuesSensor;
import org.sonar.xoo.rule.NoSonarSensor;
import org.sonar.xoo.rule.OneBlockerIssuePerFileSensor;
import org.sonar.xoo.rule.OneBugIssuePerLineSensor;
import org.sonar.xoo.rule.OneBugIssuePerTestLineSensor;
import org.sonar.xoo.rule.OneCodeSmellIssuePerLineSensor;
import org.sonar.xoo.rule.OneCodeSmellIssuePerTestLineSensor;
import org.sonar.xoo.rule.OneDayDebtPerFileSensor;
import org.sonar.xoo.rule.OneExternalIssueOnProjectSensor;
import org.sonar.xoo.rule.OneExternalIssuePerFoobarSensor;
import org.sonar.xoo.rule.OneExternalIssuePerLineSensor;
import org.sonar.xoo.rule.OneExternalIssuePerLineWithoutMessageSensor;
import org.sonar.xoo.rule.OneIssueOnDirPerFileSensor;
import org.sonar.xoo.rule.OneIssuePerDirectorySensor;
import org.sonar.xoo.rule.OneIssuePerFileSensor;
import org.sonar.xoo.rule.OneIssuePerLineSensor;
import org.sonar.xoo.rule.OneIssuePerProjectSensor;
import org.sonar.xoo.rule.OneIssuePerTestFileSensor;
import org.sonar.xoo.rule.OneIssuePerUnknownFileSensor;
import org.sonar.xoo.rule.OnePredefinedAndAdHocRuleExternalIssuePerLineSensor;
import org.sonar.xoo.rule.OnePredefinedRuleExternalIssuePerLineSensor;
import org.sonar.xoo.rule.OneQuickFixPerLineSensor;
import org.sonar.xoo.rule.OneVulnerabilityIssuePerProjectSensor;
import org.sonar.xoo.rule.OneVulnerabilityPerSecurityStandardSensor;
import org.sonar.xoo.rule.RandomAccessSensor;
import org.sonar.xoo.rule.SaveDataTwiceSensor;
import org.sonar.xoo.rule.Xoo2BasicProfile;
import org.sonar.xoo.rule.Xoo2SonarWayProfile;
import org.sonar.xoo.rule.XooBasicProfile;
import org.sonar.xoo.rule.XooBuiltInQualityProfilesDefinition;
import org.sonar.xoo.rule.XooEmptyProfile;
import org.sonar.xoo.rule.XooFakeExporter;
import org.sonar.xoo.rule.XooFakeImporter;
import org.sonar.xoo.rule.XooFakeImporterWithMessages;
import org.sonar.xoo.rule.XooRulesDefinition;
import org.sonar.xoo.rule.XooSonarWayProfile;
import org.sonar.xoo.rule.hotspot.HotspotWithContextsSensor;
import org.sonar.xoo.rule.hotspot.HotspotWithSingleContextSensor;
import org.sonar.xoo.rule.hotspot.HotspotWithoutContextSensor;
import org.sonar.xoo.rule.telemetry.OneIssuePerUninitializedVariableForTelemetrySensor;
import org.sonar.xoo.rule.telemetry.SensorMetrics;
import org.sonar.xoo.rule.variant.HotspotWithCodeVariantsSensor;
import org.sonar.xoo.rule.variant.IssueWithCodeVariantsSensor;
import org.sonar.xoo.scm.XooBlameCommand;
import org.sonar.xoo.scm.XooIgnoreCommand;
import org.sonar.xoo.scm.XooScmProvider;

/**
 * Plugin entry-point, as declared in pom.xml.
 */
public class XooPlugin implements Plugin {

  @Override
  public void define(Context context) {
    context.addExtensions(
      PropertyDefinition.builder(Xoo.FILE_SUFFIXES_KEY)
        .defaultValue(Xoo.DEFAULT_FILE_SUFFIXES)
        .name("File suffixes")
        .description("Comma-separated list of suffixes for files to analyze. To not filter, leave the list empty.")
        .subCategory("General")
        .onConfigScopes(ConfigScope.PROJECT)
        .multiValues(true)
        .build(),
      PropertyDefinition.builder(Xoo2.FILE_SUFFIXES_KEY)
        .defaultValue(Xoo2.DEFAULT_FILE_SUFFIXES)
        .name("File suffixes")
        .description("Comma-separated list of suffixes for files to analyze. To not filter, leave the list empty.")
        .subCategory("General")
        .onConfigScopes(ConfigScope.PROJECT)
        .build(),
      // Used by DuplicationsTest and IssueFilterOnCommonRulesTest. If not declared it is not returned by api/settings
      PropertyDefinition.builder("sonar.cpd.xoo.minimumTokens")
        .onConfigScopes(ConfigScope.PROJECT)
        .type(PropertyType.INTEGER)
        .build(),
      PropertyDefinition.builder("sonar.cpd.xoo.minimumLines")
        .onConfigScopes(ConfigScope.PROJECT)
        .type(PropertyType.INTEGER)
        .build(),
      Xoo.class,
      Xoo2.class,
      Xoo3NoAutoPublish.class,
      Xoo3QualityProfileDefinition.class,
      XooRulesDefinition.class,
      XooBuiltInQualityProfilesDefinition.class,
      XooSonarWayProfile.class,
      XooBasicProfile.class,
      Xoo2SonarWayProfile.class,
      Xoo2BasicProfile.class,
      XooEmptyProfile.class,

      XooFakeExporter.class,
      XooFakeImporter.class,
      XooFakeImporterWithMessages.class,

      // SCM
      XooScmProvider.class,
      XooBlameCommand.class,

      // sensors
      HasTagSensor.class,
      LineMeasureSensor.class,
      SyntaxHighlightingSensor.class,
      SymbolReferencesSensor.class,
      ChecksSensor.class,
      RandomAccessSensor.class,
      SaveDataTwiceSensor.class,
      NoSonarSensor.class,
      CpdTokenizerSensor.class,

      OneBlockerIssuePerFileSensor.class,
      OneIssuePerLineSensor.class,
      OneDayDebtPerFileSensor.class,
      OneIssuePerFileSensor.class,
      OneIssuePerTestFileSensor.class,
      OneBugIssuePerTestLineSensor.class,
      OneCodeSmellIssuePerTestLineSensor.class,
      OneIssuePerDirectorySensor.class,
      OneIssuePerProjectSensor.class,
      OneIssueOnDirPerFileSensor.class,
      OneIssuePerUnknownFileSensor.class,
      OneQuickFixPerLineSensor.class,

      OneExternalIssuePerFoobarSensor.class,
      OneExternalIssuePerLineSensor.class,
      OneExternalIssuePerLineWithoutMessageSensor.class,
      OneExternalIssueOnProjectSensor.class,
      OnePredefinedRuleExternalIssuePerLineSensor.class,
      OnePredefinedAndAdHocRuleExternalIssuePerLineSensor.class,

      OneIssuePerUninitializedVariableForTelemetrySensor.class,

      CreateIssueByInternalKeySensor.class,
      MultilineIssuesSensor.class,
      MultilineHotspotSensor.class,
      CustomMessageSensor.class,

      OneBugIssuePerLineSensor.class,
      OneCodeSmellIssuePerLineSensor.class,
      OneVulnerabilityIssuePerProjectSensor.class,
      OneVulnerabilityPerSecurityStandardSensor.class,

      SensorMetrics.class,
      DeprecatedGlobalSensor.class,
      GlobalProjectSensor.class,

      HotspotWithoutContextSensor.class,
      HotspotWithContextsSensor.class,
      HotspotWithSingleContextSensor.class,
      HotspotWithCodeVariantsSensor.class,

      // Coverage
      UtCoverageSensor.class,
      ItCoverageSensor.class,
      OverallCoverageSensor.class,

      // Analysis errors
      AnalysisErrorSensor.class,

      // Other
      MarkAsUnchangedSensor.class,
      XooProjectBuilder.class,
      XooPostJob.class,
      XooIssueFilter.class,
      XooIgnoreCommand.class,
      SignificantCodeSensor.class,
      IssueWithCodeVariantsSensor.class,
      XooExcludeFileFilter.class);

    if (context.getRuntime().getProduct() != SonarProduct.SONARLINT) {
      context.addExtension(MeasureSensor.class);
    }
  }

}
