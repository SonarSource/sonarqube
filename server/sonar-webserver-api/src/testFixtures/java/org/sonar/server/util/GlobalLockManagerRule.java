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
package org.sonar.server.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import static org.sonar.api.utils.Preconditions.checkArgument;

/**
 * {@link org.junit.Rule} implementing {@link GlobalLockManager} to test against this interface without consideration
 * of time, only attempts to acquire a given lock.
 */
public class GlobalLockManagerRule extends ExternalResource implements GlobalLockManager {
  private final Map<String, Deque<Boolean>> lockAttemptsByLockName = new HashMap<>();

  @Test
  public GlobalLockManagerRule addAttempt(String lockName, boolean success) {
    lockAttemptsByLockName.compute(lockName, (k, v) -> {
      Deque<Boolean> queue = v == null ? new ArrayDeque<>() : v;
      queue.push(success);
      return queue;
    });
    return this;
  }

  @Override
  public boolean tryLock(String name) {
    checkArgument(!name.isEmpty() && name.length() <= LOCK_NAME_MAX_LENGTH, "invalid lock name");

    Deque<Boolean> deque = lockAttemptsByLockName.get(name);
    Boolean res = deque == null ? null : deque.pop();
    if (res == null) {
      throw new IllegalStateException("No more attempt value available");
    }
    return res;
  }

  @Override
  public boolean tryLock(String name, int durationSecond) {
    checkArgument(durationSecond > 0, "negative duration not allowed");

    return tryLock(name);
  }
}
