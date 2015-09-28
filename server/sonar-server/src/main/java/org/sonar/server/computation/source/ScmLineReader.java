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

package org.sonar.server.computation.source;

import javax.annotation.CheckForNull;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.db.protobuf.DbFileSources;

import static com.google.common.base.Preconditions.checkArgument;

public class ScmLineReader implements LineReader {

  private final BatchReport.Changesets scmReport;
  @CheckForNull
  private BatchReport.Changesets.Changeset latestChange;

  public ScmLineReader(BatchReport.Changesets scmReport) {
    this.scmReport = scmReport;
  }

  @Override
  public void read(DbFileSources.Line.Builder lineBuilder) {
    int changeSetIndex = scmReport.getChangesetIndexByLine(lineBuilder.getLine() - 1);
    BatchReport.Changesets.Changeset changeset = scmReport.getChangeset(changeSetIndex);
    boolean hasAuthor = changeset.hasAuthor();
    if (hasAuthor) {
      lineBuilder.setScmAuthor(changeset.getAuthor());
    }
    boolean hasRevision = changeset.hasRevision();
    if (hasRevision) {
      lineBuilder.setScmRevision(changeset.getRevision());
    }
    boolean hasDate = changeset.hasDate();
    if (hasDate) {
      lineBuilder.setScmDate(changeset.getDate());
    }

    checkArgument(
      hasAuthor || hasRevision || hasDate,
      "A changeset must contain at least one of : author, revision or date");
    updateLatestChange(changeset);
  }

  private void updateLatestChange(BatchReport.Changesets.Changeset newChangeSet) {
    if (this.latestChange == null) {
      this.latestChange = newChangeSet;
    } else if (newChangeSet.hasDate() && this.latestChange.hasDate()
      && newChangeSet.getDate() > this.latestChange.getDate()) {
      this.latestChange = newChangeSet;
    }
  }

  @CheckForNull
  public BatchReport.Changesets.Changeset getLatestChange() {
    return latestChange;
  }
}
