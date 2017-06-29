/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

/**
 * The type Resilient indexer result.
 */
public class ResilientIndexerResult {
  private long total = 0L;
  private long failures = 0L;

  public ResilientIndexerResult clear() {
    total = 0L;
    failures = 0L;
    return this;
  }

  public ResilientIndexerResult increaseFailure() {
    failures += 1;
    total += 1;
    return this;
  }

  public ResilientIndexerResult increaseSuccess() {
    total += 1;
    return this;
  }

  public long getFailures() {
    return failures;
  }

  public long getTotal() {
    return total;
  }

  public long getSuccess() {
    return total - failures;
  }

  /**
   * Get the failure ratio,
   * if the total is 0, we always return 1 in order to break loop
   * @see {@link RecoveryIndexer#recover()}
   */
  public double getFailureRatio() {
    return total == 0 ? 1 : 1.0d * failures / total;
  }

  public double getSuccessRatio() {
    return total == 0 ? 0 : 1 -  1.0d * failures / total;
  }

  public ResilientIndexerResult add(ResilientIndexerResult other) {
    this.total += other.getTotal();
    this.failures += other.getFailures();
    return this;
  }
}
