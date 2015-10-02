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

import com.google.common.base.Optional;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.server.computation.scm.Changeset;
import org.sonar.server.computation.scm.ScmInfo;

import javax.annotation.CheckForNull;

import static com.google.common.base.Preconditions.checkArgument;

public class ScmLineReader implements LineReader {

  private final ScmInfo scmReport;
  @CheckForNull
  private Changeset latestChange;

  public ScmLineReader(ScmInfo scmReport) {
    this.scmReport = scmReport;
  }

  @Override
  public void read(DbFileSources.Line.Builder lineBuilder) {
    Optional<Changeset> changesetOptional = scmReport.getForLine(lineBuilder.getLine());
    if (!changesetOptional.isPresent()) {
      return;
    }
    Changeset changeset = changesetOptional.get();
    String author = changeset.getAuthor();
    if (author != null) {
      lineBuilder.setScmAuthor(author);
    }
    String revision = changeset.getRevision();
    if (revision != null) {
      lineBuilder.setScmRevision(revision);
    }
    Long date = changeset.getDate();
    if (date != null) {
      lineBuilder.setScmDate(date);
    }

    checkArgument(
      author != null || revision != null || date != null,
      "A changeset must contain at least one of : author, revision or date");
    updateLatestChange(changeset);
  }

  private void updateLatestChange(Changeset newChangeSet) {
    if (latestChange == null) {
      latestChange = newChangeSet;
    } else {
      Long newChangesetDate = newChangeSet.getDate();
      Long latestChangeDate = latestChange.getDate();
      if (newChangesetDate != null && latestChangeDate != null && newChangesetDate > latestChangeDate) {
        latestChange = newChangeSet;
      }
    }
  }

  @CheckForNull
  public Changeset getLatestChange() {
    return latestChange;
  }
}
