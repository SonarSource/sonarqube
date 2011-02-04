/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class ChidamberKemererBridgeTest extends BridgeTestCase {

  @Test
  public void depthInTree() {
    verify(context).saveMeasure(new JavaFile("org.apache.struts.config.FormBeanConfig"), CoreMetrics.DEPTH_IN_TREE, 2.0);
    verify(context, never()).saveMeasure(eq(new JavaPackage("org.apache.struts.config")), eq(CoreMetrics.DEPTH_IN_TREE), anyDouble());
    verify(context, never()).saveMeasure(eq(project), eq(CoreMetrics.DEPTH_IN_TREE), anyDouble());
  }

  @Test
  public void numberOfChildren() {
    verify(context).saveMeasure(eq(new JavaFile("org.apache.struts.config.BaseConfig")), eq(CoreMetrics.NUMBER_OF_CHILDREN), anyDouble());
    verify(context, never()).saveMeasure(eq(new JavaPackage("org.apache.struts.config")), eq(CoreMetrics.NUMBER_OF_CHILDREN), anyDouble());
    verify(context, never()).saveMeasure(eq(project), eq(CoreMetrics.NUMBER_OF_CHILDREN), anyDouble());
  }

  @Test
  public void lcom4() {
    verify(context).saveMeasure(eq(new JavaFile("org.apache.struts.config.BaseConfig")), eq(CoreMetrics.LCOM4), anyDouble());
    verify(context, never()).saveMeasure(eq(new JavaPackage("org.apache.struts.config")), eq(CoreMetrics.LCOM4), anyDouble());
    verify(context, never()).saveMeasure(eq(project), eq(CoreMetrics.LCOM4), anyDouble());
  }
}
