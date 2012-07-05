/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.java.squid.check;

import org.sonar.api.checks.CheckFactory;
import org.sonar.api.rules.ActiveRule;
import org.sonar.check.Priority;
import org.sonar.check.Rule;

import javax.annotation.CheckForNull;

/**
 * Companion of {@link org.sonar.plugins.squid.bridges.DesignBridge} which actually does the job on finding cycles and creation of violations.
 */
@Rule(key = "CycleBetweenPackages", priority = Priority.MAJOR)
public class CycleBetweenPackagesCheck extends SquidCheck {

  /**
   * @return null, if this check is inactive
   */
  @CheckForNull
  public static ActiveRule getActiveRule(CheckFactory checkFactory) {
    for (Object check : checkFactory.getChecks()) {
      if (check.getClass().getName() == CycleBetweenPackagesCheck.class.getName()) {
        return checkFactory.getActiveRule(check);
      }
    }
    return null;
  }

}
