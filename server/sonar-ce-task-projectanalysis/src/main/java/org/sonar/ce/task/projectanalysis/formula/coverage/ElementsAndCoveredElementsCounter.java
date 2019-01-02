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
package org.sonar.ce.task.projectanalysis.formula.coverage;

import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.formula.Counter;
import org.sonar.ce.task.projectanalysis.formula.CounterInitializationContext;

/**
 * A counter used to create a measure which are based on a count of elements and coveredElements.
 */
public abstract class ElementsAndCoveredElementsCounter implements Counter<ElementsAndCoveredElementsCounter> {
  protected long elements = 0L;
  protected long coveredElements = 0L;

  @Override
  public void aggregate(ElementsAndCoveredElementsCounter counter) {
    this.elements += counter.elements;
    this.coveredElements += counter.coveredElements;
  }

  @Override
  public void initialize(CounterInitializationContext context) {
    Component component = context.getLeaf();
    if (component.getType() == Component.Type.FILE && component.getFileAttributes().isUnitTest()) {
      return;
    }
    initializeForSupportedLeaf(context);
  }

  protected abstract void initializeForSupportedLeaf(CounterInitializationContext counterContext);
}
