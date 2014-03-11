/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

package org.sonar.squid.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class CodeScanner<V extends CodeVisitor> {

  private List<CodeVisitor> visitors = new ArrayList<CodeVisitor>();

  public abstract Collection<Class<? extends V>> getVisitorClasses();

  public void accept(CodeVisitor visitor) {
    visitors.add(visitor);
  }

  public List<V> getVisitors() {
    return (List<V>) visitors;
  }
}
