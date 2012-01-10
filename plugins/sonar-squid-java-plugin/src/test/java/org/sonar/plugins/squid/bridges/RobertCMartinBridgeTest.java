/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class RobertCMartinBridgeTest extends BridgeTestCase {

  @Test
  public void couplingsOnPackagesAndFiles() {
    verify(context).saveMeasure(eq(new JavaFile("org.apache.struts.config.BaseConfig")), eq(CoreMetrics.AFFERENT_COUPLINGS), doubleThat(greaterThanOrEqualTo(0.0)));
    verify(context).saveMeasure(eq(new JavaFile("org.apache.struts.config.BaseConfig")), eq(CoreMetrics.EFFERENT_COUPLINGS), doubleThat(greaterThanOrEqualTo(0.0)));

    verify(context).saveMeasure(eq(new JavaPackage("org.apache.struts.config")), eq(CoreMetrics.AFFERENT_COUPLINGS), doubleThat(greaterThanOrEqualTo(1.0)));
    verify(context).saveMeasure(eq(new JavaPackage("org.apache.struts.config")), eq(CoreMetrics.EFFERENT_COUPLINGS), doubleThat(greaterThanOrEqualTo(1.0)));
  }

  @Test
  public void noCouplingsOnProject() {
    verify(context, never()).saveMeasure(eq(project), eq(CoreMetrics.AFFERENT_COUPLINGS), anyDouble());
    verify(context, never()).saveMeasure(eq(project), eq(CoreMetrics.EFFERENT_COUPLINGS), anyDouble());
  }
}
