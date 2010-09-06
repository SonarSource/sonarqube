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
package org.sonar.plugins.pmd;

import org.sonar.api.CoreProperties;
import org.sonar.api.Plugin;

import java.util.Arrays;
import java.util.List;

public class PmdPlugin implements Plugin {

  public String getKey() {
    return CoreProperties.PMD_PLUGIN;
  }

  public String getName() {
    return "PMD";
  }

  public String getDescription() {
    return "PMD is a tool that looks for potential problems like possible bugs, dead code, suboptimal code,  overcomplicated expressions or duplicate code. You can find more by going to the <a href='http://pmd.sourceforge.net'>PMD web site</a>.";
  }

  public List getExtensions() {
    return Arrays.asList(PmdSensor.class, PmdConfiguration.class, PmdExecutor.class, PmdRulesRepository.class);
  }
}
