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

package org.sonar.server.computation.source;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import org.junit.rules.ExternalResource;
import org.sonar.core.util.CloseableIterator;
import org.sonar.server.computation.component.Component;

import static org.sonar.server.computation.component.Component.Type.FILE;

public class SourceLinesRepositoryRule extends ExternalResource implements SourceLinesRepository {

  private List<String> lines = new ArrayList<>();

  @Override
  protected void after() {
    lines.clear();
  }

  @Override
  public CloseableIterator<String> readLines(Component component) {
    Preconditions.checkNotNull(component, "Component should not be bull");
    if (!component.getType().equals(FILE)) {
      throw new IllegalArgumentException(String.format("Component '%s' is not a file", component));
    }
    return CloseableIterator.from(lines.iterator());
  }

  public SourceLinesRepositoryRule addLine(String line){
    lines.add(line);
    return this;
  }

}
