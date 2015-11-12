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

package org.sonar.server.computation.duplication;

import com.google.common.base.Preconditions;
import javax.annotation.CheckForNull;
import org.picocontainer.Startable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.computation.analysis.AnalysisMetadataHolder;

public class CrossProjectDuplicationStatusHolderImpl implements CrossProjectDuplicationStatusHolder, Startable {

  private static final Logger LOGGER = Loggers.get(CrossProjectDuplicationStatusHolderImpl.class);

  @CheckForNull
  private Boolean enabled;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public CrossProjectDuplicationStatusHolderImpl(AnalysisMetadataHolder analysisMetadataHolder) {
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  @Override
  public boolean isEnabled() {
    Preconditions.checkState(enabled != null, "Flag hasn't been initialized, the start() should have been called before.");
    return enabled;
  }

  @Override
  public void start() {
    boolean crossProjectDuplicationIsEnabledInReport = analysisMetadataHolder.isCrossProjectDuplicationEnabled();
    boolean branchIsUsed = analysisMetadataHolder.getBranch() != null;
    if (crossProjectDuplicationIsEnabledInReport && !branchIsUsed) {
      LOGGER.info("Cross project duplication is enabled");
      this.enabled = true;
    } else {
      if (!crossProjectDuplicationIsEnabledInReport) {
        LOGGER.info("Cross project duplication is disabled because it's disabled in the analysis report");
      } else {
        LOGGER.info("Cross project duplication is disabled because of a branch is used");
      }
      this.enabled = false;
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }
}
