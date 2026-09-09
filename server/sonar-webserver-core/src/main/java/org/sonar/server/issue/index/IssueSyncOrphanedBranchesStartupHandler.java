/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.issue.index;

import org.sonar.api.platform.Server;
import org.sonar.api.platform.ServerStartHandler;

/**
 * On every startup, re-queues branches that have need_issue_sync=true but no pending BRANCH_ISSUE_SYNC
 * task in the CE queue. This covers branches left stranded when a task was cancelled during shutdown.
 */
public class IssueSyncOrphanedBranchesStartupHandler implements ServerStartHandler {

  private final AsyncIssueIndexing asyncIssueIndexing;

  public IssueSyncOrphanedBranchesStartupHandler(AsyncIssueIndexing asyncIssueIndexing) {
    this.asyncIssueIndexing = asyncIssueIndexing;
  }

  @Override
  public void onServerStart(Server server) {
    asyncIssueIndexing.triggerForOrphanedBranches();
  }
}
