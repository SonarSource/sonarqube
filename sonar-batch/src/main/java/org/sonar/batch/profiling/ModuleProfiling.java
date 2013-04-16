/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.profiling;

import org.sonar.batch.phases.Phases;
import org.sonar.batch.phases.Phases.Phase;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ModuleProfiling extends AbstractTimeProfiling {

  private Map<Phases.Phase, PhaseProfiling> profilingPerPhase = new HashMap<Phases.Phase, PhaseProfiling>();

  public PhaseProfiling getProfilingPerPhase(Phase phase) {
    return profilingPerPhase.get(phase);
  }

  public void addPhaseProfiling(Phase phase) {
    profilingPerPhase.put(phase, PhaseProfiling.create(phase));
  }

  public void dump() {
    for (PhaseProfiling phaseProfiling : sortByDescendingTotalTime(profilingPerPhase.values())) {
      System.out.println(" * " + phaseProfiling.phase() + " execution time: " + phaseProfiling.totalTimeAsString());
    }
    for (Phase phase : Phases.Phase.values()) {
      if (profilingPerPhase.containsKey(phase)) {
        System.out.println("");
        System.out.println(" * " + phase + " execution time breakdown");
        getProfilingPerPhase(phase).dump();
      }
    }
  }

  public void merge(ModuleProfiling other) {
    super.add(other);
    for (Entry<Phases.Phase, PhaseProfiling> entry : other.profilingPerPhase.entrySet()) {
      if (!this.profilingPerPhase.containsKey(entry.getKey())) {
        this.addPhaseProfiling(entry.getKey());
      }
      this.getProfilingPerPhase(entry.getKey()).merge(entry.getValue());
    }
  }

}
