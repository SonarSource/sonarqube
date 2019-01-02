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

import org.sonar.api.issue.Issue;

public class NonClosedTracking<RAW extends Trackable, BASE extends Trackable> extends Tracking<RAW, BASE> {
  private final Input<RAW> rawInput;
  private final Input<BASE> baseInput;

  private NonClosedTracking(Input<RAW> rawInput, Input<BASE> baseInput) {
    super(rawInput.getIssues(), baseInput.getIssues());
    this.rawInput = rawInput;
    this.baseInput = baseInput;
  }

  public static <RAW extends Trackable, BASE extends Trackable> NonClosedTracking<RAW, BASE> of(Input<RAW> rawInput, Input<BASE> baseInput) {
    Input<BASE> nonClosedBaseInput = new FilteringBaseInputWrapper<>(baseInput, t -> !Issue.STATUS_CLOSED.equals(t.getStatus()));
    return new NonClosedTracking<>(rawInput, nonClosedBaseInput);
  }

  Input<RAW> getRawInput() {
    return rawInput;
  }

  Input<BASE> getBaseInput() {
    return baseInput;
  }
}
