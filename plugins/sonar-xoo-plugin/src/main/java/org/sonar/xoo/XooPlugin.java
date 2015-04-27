/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.xoo;

import org.sonar.api.SonarPlugin;
import org.sonar.xoo.coverage.ItCoverageSensor;
import org.sonar.xoo.coverage.OverallCoverageSensor;
import org.sonar.xoo.coverage.UtCoverageSensor;
import org.sonar.xoo.extensions.XooPostJob;
import org.sonar.xoo.extensions.XooProjectBuilder;
import org.sonar.xoo.lang.*;
import org.sonar.xoo.rule.*;
import org.sonar.xoo.scm.XooBlameCommand;
import org.sonar.xoo.scm.XooScmProvider;
import org.sonar.xoo.test.CoveragePerTestSensor;
import org.sonar.xoo.test.TestExecutionSensor;

import java.util.Arrays;
import java.util.List;

/**
 * Plugin entry-point, as declared in pom.xml.
 */
public class XooPlugin extends SonarPlugin {

  /**
   * Declares all the extensions implemented in the plugin
   */
  @Override
  public List getExtensions() {
    return Arrays.asList(
      Xoo.class,
      XooRulesDefinition.class,
      XooQualityProfile.class,

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
      MeasureSensor.class,
      SyntaxHighlightingSensor.class,
      SymbolReferencesSensor.class,
      DependencySensor.class,
      ChecksSensor.class,
      RandomAccessSensor.class,
      DeprecatedResourceApiSensor.class,

      OneIssuePerLineSensor.class,
      OneIssueOnDirPerFileSensor.class,
      CreateIssueByInternalKeySensor.class,

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
  }
}
