/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.analysis;

import java.util.Map;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;

@Immutable
public class DefaultAnalysisMode implements AnalysisMode {
  private static final Logger LOG = Loggers.get(DefaultAnalysisMode.class);
  private static final String KEY_SCAN_ALL = "sonar.scanAllFiles";

  private final Map<String, String> analysisProps;
  private final GlobalAnalysisMode analysisMode;

  private boolean scanAllFiles;

  public DefaultAnalysisMode(AnalysisProperties props, GlobalAnalysisMode analysisMode) {
    this.analysisMode = analysisMode;
    this.analysisProps = props.properties();
    load();
    printFlags();
  }

  public boolean scanAllFiles() {
    return scanAllFiles;
  }

  private void printFlags() {
    if (!scanAllFiles) {
      LOG.info("Scanning only changed files");
    }
  }

  private void load() {
    String scanAllStr = analysisProps.get(KEY_SCAN_ALL);
    scanAllFiles = !analysisMode.isIssues() || "true".equals(scanAllStr);
  }

  @Override
  public boolean isPreview() {
    return analysisMode.isPreview();
  }

  @Override
  public boolean isIssues() {
    return analysisMode.isIssues();
  }

  @Override
  public boolean isPublish() {
    return analysisMode.isPublish();
  }
}
