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
package org.sonar.duplications.detector.suffixtree;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractText implements Text {

  protected final List<Object> symbols;

  public AbstractText(int size) {
    this.symbols = new ArrayList<>(size);
  }

  public AbstractText(List<Object> symbols) {
    this.symbols = symbols;
  }

  @Override
  public int length() {
    return symbols.size();
  }

  @Override
  public Object symbolAt(int index) {
    return symbols.get(index);
  }

  @Override
  public List<Object> sequence(int fromIndex, int toIndex) {
    return symbols.subList(fromIndex, toIndex);
  }

}
