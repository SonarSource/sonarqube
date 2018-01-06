/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.profiling;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.sonar.api.utils.System2;

public class PhaseProfiling extends AbstractTimeProfiling {

  private final Phase phase;

  private Map<String, ItemProfiling> profilingPerItem = new HashMap<>();

  PhaseProfiling(System2 system, Phase phase) {
    super(system);
    this.phase = phase;
  }

  public static PhaseProfiling create(System2 system, Phase phase) {
    return new PhaseProfiling(system, phase);
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
    profilingPerItem.put(stringOrSimpleName, new ItemProfiling(system(), stringOrSimpleName));
  }

  public void newItemProfiling(String itemName) {
    profilingPerItem.put(itemName, new ItemProfiling(system(), itemName));
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

  public void dump(Properties props) {
    double percent = this.totalTime() / 100.0;
    for (ItemProfiling itemProfiling : profilingPerItem.values()) {
      props.setProperty(itemProfiling.itemName(), Long.toString(itemProfiling.totalTime()));
    }
    for (ItemProfiling itemProfiling : truncate(sortByDescendingTotalTime(profilingPerItem).values())) {
      println("   o " + itemProfiling.itemName() + ": ", percent, itemProfiling);
    }
  }

  /**
   * Try to use toString if it is not the default {@link Object#toString()}. Else use {@link Class#getSimpleName()}
   * @param o
   * @return
   */
  private static String toStringOrSimpleName(Object o) {
    String toString = o.toString();
    if (toString == null || toString.startsWith(o.getClass().getName())) {
      return o.getClass().getSimpleName();
    }
    return toString;
  }

}
