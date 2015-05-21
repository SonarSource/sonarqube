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
package org.sonar.batch.deprecated.perspectives;

import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.component.Perspective;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.resources.Resource;
import org.sonar.batch.index.BatchComponentCache;

public class BatchPerspectives implements ResourcePerspectives {

  private final Map<Class<?>, PerspectiveBuilder<?>> builders = Maps.newHashMap();
  private final SonarIndex resourceIndex;
  private final BatchComponentCache componentCache;

  public BatchPerspectives(PerspectiveBuilder[] builders, SonarIndex resourceIndex, BatchComponentCache componentCache) {
    this.resourceIndex = resourceIndex;
    this.componentCache = componentCache;
    for (PerspectiveBuilder builder : builders) {
      this.builders.put(builder.getPerspectiveClass(), builder);
    }
  }

  @Override
  @CheckForNull
  public <P extends Perspective> P as(Class<P> perspectiveClass, Resource resource) {
    Resource indexedResource = resource;
    if (resource.getEffectiveKey() == null) {
      indexedResource = resourceIndex.getResource(resource);
    }
    if (indexedResource != null) {
      PerspectiveBuilder<P> builder = builderFor(perspectiveClass);
      return builder.loadPerspective(perspectiveClass, componentCache.get(indexedResource));
    }
    return null;
  }

  @Override
  public <P extends Perspective> P as(Class<P> perspectiveClass, InputPath inputPath) {
    PerspectiveBuilder<P> builder = builderFor(perspectiveClass);
    return builder.loadPerspective(perspectiveClass, componentCache.get(inputPath));
  }

  private <T extends Perspective> PerspectiveBuilder<T> builderFor(Class<T> clazz) {
    PerspectiveBuilder<T> builder = (PerspectiveBuilder<T>) builders.get(clazz);
    if (builder == null) {
      throw new PerspectiveNotFoundException("Perspective class is not registered: " + clazz);
    }
    return builder;
  }
}
