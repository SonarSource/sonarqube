/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.index;

import java.util.Collection;
import org.sonar.ce.task.projectanalysis.issue.ChangedIssuesRepository;
import org.sonar.server.es.AnalysisIndexer;
import org.sonar.server.issue.index.IssueIndexer;

public class IndexDiffResolverImpl implements IndexDiffResolver {
  private final ChangedIssuesRepository changedIssuesRepository;

  public IndexDiffResolverImpl(ChangedIssuesRepository changedIssuesRepository) {
    this.changedIssuesRepository = changedIssuesRepository;
  }

  @Override
  public Collection<String> resolve(Class<? extends AnalysisIndexer> clazz) {
    if (clazz.isAssignableFrom(IssueIndexer.class)) {
      return changedIssuesRepository.getChangedIssuesKeys();
    }
    throw new UnsupportedOperationException("Unsupported indexer: " + clazz);
  }
}
