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
package org.sonar.batch.source;

import com.google.common.collect.ImmutableSet;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.component.Component;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.source.Highlightable;
import org.sonar.batch.index.BatchResource;
import org.sonar.batch.index.ResourceCache;
import org.sonar.core.component.PerspectiveBuilder;
import org.sonar.core.component.ResourceComponent;

import javax.annotation.CheckForNull;

import java.util.Set;

public class HighlightableBuilder extends PerspectiveBuilder<Highlightable> {

  private static final Set<String> SUPPORTED_QUALIFIERS = ImmutableSet.of(Qualifiers.FILE, Qualifiers.UNIT_TEST_FILE);
  private final ResourceCache cache;
  private final SensorStorage sensorStorage;

  public HighlightableBuilder(ResourceCache cache, SensorStorage sensorStorage) {
    super(Highlightable.class);
    this.cache = cache;
    this.sensorStorage = sensorStorage;
  }

  @CheckForNull
  @Override
  public Highlightable loadPerspective(Class<Highlightable> perspectiveClass, Component component) {
    boolean supported = SUPPORTED_QUALIFIERS.contains(component.qualifier());
    if (supported && component instanceof ResourceComponent) {
      BatchResource batchComponent = cache.get(component.key());
      if (batchComponent != null) {
        InputFile path = (InputFile) batchComponent.inputPath();
        if (path != null) {
          return new DefaultHighlightable((DefaultInputFile) path, sensorStorage);
        }
      }
    }
    return null;
  }
}
