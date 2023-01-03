/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scm.svn;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;

import static org.sonar.scm.svn.SvnScmSupport.newSvnClientManager;

@ScannerSide
public class FindFork {
  private static final Logger LOG = Loggers.get(FindFork.class);

  private final SvnConfiguration configuration;

  public FindFork(SvnConfiguration configuration) {
    this.configuration = configuration;
  }

  @CheckForNull
  public Instant findDate(Path location, String referenceBranch) throws SVNException {
    ForkPoint forkPoint = find(location, referenceBranch);
    if (forkPoint != null) {
      return forkPoint.date();
    }
    return null;
  }

  @CheckForNull
  public ForkPoint find(Path location, String referenceBranch) throws SVNException {
    SVNClientManager clientManager = newSvnClientManager(configuration);
    SVNRevision revision = getSvnRevision(location, clientManager);
    LOG.debug("latest revision is " + revision);
    String svnRefBranch = "/" + referenceBranch;

    SVNLogEntryHolder handler = new SVNLogEntryHolder();
    SVNRevision endRevision = SVNRevision.create(1);
    SVNRevision startRevision = SVNRevision.create(revision.getNumber());

    do {
      clientManager.getLogClient().doLog(new File[] {location.toFile()}, startRevision, endRevision, true, true, -1, handler);
      SVNLogEntry lastEntry = handler.getLastEntry();
      Optional<SVNLogEntryPath> copyFromReference = lastEntry.getChangedPaths().values().stream()
        .filter(e -> e.getCopyPath() != null && e.getCopyPath().equals(svnRefBranch))
        .findFirst();

      if (copyFromReference.isPresent()) {
        return new ForkPoint(String.valueOf(copyFromReference.get().getCopyRevision()), Instant.ofEpochMilli(lastEntry.getDate().getTime()));
      }

      if (lastEntry.getChangedPaths().isEmpty()) {
        // shouldn't happen since it should only stop in revisions with changed paths
        return null;
      }

      SVNLogEntryPath firstChangedPath = lastEntry.getChangedPaths().values().iterator().next();
      if (firstChangedPath.getCopyPath() == null) {
        // we walked the history to the root, and the last commit found had no copy reference. Must be the trunk, there is no fork point
        return null;
      }

      // TODO Looks like a revision can have multiple changed paths. Should we iterate through all of them?
      startRevision = SVNRevision.create(firstChangedPath.getCopyRevision());
    } while (true);

  }

  private static SVNRevision getSvnRevision(Path location, SVNClientManager clientManager) throws SVNException {
    SVNStatus svnStatus = clientManager.getStatusClient().doStatus(location.toFile(), false);
    return svnStatus.getRevision();
  }

  /**
   * Handler keeping only the last entry, and count how many entries have been seen.
   */
  private static class SVNLogEntryHolder implements ISVNLogEntryHandler {
    SVNLogEntry value;

    public SVNLogEntry getLastEntry() {
      return value;
    }

    @Override
    public void handleLogEntry(SVNLogEntry svnLogEntry) {
      this.value = svnLogEntry;
    }
  }
}
