/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.squid.bridges;

import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;

import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.doubleThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

public class CommentLinesDensityBridgeTest extends BridgeTestCase {

  @Test
  public void commentLineDensity() {
    verify(context).saveMeasure(eq(new JavaFile("org.apache.struts.config.BaseConfig")), eq(CoreMetrics.COMMENT_LINES_DENSITY), doubleThat(closeTo(48.0, 0.1)));
    verify(context).saveMeasure(eq(new JavaPackage("org.apache.struts.config")), eq(CoreMetrics.COMMENT_LINES_DENSITY), doubleThat(closeTo(35.2, 0.1)));
    verify(context).saveMeasure(eq(project), eq(CoreMetrics.COMMENT_LINES_DENSITY), anyDouble());
  }

}
