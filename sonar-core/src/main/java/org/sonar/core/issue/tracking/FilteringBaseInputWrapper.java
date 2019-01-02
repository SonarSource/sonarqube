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
package org.sonar.core.issue.tracking;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import static org.sonar.core.util.stream.MoreCollectors.toList;

class FilteringBaseInputWrapper<BASE extends Trackable> implements Input<BASE> {
  private final Input<BASE> baseInput;
  private final List<BASE> nonClosedIssues;

  public FilteringBaseInputWrapper(Input<BASE> baseInput, Predicate<BASE> baseInputFilter) {
    this.baseInput = baseInput;
    Collection<BASE> baseIssues = baseInput.getIssues();
    this.nonClosedIssues = baseIssues.stream()
      .filter(baseInputFilter)
      .collect(toList(baseIssues.size()));
  }

  @Override
  public LineHashSequence getLineHashSequence() {
    return baseInput.getLineHashSequence();
  }

  @Override
  public BlockHashSequence getBlockHashSequence() {
    return baseInput.getBlockHashSequence();
  }

  @Override
  public Collection<BASE> getIssues() {
    return nonClosedIssues;
  }
}
