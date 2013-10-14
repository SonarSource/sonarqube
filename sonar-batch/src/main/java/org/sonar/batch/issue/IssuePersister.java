/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.issue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.index.ScanPersister;

/**
 * Executed at the end of project scan, when all the modules are completed.
 */
public class IssuePersister implements ScanPersister {

  private static final Logger LOG = LoggerFactory.getLogger(IssuePersister.class);

  private final IssueCache issueCache;
  private final ScanIssueStorage storage;
  private AnalysisMode analysisMode;

  public IssuePersister(IssueCache issueCache, ScanIssueStorage storage, AnalysisMode analysisMode) {
    this.issueCache = issueCache;
    this.storage = storage;
    this.analysisMode = analysisMode;
  }

  @Override
  public void persist() {
    if (analysisMode.isPreview()) {
      LOG.debug("IssuePersister skipped in preview mode");
      return;
    }
    Iterable<DefaultIssue> issues = issueCache.all();
    storage.save(issues);
  }
}
