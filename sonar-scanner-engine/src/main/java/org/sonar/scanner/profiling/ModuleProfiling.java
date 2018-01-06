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

import com.google.common.collect.Maps;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import javax.annotation.Nullable;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.System2;

public class ModuleProfiling extends AbstractTimeProfiling {

  private Map<Phase, PhaseProfiling> profilingPerPhase = new EnumMap<>(Phase.class);
  private Map<String, ItemProfiling> profilingPerBatchStep = new LinkedHashMap<>();
  private final Project module;

  public ModuleProfiling(@Nullable Project module, System2 system) {
    super(system);
    this.module = module;
  }

  public String moduleName() {
    if (module != null) {
      return module.getName();
    }
    return null;
  }

  public PhaseProfiling getProfilingPerPhase(Phase phase) {
    return profilingPerPhase.get(phase);
  }

  public ItemProfiling getProfilingPerBatchStep(String stepName) {
    return profilingPerBatchStep.get(stepName);
  }

  public void addPhaseProfiling(Phase phase) {
    profilingPerPhase.put(phase, PhaseProfiling.create(system(), phase));
  }

  public void addBatchStepProfiling(String stepName) {
    profilingPerBatchStep.put(stepName, new ItemProfiling(system(), stepName));
  }

  public void dump(Properties props) {
    double percent = this.totalTime() / 100.0;
    Map<Object, AbstractTimeProfiling> categories = Maps.newLinkedHashMap();
    categories.putAll(profilingPerPhase);
    categories.putAll(profilingPerBatchStep);

    for (Map.Entry<Object, AbstractTimeProfiling> batchStep : categories.entrySet()) {
      props.setProperty(batchStep.getKey().toString(), Long.toString(batchStep.getValue().totalTime()));
    }

    for (Map.Entry<Object, AbstractTimeProfiling> batchStep : sortByDescendingTotalTime(categories).entrySet()) {
      println(" * " + batchStep.getKey() + " execution time: ", percent, batchStep.getValue());
    }
    // Breakdown per phase
    for (Phase phase : Phase.values()) {
      if (profilingPerPhase.containsKey(phase) && getProfilingPerPhase(phase).hasItems()) {
        println("");
        println(" * " + phase + " execution time breakdown: ", getProfilingPerPhase(phase));
        getProfilingPerPhase(phase).dump(props);
      }
    }
  }

  public void merge(ModuleProfiling other) {
    super.add(other);
    for (Entry<Phase, PhaseProfiling> entry : other.profilingPerPhase.entrySet()) {
      if (!this.profilingPerPhase.containsKey(entry.getKey())) {
        this.addPhaseProfiling(entry.getKey());
      }
      this.getProfilingPerPhase(entry.getKey()).merge(entry.getValue());
    }
    for (Map.Entry<String, ItemProfiling> entry : other.profilingPerBatchStep.entrySet()) {
      if (!this.profilingPerBatchStep.containsKey(entry.getKey())) {
        profilingPerBatchStep.put(entry.getKey(), new ItemProfiling(system(), entry.getKey()));
      }
      this.getProfilingPerBatchStep(entry.getKey()).add(entry.getValue());
    }
  }

}
