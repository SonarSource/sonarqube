/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.dbcleaner;

import java.util.List;
import org.sonar.api.server.ServerSide;
import org.sonar.db.purge.PurgeListener;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.test.index.TestIndexer;

@ServerSide
public class IndexPurgeListener implements PurgeListener {
  private final TestIndexer testIndexer;
  private final IssueIndexer issueIndexer;

  public IndexPurgeListener(TestIndexer testIndexer, IssueIndexer issueIndexer) {
    this.testIndexer = testIndexer;
    this.issueIndexer = issueIndexer;
  }

  @Override
  public void onComponentDisabling(String uuid) {
    testIndexer.deleteByFile(uuid);
  }

  @Override
  public void onIssuesRemoval(String projectUuid, List<String> issueKeys) {
    issueIndexer.deleteByKeys(projectUuid, issueKeys);
  }

}
