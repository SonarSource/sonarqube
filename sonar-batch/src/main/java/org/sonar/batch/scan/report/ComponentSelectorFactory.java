/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.scan.report;

import org.sonar.api.BatchComponent;
import org.sonar.batch.bootstrap.AnalysisMode;
import org.sonar.batch.scan.filesystem.InputFileCache;

public class ComponentSelectorFactory implements BatchComponent {

  private final InputFileCache fileCache;
  private final AnalysisMode mode;

  public ComponentSelectorFactory(InputFileCache fileCache, AnalysisMode mode) {
    this.fileCache = fileCache;
    this.mode = mode;
  }

  public ComponentSelector create() {
    if (mode.isIncremental()) {
      return new IncrementalComponentSelector(fileCache);
    }
    return new DefaultComponentSelector();
  }
}
