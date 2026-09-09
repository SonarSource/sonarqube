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

import org.junit.Test;
import org.sonar.api.platform.Server;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class IssueSyncOrphanedBranchesStartupHandlerTest {

  private final AsyncIssueIndexing asyncIssueIndexing = mock(AsyncIssueIndexing.class);
  private final IssueSyncOrphanedBranchesStartupHandler underTest = new IssueSyncOrphanedBranchesStartupHandler(asyncIssueIndexing);

  @Test
  public void onServerStart_triggers_orphaned_branch_requeue() {
    underTest.onServerStart(mock(Server.class));

    verify(asyncIssueIndexing).triggerForOrphanedBranches();
  }
}
