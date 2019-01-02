/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.source.linereader;

import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfo;
import org.sonar.db.protobuf.DbFileSources;

public class ScmLineReader implements LineReader {

  private final ScmInfo scmReport;
  @CheckForNull
  private Changeset latestChange;
  @CheckForNull
  private Changeset latestChangeWithRevision;

  public ScmLineReader(ScmInfo scmReport) {
    this.scmReport = scmReport;
  }

  @Override
  public Optional<ReadError> read(DbFileSources.Line.Builder lineBuilder) {
    if (scmReport.hasChangesetForLine(lineBuilder.getLine())) {
      Changeset changeset = scmReport.getChangesetForLine(lineBuilder.getLine());
      String author = changeset.getAuthor();
      if (author != null) {
        lineBuilder.setScmAuthor(author);
      }
      String revision = changeset.getRevision();
      if (revision != null) {
        lineBuilder.setScmRevision(revision);
      }
      lineBuilder.setScmDate(changeset.getDate());
      updateLatestChange(changeset);

      if (revision != null) {
        updateLatestChangeWithRevision(changeset);
      }
    }
    return Optional.empty();
  }

  private void updateLatestChange(Changeset newChangeSet) {
    if (latestChange == null) {
      latestChange = newChangeSet;
    } else {
      long newChangesetDate = newChangeSet.getDate();
      long latestChangeDate = latestChange.getDate();
      if (newChangesetDate > latestChangeDate) {
        latestChange = newChangeSet;
      }
    }
  }

  private void updateLatestChangeWithRevision(Changeset newChangeSet) {
    if (latestChangeWithRevision == null) {
      latestChangeWithRevision = newChangeSet;
    } else {
      long newChangesetDate = newChangeSet.getDate();
      long latestChangeDate = latestChangeWithRevision.getDate();
      if (newChangesetDate > latestChangeDate) {
        latestChangeWithRevision = newChangeSet;
      }
    }
  }

  @CheckForNull
  public Changeset getLatestChangeWithRevision() {
    return latestChangeWithRevision;
  }

  @CheckForNull
  public Changeset getLatestChange() {
    return latestChange;
  }
}
