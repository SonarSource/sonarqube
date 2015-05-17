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
package org.sonar.api.charts;

import org.sonar.api.ServerSide;

import java.awt.image.BufferedImage;

/**
 * Extension point to generate charts
 *
 * @since 1.10
 * @deprecated in 4.5.1, replaced by Javascript charts
 */
@Deprecated
@ServerSide
public interface Chart {
  String getKey();

  /**
   * The method to implement to generate the chart
   *
   * @param params the chart parameters
   * @return the image generated
   */
  BufferedImage generateImage(ChartParameters params);
}
