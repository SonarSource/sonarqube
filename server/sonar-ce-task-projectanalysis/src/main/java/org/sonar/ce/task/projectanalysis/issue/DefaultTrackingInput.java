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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Collection;

import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.BlockHashSequence;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.LineHashSequence;

public class DefaultTrackingInput implements Input<DefaultIssue> {
  private final Collection<DefaultIssue> issues;
  private final LineHashSequence lineHashes;
  private final BlockHashSequence blockHashes;

  public DefaultTrackingInput(Collection<DefaultIssue> issues, LineHashSequence lineHashes, BlockHashSequence blockHashes) {
    this.issues = issues;
    this.lineHashes = lineHashes;
    this.blockHashes = blockHashes;
  }

  @Override
  public LineHashSequence getLineHashSequence() {
    return lineHashes;
  }

  @Override
  public BlockHashSequence getBlockHashSequence() {
    return blockHashes;
  }

  @Override
  public Collection<DefaultIssue> getIssues() {
    return issues;
  }

}
