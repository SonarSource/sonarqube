/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import javax.annotation.Nullable;
import org.sonar.ce.common.sca.PersistScaStepProvider;
import org.sonar.ce.common.sca.ScaHolder;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.step.ComputationStep;
import org.springframework.beans.factory.annotation.Autowired;

public class PersistScaStep implements ComputationStep {

  private final ComputationStep wrapped;

  @Autowired(required = false)
  public PersistScaStep(ScaHolder scaHolder, TreeRootHolder treeRootHolder) {
    this(scaHolder, treeRootHolder, null);
  }

  @Autowired(required = false)
  public PersistScaStep(ScaHolder scaHolder, TreeRootHolder treeRootHolder,
    @Nullable PersistScaStepProvider provider) {
    if (provider != null) {
      var branchUuid = treeRootHolder.getRoot().getUuid();
      this.wrapped = provider.get(scaHolder, branchUuid);
    } else {
      this.wrapped = null;
    }
  }

  @Override
  public String getDescription() {
    return wrapped != null ? wrapped.getDescription() : "Software composition analysis unavailable";
  }

  @Override
  public void execute(Context context) {
    if (wrapped != null) {
      wrapped.execute(context);
    }
  }
}
