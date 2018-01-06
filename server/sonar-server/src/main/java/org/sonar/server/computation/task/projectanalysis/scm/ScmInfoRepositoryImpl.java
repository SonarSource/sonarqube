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
package org.sonar.server.computation.task.projectanalysis.scm;

import com.google.common.base.Optional;
import java.util.HashMap;
import java.util.Map;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReader;
import org.sonar.server.computation.task.projectanalysis.component.Component;

import static java.util.Objects.requireNonNull;

public class ScmInfoRepositoryImpl implements ScmInfoRepository {

  private static final Logger LOGGER = Loggers.get(ScmInfoRepositoryImpl.class);

  private final BatchReportReader batchReportReader;
  private final Map<Component, ScmInfo> scmInfoCache = new HashMap<>();
  private final ScmInfoDbLoader scmInfoDbLoader;

  public ScmInfoRepositoryImpl(BatchReportReader batchReportReader, ScmInfoDbLoader scmInfoDbLoader) {
    this.batchReportReader = batchReportReader;
    this.scmInfoDbLoader = scmInfoDbLoader;
  }

  @Override
  public Optional<ScmInfo> getScmInfo(Component component) {
    requireNonNull(component, "Component cannot be null");
    return initializeScmInfoForComponent(component);
  }

  private Optional<ScmInfo> initializeScmInfoForComponent(Component component) {
    if (component.getType() != Component.Type.FILE) {
      return Optional.absent();
    }
    ScmInfo scmInfo = scmInfoCache.get(component);
    if (scmInfo != null) {
      return optionalOf(scmInfo);
    }

    scmInfo = getScmInfoForComponent(component);
    scmInfoCache.put(component, scmInfo);
    return optionalOf(scmInfo);
  }

  private static Optional<ScmInfo> optionalOf(ScmInfo scmInfo) {
    if (scmInfo == NoScmInfo.INSTANCE) {
      return Optional.absent();
    }
    return Optional.of(scmInfo);
  }

  private ScmInfo getScmInfoForComponent(Component component) {
    ScannerReport.Changesets changesets = batchReportReader.readChangesets(component.getReportAttributes().getRef());
    if (changesets == null) {
      LOGGER.trace("No SCM info for file '{}'", component.getKey());
      return NoScmInfo.INSTANCE;
    }
    if (changesets.getCopyFromPrevious()) {
      return scmInfoDbLoader.getScmInfoFromDb(component);
    }
    return getScmInfoFromReport(component, changesets);
  }

  private static ScmInfo getScmInfoFromReport(Component file, ScannerReport.Changesets changesets) {
    LOGGER.trace("Reading SCM info from report for file '{}'", file.getKey());
    return new ReportScmInfo(changesets);
  }

  /**
   * Internally used to populate cache when no ScmInfo exist.
   */
  enum NoScmInfo implements ScmInfo {
    INSTANCE {
      @Override
      public Changeset getLatestChangeset() {
        return notImplemented();
      }

      @Override
      public Changeset getChangesetForLine(int lineNumber) {
        return notImplemented();
      }

      @Override
      public boolean hasChangesetForLine(int lineNumber) {
        return notImplemented();
      }

      @Override
      public Iterable<Changeset> getAllChangesets() {
        return notImplemented();
      }

      private <T> T notImplemented() {
        throw new UnsupportedOperationException("NoScmInfo does not implement any method");
      }
    }
  }
}
