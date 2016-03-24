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
package org.sonar.xoo;

import org.sonar.api.Plugin;
import org.sonar.xoo.coverage.ItCoverageSensor;
import org.sonar.xoo.coverage.OverallCoverageSensor;
import org.sonar.xoo.coverage.UtCoverageSensor;
import org.sonar.xoo.extensions.XooPostJob;
import org.sonar.xoo.extensions.XooProjectBuilder;
import org.sonar.xoo.lang.CpdTokenizerSensor;
import org.sonar.xoo.lang.MeasureSensor;
import org.sonar.xoo.lang.SymbolReferencesSensor;
import org.sonar.xoo.lang.SyntaxHighlightingSensor;
import org.sonar.xoo.lang.XooCpdMapping;
import org.sonar.xoo.lang.XooTokenizer;
import org.sonar.xoo.rule.ChecksSensor;
import org.sonar.xoo.rule.CreateIssueByInternalKeySensor;
import org.sonar.xoo.rule.CustomMessageSensor;
import org.sonar.xoo.rule.DeprecatedResourceApiSensor;
import org.sonar.xoo.rule.HasTagSensor;
import org.sonar.xoo.rule.MultilineIssuesSensor;
import org.sonar.xoo.rule.OneBlockerIssuePerFileSensor;
import org.sonar.xoo.rule.OneBugIssuePerLineSensor;
import org.sonar.xoo.rule.OneDayDebtPerFileSensor;
import org.sonar.xoo.rule.OneIssueOnDirPerFileSensor;
import org.sonar.xoo.rule.OneIssuePerFileSensor;
import org.sonar.xoo.rule.OneIssuePerLineSensor;
import org.sonar.xoo.rule.OneIssuePerModuleSensor;
import org.sonar.xoo.rule.OneVulnerabilityIssuePerModuleSensor;
import org.sonar.xoo.rule.RandomAccessSensor;
import org.sonar.xoo.rule.Xoo2BasicProfile;
import org.sonar.xoo.rule.XooBasicProfile;
import org.sonar.xoo.rule.XooEmptyProfile;
import org.sonar.xoo.rule.XooFakeExporter;
import org.sonar.xoo.rule.XooFakeImporter;
import org.sonar.xoo.rule.XooFakeImporterWithMessages;
import org.sonar.xoo.rule.XooRulesDefinition;
import org.sonar.xoo.scm.XooBlameCommand;
import org.sonar.xoo.scm.XooScmProvider;
import org.sonar.xoo.test.CoveragePerTestSensor;
import org.sonar.xoo.test.TestExecutionSensor;

import static org.sonar.api.SonarQubeVersion.V5_5;

/**
 * Plugin entry-point, as declared in pom.xml.
 */
public class XooPlugin implements Plugin {

  @Override
  public void define(Context context) {
    context.addExtensions(
      Xoo.class,
      Xoo2.class,
      XooRulesDefinition.class,
      XooBasicProfile.class,
      Xoo2BasicProfile.class,
      XooEmptyProfile.class,

      XooFakeExporter.class,
      XooFakeImporter.class,
      XooFakeImporterWithMessages.class,

      // SCM
      XooScmProvider.class,
      XooBlameCommand.class,

      // CPD
      XooCpdMapping.class,
      XooTokenizer.class,

      // sensors
      HasTagSensor.class,
      MeasureSensor.class,
      SyntaxHighlightingSensor.class,
      SymbolReferencesSensor.class,
      ChecksSensor.class,
      RandomAccessSensor.class,
      DeprecatedResourceApiSensor.class,

      OneBlockerIssuePerFileSensor.class,
      OneIssuePerLineSensor.class,
      OneDayDebtPerFileSensor.class,
      OneIssuePerFileSensor.class,
      OneIssuePerModuleSensor.class,
      OneIssueOnDirPerFileSensor.class,
      CreateIssueByInternalKeySensor.class,
      MultilineIssuesSensor.class,
      CustomMessageSensor.class,

      OneBugIssuePerLineSensor.class,
      OneVulnerabilityIssuePerModuleSensor.class,

      // Coverage
      UtCoverageSensor.class,
      ItCoverageSensor.class,
      OverallCoverageSensor.class,

      // Tests
      TestExecutionSensor.class,
      CoveragePerTestSensor.class,

      // Other
      XooProjectBuilder.class,
      XooPostJob.class);

    if (context.getSonarQubeVersion().isGreaterThanOrEqual(V5_5)) {
      context.addExtension(CpdTokenizerSensor.class);
    }
  }

}
