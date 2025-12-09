/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.process.cluster.hz;

import com.hazelcast.map.IMap;
import javax.annotation.Nullable;

public class DistributedReference<E> {
  private static final String ATOMIC_KEY = "atomicKey";

  private final IMap<String, E> lockMap;

  public DistributedReference(IMap<String, E> lockMap) {
    this.lockMap = lockMap;
  }

  public E get() {
    return lockMap.get(ATOMIC_KEY);
  }

  public void set(@Nullable E value) {
    lockMap.lock(ATOMIC_KEY);
    try {
      putOrRemove(value);
    } finally {
      lockMap.unlock(ATOMIC_KEY);
    }
  }

  public boolean compareAndSet(@Nullable E expected, @Nullable E newValue) {
    lockMap.lock(ATOMIC_KEY);
    try {
      if ((expected == null && lockMap.get(ATOMIC_KEY) == null) ||
        (expected != null && expected.equals(lockMap.get(ATOMIC_KEY)))) {
        putOrRemove(newValue);
        return true;
      }
      return false;
    } finally {
      lockMap.unlock(ATOMIC_KEY);
    }
  }

  private void putOrRemove(@org.jetbrains.annotations.Nullable E value) {
    if (value != null) {
      lockMap.put(ATOMIC_KEY, value);
    } else {
      lockMap.remove(ATOMIC_KEY);
    }
  }

}
