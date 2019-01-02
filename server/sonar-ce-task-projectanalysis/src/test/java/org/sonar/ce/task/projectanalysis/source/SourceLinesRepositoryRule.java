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
package org.sonar.ce.task.projectanalysis.source;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Arrays;
import java.util.Collection;
import org.junit.rules.ExternalResource;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.util.CloseableIterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class SourceLinesRepositoryRule extends ExternalResource implements SourceLinesRepository {

  private Multimap<Integer, String> lines = ArrayListMultimap.create();

  @Override
  protected void after() {
    lines.clear();
  }

  @Override
  public CloseableIterator<String> readLines(Component component) {
    checkNotNull(component, "Component should not be bull");
    if (!component.getType().equals(Component.Type.FILE)) {
      throw new IllegalArgumentException(String.format("Component '%s' is not a file", component));
    }
    Collection<String> componentLines = lines.get(component.getReportAttributes().getRef());
    checkState(!componentLines.isEmpty(), String.format("File '%s' has no source code", component));
    return CloseableIterator.from(componentLines.iterator());
  }

  public SourceLinesRepositoryRule addLine(int componentRef, String line) {
    this.lines.put(componentRef, line);
    return this;
  }

  public SourceLinesRepositoryRule addLines(int componentRef, String... lines) {
    this.lines.putAll(componentRef, Arrays.asList(lines));
    return this;
  }

}
