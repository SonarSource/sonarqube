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
package org.sonar.ce.es;

import org.picocontainer.Startable;
import org.sonar.server.activity.index.ActivityIndexer;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.test.index.TestIndexer;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.view.index.ViewIndexer;

/**
 * Replaces the {@link org.sonar.server.search.IndexSynchronizer} to enable indexers but without triggering a full
 * indexation (it's the WebServer's responsibility).
 */
public class EsIndexerEnabler implements Startable {

  private final TestIndexer testIndexer;
  private final IssueAuthorizationIndexer issueAuthorizationIndexer;
  private final IssueIndexer issueIndexer;
  private final UserIndexer userIndexer;
  private final ViewIndexer viewIndexer;
  private final ActivityIndexer activityIndexer;

  public EsIndexerEnabler(TestIndexer testIndexer, IssueAuthorizationIndexer issueAuthorizationIndexer,
    IssueIndexer issueIndexer, UserIndexer userIndexer, ViewIndexer viewIndexer, ActivityIndexer activityIndexer) {
    this.testIndexer = testIndexer;
    this.issueAuthorizationIndexer = issueAuthorizationIndexer;
    this.issueIndexer = issueIndexer;
    this.userIndexer = userIndexer;
    this.viewIndexer = viewIndexer;
    this.activityIndexer = activityIndexer;
  }

  @Override
  public void start() {
    activityIndexer.setEnabled(true);
    issueAuthorizationIndexer.setEnabled(true);
    issueIndexer.setEnabled(true);
    testIndexer.setEnabled(true);
    userIndexer.setEnabled(true);
    viewIndexer.setEnabled(true);
  }

  @Override
  public void stop() {
    // nothing to do at stop
  }
}
