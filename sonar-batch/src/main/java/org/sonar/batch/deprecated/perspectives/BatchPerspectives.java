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
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.component.Component;
import org.sonar.api.component.Perspective;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Resource;
import org.sonar.core.component.PerspectiveBuilder;
import org.sonar.core.component.PerspectiveNotFoundException;
import org.sonar.core.component.ResourceComponent;

import javax.annotation.CheckForNull;

import java.util.Map;

public class BatchPerspectives implements ResourcePerspectives {

  private final Map<Class<?>, PerspectiveBuilder<?>> builders = Maps.newHashMap();
  private final SonarIndex resourceIndex;

  public BatchPerspectives(PerspectiveBuilder[] builders, SonarIndex resourceIndex) {
    this.resourceIndex = resourceIndex;
    for (PerspectiveBuilder builder : builders) {
      // TODO check duplications
      this.builders.put(builder.getPerspectiveClass(), builder);
    }
  }

  @Override
  @CheckForNull
  public <P extends Perspective> P as(Class<P> perspectiveClass, Component component) {
    if (component.key() == null) {
      return null;
    }
    PerspectiveBuilder<P> builder = builderFor(perspectiveClass);
    return builder.loadPerspective(perspectiveClass, component);
  }

  @Override
  @CheckForNull
  public <P extends Perspective> P as(Class<P> perspectiveClass, Resource resource) {
    Resource indexedResource = resource;
    if (resource.getEffectiveKey() == null) {
      indexedResource = resourceIndex.getResource(resource);
    }
    if (indexedResource != null) {
      return as(perspectiveClass, new ResourceComponent(indexedResource));
    }
    return null;
  }

  @Override
  public <P extends Perspective> P as(Class<P> perspectiveClass, InputPath inputPath) {
    Resource r;
    if (inputPath instanceof InputDir) {
      r = Directory.create(((InputDir) inputPath).relativePath());
    } else if (inputPath instanceof InputFile) {
      r = File.create(((InputFile) inputPath).relativePath());
    } else {
      throw new IllegalArgumentException("Unknow input path type: " + inputPath);
    }
    return as(perspectiveClass, r);
  }

  private <T extends Perspective> PerspectiveBuilder<T> builderFor(Class<T> clazz) {
    PerspectiveBuilder<T> builder = (PerspectiveBuilder<T>) builders.get(clazz);
    if (builder == null) {
      throw new PerspectiveNotFoundException("Perspective class is not registered: " + clazz);
    }
    return builder;
  }
}
