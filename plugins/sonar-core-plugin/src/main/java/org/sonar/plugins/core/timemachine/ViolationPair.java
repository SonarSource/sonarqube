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
package org.sonar.plugins.core.timemachine;

import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.rules.Violation;

import java.util.Comparator;

public class ViolationPair {

  private final RuleFailureModel pastViolation;
  private final Violation newViolation;
  private final int weight;

  public ViolationPair(RuleFailureModel pastViolation, Violation newViolation, int weight) {
    this.pastViolation = pastViolation;
    this.newViolation = newViolation;
    this.weight = weight;
  }

  public Violation getNewViolation() {
    return newViolation;
  }

  public RuleFailureModel getPastViolation() {
    return pastViolation;
  }

  public static final Comparator<ViolationPair> COMPARATOR = new Comparator<ViolationPair>() {
    @Override
    public int compare(ViolationPair o1, ViolationPair o2) {
      return o2.weight - o1.weight;
    }
  };

}
