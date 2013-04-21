/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.phases;

import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.Set;

public class Phases {

  public static enum Phase {
    MAVEN("Maven"), INIT("Initializers"), SENSOR("Sensors"), DECORATOR("Decorators"), POSTJOB("Post-Jobs");

    private final String label;

    private Phase(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return label;
    }
  }

  private final Set<Phase> enabled = Sets.newHashSet();

  public Phases enable(Phase... phases) {
    enabled.addAll(Arrays.asList(phases));
    return this;
  }

  public boolean isEnabled(Phase phase) {
    return enabled.contains(phase);
  }

  public boolean isFullyEnabled() {
    return enabled.containsAll(Arrays.asList(Phase.values()));
  }
}
