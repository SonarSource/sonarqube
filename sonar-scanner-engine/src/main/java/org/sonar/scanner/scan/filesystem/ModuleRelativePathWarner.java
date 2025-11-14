/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.scanner.scan.filesystem;

import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.notifications.AnalysisWarnings;

public class ModuleRelativePathWarner {

  private static final Logger LOG = LoggerFactory.getLogger(ModuleRelativePathWarner.class);
  private final AnalysisWarnings analysisWarnings;
  private final Set<String> previouslyWarnedProps = new HashSet<>();

  public ModuleRelativePathWarner(AnalysisWarnings analysisWarnings) {
    this.analysisWarnings = analysisWarnings;
  }

  public void warnOnce(String propKey, String filePath) {
    if (!previouslyWarnedProps.contains(propKey)) {
      previouslyWarnedProps.add(propKey);
      String msg = "Specifying module-relative paths at project level in the property '" + propKey + "' is deprecated. " +
        "To continue matching files like '" + filePath + "', update this property so that patterns refer to project-relative paths.";
      LOG.warn(msg);
      analysisWarnings.addUnique(msg);
    }
  }
}
