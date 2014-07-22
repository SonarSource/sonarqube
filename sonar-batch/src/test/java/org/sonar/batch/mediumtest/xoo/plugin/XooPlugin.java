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
package org.sonar.batch.mediumtest.xoo.plugin;

import org.sonar.api.SonarPlugin;
import org.sonar.batch.mediumtest.xoo.plugin.base.Xoo;
import org.sonar.batch.mediumtest.xoo.plugin.lang.MeasureSensor;
import org.sonar.batch.mediumtest.xoo.plugin.lang.ScmActivitySensor;
import org.sonar.batch.mediumtest.xoo.plugin.rule.CreateIssueByInternalKeySensor;
import org.sonar.batch.mediumtest.xoo.plugin.rule.OneIssueOnDirPerFileSensor;
import org.sonar.batch.mediumtest.xoo.plugin.rule.OneIssuePerLineSensor;

import java.util.Arrays;
import java.util.List;

public final class XooPlugin extends SonarPlugin {

  @Override
  public List getExtensions() {
    return Arrays.asList(
      // language
      MeasureSensor.class,
      ScmActivitySensor.class,
      Xoo.class,

      // sensors
      OneIssuePerLineSensor.class,
      OneIssueOnDirPerFileSensor.class,
      CreateIssueByInternalKeySensor.class
      );
  }
}
