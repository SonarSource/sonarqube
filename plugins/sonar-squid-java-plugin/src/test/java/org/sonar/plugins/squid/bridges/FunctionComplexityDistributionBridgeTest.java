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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Matchers.*;
import org.sonar.api.resources.JavaPackage;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.test.IsMeasure;
import static org.hamcrest.Matchers.any;

public class FunctionComplexityDistributionBridgeTest extends BridgeTestCase{

  @Test
  public void functionComplexityDistribution() {
    verify(context).saveMeasure(eq(new JavaPackage("org.apache.struts.config")), argThat(new IsMeasure(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, "1=186;2=88;4=11;6=12;8=7;10=2;12=8")));
    verify(context, never()).saveMeasure(eq(new JavaFile("org.apache.struts.config.ConfigRuleSet")), eq(new Measure(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, "equals() on measure only uses the metric")));
    verify(context, never()).saveMeasure(eq(project), eq(new Measure(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, "equals() on measure only uses the metric")));
  }
}
