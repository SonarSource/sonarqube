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
package org.sonar.ce.task.projectanalysis.scm;

import static com.google.common.base.Preconditions.checkState;

public class GeneratedScmInfo implements ScmInfo {
  private final ScmInfoImpl delegate;

  public GeneratedScmInfo(Changeset[] lineChangeset) {
    delegate = new ScmInfoImpl(lineChangeset);
  }

  public static ScmInfo create(long analysisDate, int lines) {
    checkState(lines > 0, "No changesets");

    Changeset changeset = Changeset.newChangesetBuilder()
      .setDate(analysisDate)
      .build();
    Changeset[] lineChangeset = new Changeset[lines];
    for (int i = 0; i < lines; i++) {
      lineChangeset[i] = changeset;
    }
    return new GeneratedScmInfo(lineChangeset);
  }

  public static ScmInfo create(long analysisDate, int[] matches, ScmInfo dbScmInfo) {
    Changeset changeset = Changeset.newChangesetBuilder()
      .setDate(analysisDate)
      .build();

    Changeset[] dbChangesets = dbScmInfo.getAllChangesets();
    Changeset[] changesets = new Changeset[matches.length];

    for (int i = 0; i < matches.length; i++) {
      if (matches[i] > 0) {
        changesets[i] = dbChangesets[matches[i]];
      } else {
        changesets[i] = changeset;
      }
    }
    return new GeneratedScmInfo(changesets);
  }

  @Override
  public Changeset getLatestChangeset() {
    return delegate.getLatestChangeset();
  }

  @Override
  public Changeset getChangesetForLine(int lineNumber) {
    return delegate.getChangesetForLine(lineNumber);
  }

  @Override
  public boolean hasChangesetForLine(int lineNumber) {
    return delegate.hasChangesetForLine(lineNumber);
  }

  @Override
  public Changeset[] getAllChangesets() {
    return delegate.getAllChangesets();
  }

}
