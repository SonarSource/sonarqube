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
package org.sonar.tests.integration;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.ResourceQuery;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class JavaComplexityIT {
  private static Sonar sonar;

  @BeforeClass
  public static void buildServer() {
    sonar = ITUtils.createSonarWsClient();
  }

  @Test
  public void testClasses() {
    assertThat(getMeasure("org.sonar.tests:java-complexity:foo.AnonymousClass", CoreMetrics.CLASSES_KEY).getIntValue(), is(1));
    assertThat(getMeasure("org.sonar.tests:java-complexity:foo.ZeroComplexity", CoreMetrics.CLASSES_KEY).getIntValue(), is(1));
    assertThat(getMeasure("org.sonar.tests:java-complexity", CoreMetrics.CLASSES_KEY).getIntValue(), is(6));
  }

  @Test
  public void testMethods() {
    assertThat(getMeasure("org.sonar.tests:java-complexity:foo.AnonymousClass", CoreMetrics.FUNCTIONS_KEY).getIntValue(), is(2));
    assertThat(getMeasure("org.sonar.tests:java-complexity:foo.ZeroComplexity", CoreMetrics.FUNCTIONS_KEY).getIntValue(), is(0));
    assertThat(getMeasure("org.sonar.tests:java-complexity:foo.ContainsInnerClasses", CoreMetrics.FUNCTIONS_KEY).getIntValue(), is(4));
    assertThat(getMeasure("org.sonar.tests:java-complexity", CoreMetrics.FUNCTIONS_KEY).getIntValue(), is(8));
  }

  @Test
  public void testFileComplexity() {
    assertThat(getMeasure("org.sonar.tests:java-complexity:foo.Helloworld", CoreMetrics.COMPLEXITY_KEY).getIntValue(), is(6));
    assertThat(getMeasure("org.sonar.tests:java-complexity:foo.ContainsInnerClasses", CoreMetrics.COMPLEXITY_KEY).getIntValue(), is(5));
  }

  @Test
  public void testPackageComplexity() {
    assertThat(getMeasure("org.sonar.tests:java-complexity:foo", CoreMetrics.COMPLEXITY_KEY).getIntValue(), is(15));
  }

  @Test
  public void testProjectComplexity() {
    assertThat(getMeasure("org.sonar.tests:java-complexity", CoreMetrics.COMPLEXITY_KEY).getIntValue(), is(15));
  }

  @Test
  public void testAverageMethodComplexity() {
    // complexity 6 / 2 methods
    // BUG http://jira.codehaus.org/browse/SONAR-2152
    // => the complexity of the static block should not be included. Good value should be 4 / 2 = 2
    assertThat(getMeasure("org.sonar.tests:java-complexity:foo.Helloworld", CoreMetrics.FUNCTION_COMPLEXITY_KEY).getValue(), is(3.0));

    // complexity 5 / 4 methods. Real value is 1.25 but round up to 1.3
    assertThat(getMeasure("org.sonar.tests:java-complexity:foo.ContainsInnerClasses", CoreMetrics.FUNCTION_COMPLEXITY_KEY).getValue(), is(1.3));

    // (1 + 3) / 2 = 2
    assertThat(getMeasure("org.sonar.tests:java-complexity:foo.AnonymousClass", CoreMetrics.FUNCTION_COMPLEXITY_KEY).getValue(), is(2.0));

    // ContainsInnerClasses: 5/4
    // Helloworld: 6/2
    // AnonymousClass: 4/2
    // => 15/8=1.875
    // BUG http://jira.codehaus.org/browse/SONAR-2152
    // Should use sum of method complexity, not class complexity.
    assertThat(getMeasure("org.sonar.tests:java-complexity:foo", CoreMetrics.FUNCTION_COMPLEXITY_KEY).getValue(), is(1.9));
    assertThat(getMeasure("org.sonar.tests:java-complexity", CoreMetrics.FUNCTION_COMPLEXITY_KEY).getValue(), is(1.9));
  }

  @Test
  public void testAverageClassComplexity() {
    assertThat(getMeasure("org.sonar.tests:java-complexity:foo.Helloworld", CoreMetrics.CLASS_COMPLEXITY_KEY).getValue(), is(6.0));

    // 1 + 1 + 3 => complexity 5/3
    assertThat(getMeasure("org.sonar.tests:java-complexity:foo.ContainsInnerClasses", CoreMetrics.CLASS_COMPLEXITY_KEY).getValue(), is(1.7));

    // 1 + 1 + 3 + 6 + 0 + 4 => 15/6 = 2.5
    assertThat(getMeasure("org.sonar.tests:java-complexity:foo", CoreMetrics.CLASS_COMPLEXITY_KEY).getValue(), is(2.5));
  }

  @Test
  public void testDistributionOfClassComplexity() {
    // 1 + 1 + 3 + 6 + 0 + 4 => 5 in range [0,5[ and 1 in range [5,10[
    assertThat(getMeasure("org.sonar.tests:java-complexity:foo", CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION_KEY).getData(), is("0=5;5=1;10=0;20=0;30=0;60=0;90=0"));
    assertThat(getMeasure("org.sonar.tests:java-complexity", CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION_KEY).getData(), is("0=5;5=1;10=0;20=0;30=0;60=0;90=0"));
  }

  @Test
  public void testDistributionOfMethodComplexity() {
    // ContainsInnerClasses: 1+ 1 + 2 + 1
    // Helloworld: 1 + 3 (static block is not a method)
    // Anonymous class : 1 + 3
    assertThat(getMeasure("org.sonar.tests:java-complexity:foo", CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY).getData(), is("1=5;2=3;4=0;6=0;8=0;10=0;12=0"));
    assertThat(getMeasure("org.sonar.tests:java-complexity", CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY).getData(), is("1=5;2=3;4=0;6=0;8=0;10=0;12=0"));
  }

  @Test
  public void shouldNotPersistDistributionOnFiles() {
    assertThat(getMeasure("org.sonar.tests:java-complexity:foo.Helloworld", CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION_KEY), nullValue());
    assertThat(getMeasure("org.sonar.tests:java-complexity:foo.Helloworld", CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY), nullValue());
  }

  private Measure getMeasure(String resourceKey, String metricKey) {
    return sonar.find(ResourceQuery.createForMetrics(resourceKey, metricKey)).getMeasure(metricKey);
  }
}
