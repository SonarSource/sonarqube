/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.component.Perspective;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.core.component.ComponentKeys;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

public class ScannerPerspectives implements ResourcePerspectives {

  private final Map<Class<?>, PerspectiveBuilder<?>> builders = Maps.newHashMap();
  private final InputComponentStore componentStore;
  private final DefaultInputModule module;

  public ScannerPerspectives(PerspectiveBuilder[] builders, DefaultInputModule module, InputComponentStore componentStore) {
    this.componentStore = componentStore;
    this.module = module;

    for (PerspectiveBuilder builder : builders) {
      this.builders.put(builder.getPerspectiveClass(), builder);
    }
  }

  @Override
  @CheckForNull
  public <P extends Perspective> P as(Class<P> perspectiveClass, Resource resource) {
    InputComponent component = componentStore.getByKey(getComponentKey(resource));
    if (component != null) {
      PerspectiveBuilder<P> builder = builderFor(perspectiveClass);
      return builder.loadPerspective(perspectiveClass, component);
    }
    return null;
  }

  private String getComponentKey(Resource r) {
    if (ResourceUtils.isProject(r) || /* For technical projects */ResourceUtils.isRootProject(r)) {
      return r.getKey();
    } else {
      return ComponentKeys.createEffectiveKey(module.key(), r);
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
      throw new PerspectiveNotFoundException("Perspective class is not registered: " + clazz);
    }
    return builder;
  }
}
