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
package org.sonar.server.es;

import java.util.concurrent.atomic.AtomicLong;

public class IndexingResult {

  // FIXME should be private
  final AtomicLong total = new AtomicLong(0L);
  private final AtomicLong successes = new AtomicLong(0L);

  IndexingResult clear() {
    total.set(0L);
    successes.set(0L);
    return this;
  }

  public void incrementRequests() {
    total.incrementAndGet();
  }

  public IndexingResult incrementSuccess() {
    successes.incrementAndGet();
    return this;
  }

  public void add(IndexingResult other) {
    total.addAndGet(other.total.get());
    successes.addAndGet(other.successes.get());
  }

  public long getFailures() {
    return total.get() - successes.get();
  }

  public long getTotal() {
    return total.get();
  }

  public long getSuccess() {
    return successes.get();
  }

  public double getSuccessRatio() {
    return total.get() == 0 ? 1.0 : ((1.0 * successes.get()) / total.get());
  }

  public boolean isSuccess() {
    return total.get() == successes.get();
  }
}
