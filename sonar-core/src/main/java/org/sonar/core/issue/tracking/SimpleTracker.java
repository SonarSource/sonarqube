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

/**
 * A simplified version of {@link Tracker}, which doesn't use line hash sequences nor block hash sequences and
 * only has two steps instead of 5 steps.
 */
public class SimpleTracker<RAW extends Trackable, BASE extends Trackable> extends AbstractTracker<RAW, BASE> {

  public Tracking<RAW, BASE> track(Collection<RAW> rawInput, Collection<BASE> baseInput) {
    Tracking<RAW, BASE> tracking = new Tracking<>(rawInput, baseInput);

    // 1. match issues with same rule, same line and same line hash, but not necessarily with same message
    match(tracking, LineAndLineHashKey::new);

    // 2. match issues with same rule, same message and same line hash
    match(tracking, LineHashAndMessageKey::new);

    return tracking;
  }
}
