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
package org.sonar.scanner.deprecated.perspectives;

import java.util.HashMap;
import java.util.Map;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.component.Perspective;
import org.sonar.api.component.ResourcePerspectives;

public class ScannerPerspectives implements ResourcePerspectives {

  private final Map<Class<?>, PerspectiveBuilder<?>> builders = new HashMap<>();

  public ScannerPerspectives(PerspectiveBuilder[] builders) {
    for (PerspectiveBuilder builder : builders) {
      this.builders.put(builder.getPerspectiveClass(), builder);
    }
  }

  @Override
  public <P extends Perspective> P as(Class<P> perspectiveClass, InputPath inputPath) {
    PerspectiveBuilder<P> builder = builderFor(perspectiveClass);
    return builder.loadPerspective(perspectiveClass, inputPath);
  }

  private <T extends Perspective> PerspectiveBuilder<T> builderFor(Class<T> clazz) {
    PerspectiveBuilder<T> builder = (PerspectiveBuilder<T>) builders.get(clazz);
    if (builder == null) {
      throw new IllegalStateException("Perspective class is not registered: " + clazz);
    }
    return builder;
  }
}
