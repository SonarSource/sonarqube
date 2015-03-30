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

import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.highlighting.internal.DefaultHighlighting;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.component.Component;
import org.sonar.api.source.Highlightable;
import org.sonar.batch.deprecated.InputFileComponent;

/**
 * @since 3.6
 */
public class DefaultHighlightable implements Highlightable {

  private final DefaultInputFile inputFile;
  private final SensorStorage sensorStorage;

  public DefaultHighlightable(DefaultInputFile inputFile, SensorStorage sensorStorage) {
    this.inputFile = inputFile;
    this.sensorStorage = sensorStorage;
  }

  @Override
  public HighlightingBuilder newHighlighting() {
    DefaultHighlighting defaultHighlighting = new DefaultHighlighting(sensorStorage);
    defaultHighlighting.onFile(inputFile);
    return new DefaultHighlightingBuilder(defaultHighlighting);
  }

  @Override
  public Component component() {
    return new InputFileComponent(inputFile);
  }

  private static class DefaultHighlightingBuilder implements HighlightingBuilder {

    private final DefaultHighlighting defaultHighlighting;

    public DefaultHighlightingBuilder(DefaultHighlighting defaultHighlighting) {
      this.defaultHighlighting = defaultHighlighting;
    }

    @Override
    public HighlightingBuilder highlight(int startOffset, int endOffset, String typeOfText) {
      TypeOfText type = org.sonar.api.batch.sensor.highlighting.TypeOfText.forCssClass(typeOfText);
      defaultHighlighting.highlight(startOffset, endOffset, type);
      return this;
    }

    @Override
    public void done() {
      defaultHighlighting.save();
    }
  }
}
