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
import org.sonar.xoo.lang.CoveragePerTestSensor;
import org.sonar.xoo.lang.MeasureSensor;
import org.sonar.xoo.lang.SymbolReferencesSensor;
import org.sonar.xoo.lang.SyntaxHighlightingSensor;
import org.sonar.xoo.lang.TestCaseSensor;
import org.sonar.xoo.lang.XooScmProvider;
import org.sonar.xoo.lang.XooTokenizerSensor;
import org.sonar.xoo.rule.CreateIssueByInternalKeySensor;
import org.sonar.xoo.rule.OneIssueOnDirPerFileSensor;
import org.sonar.xoo.rule.OneIssuePerLineSensor;
import org.sonar.xoo.rule.XooQualityProfile;
import org.sonar.xoo.rule.XooRulesDefinition;

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

      // SCM
      XooScmProvider.class,

      // sensors
      MeasureSensor.class,
      SyntaxHighlightingSensor.class,
      SymbolReferencesSensor.class,
      XooTokenizerSensor.class,
      TestCaseSensor.class,
      CoveragePerTestSensor.class,

      OneIssuePerLineSensor.class,
      OneIssueOnDirPerFileSensor.class,
      CreateIssueByInternalKeySensor.class
      );
  }

}
