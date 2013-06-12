/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.profiling;

import org.sonar.batch.phases.Phases.Phase;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class PhaseProfiling extends AbstractTimeProfiling {

  private final Phase phase;

  private Map<String, ItemProfiling> profilingPerItem = new HashMap<String, ItemProfiling>();

  private Clock clock;

  public PhaseProfiling(Clock clock, Phase phase) {
    super(clock);
    this.clock = clock;
    this.phase = phase;
  }

  public static PhaseProfiling create(Clock clock, Phase phase) {
    return new PhaseProfiling(clock, phase);
  }

  public Phase phase() {
    return phase;
  }

  public boolean hasItems() {
    return !profilingPerItem.isEmpty();
  }

  public ItemProfiling getProfilingPerItem(Object item) {
    String stringOrSimpleName = toStringOrSimpleName(item);
    return profilingPerItem.get(stringOrSimpleName);
  }

  public void newItemProfiling(Object item) {
    String stringOrSimpleName = toStringOrSimpleName(item);
    profilingPerItem.put(stringOrSimpleName, new ItemProfiling(clock, stringOrSimpleName));
  }

  public void newItemProfiling(String itemName) {
    profilingPerItem.put(itemName, new ItemProfiling(clock, itemName));
  }

  public void merge(PhaseProfiling other) {
    super.add(other);
    for (Entry<String, ItemProfiling> entry : other.profilingPerItem.entrySet()) {
      if (!this.profilingPerItem.containsKey(entry.getKey())) {
        newItemProfiling(entry.getKey());
      }
      this.getProfilingPerItem(entry.getKey()).add(entry.getValue());
    }
  }

  public void dump() {
    double percent = this.totalTime() / 100.0;
    for (ItemProfiling itemProfiling : truncate(sortByDescendingTotalTime(profilingPerItem).values())) {
      println("   o " + itemProfiling.itemName() + ": ", percent, itemProfiling);
    }
  }

  /**
   * Try to use toString if it is not the default {@link Object#toString()}. Else use {@link Class#getSimpleName()}
   * @param o
   * @return
   */
  private String toStringOrSimpleName(Object o) {
    String toString = o.toString();
    if (toString == null || toString.startsWith(o.getClass().getName())) {
      return o.getClass().getSimpleName();
    }
    return toString;
  }

}
