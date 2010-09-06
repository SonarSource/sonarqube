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

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;

import static org.hamcrest.number.OrderingComparisons.greaterThan;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class DesignBridgeTest extends BridgeTestCase {

  @Test
  public void dependenciesBetweenPackagesOfThisProject() {
    verify(context).saveDependency(argThat(new DependencyMatcher("org.apache.struts.config", "org.apache.struts.util")));
  }

  @Test
  public void dependenciesBetweenFilesOfThisProject() {
    verify(context).saveDependency(argThat(new DependencyMatcher("org.apache.struts.config.ConfigRuleSet", "org.apache.struts.util.RequestUtils")));
  }

  @Test
  public void dependenciesBetweenFilesOfTheSamePackage() {
    verify(context).saveDependency(argThat(new DependencyMatcher("org.apache.struts.validator.LazyValidatorForm", "org.apache.struts.validator.BeanValidatorForm")));
  }

  @Test
  public void packageTangles() {
    verify(context).saveMeasure(eq(project), eq(CoreMetrics.PACKAGE_TANGLES), argThat(greaterThan(10.0)));
    verify(context).saveMeasure(eq(project), eq(CoreMetrics.PACKAGE_EDGES_WEIGHT), argThat(greaterThan(5.0))); // >5%
    verify(context, never()).saveMeasure(eq(new JavaPackage("org.apache.struts.config")), eq(CoreMetrics.PACKAGE_TANGLES), anyDouble());
    verify(context, never()).saveMeasure(eq(new JavaFile("org.apache.struts.config.ConfigRuleSet")), eq(CoreMetrics.PACKAGE_TANGLES), anyDouble());
  }

  @Test
  public void fileTangles() {
    verify(context, never()).saveMeasure(eq(project), eq(CoreMetrics.FILE_TANGLES), anyDouble());
    verify(context).saveMeasure(eq(new JavaPackage("org.apache.struts.config")), eq(CoreMetrics.FILE_TANGLES), argThat(greaterThan(1.0)));
    verify(context).saveMeasure(eq(new JavaPackage("org.apache.struts.config")), eq(CoreMetrics.FILE_EDGES_WEIGHT), argThat(greaterThan(1.0)));
  }

  static class DependencyMatcher extends BaseMatcher<Dependency> {
    private String from;
    private String to;

    DependencyMatcher(String from, String to) {
      this.from = from;
      this.to = to;
    }

    public boolean matches(Object o) {
      if (!(o instanceof Dependency)) {
        return false;
      }
      Dependency dep = (Dependency) o;
      return from.equals(dep.getFrom().getKey()) && to.equals(dep.getTo().getKey());
    }

    public void describeTo(Description description) {
    }
  }
}
