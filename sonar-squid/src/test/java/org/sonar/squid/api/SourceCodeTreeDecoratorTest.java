/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.squid.api;

import org.junit.Test;
import org.sonar.squid.measures.Metric;

import static org.junit.Assert.assertEquals;

public class SourceCodeTreeDecoratorTest {

  private int idCounter = 0;

  @Test
  public void addMethodMeasures() {
    SourceCode method1 = new SourceMethod("method1");
    method1.setMeasure(Metric.COMPLEXITY, 4);
    method1.setMeasure(Metric.STATEMENTS, 8);
    method1.setMeasure(Metric.METHODS, 1);
    SourceCode method2 = new SourceMethod("method2");
    method2.setMeasure(Metric.COMPLEXITY, 2);
    method2.setMeasure(Metric.STATEMENTS, 3);
    method2.setMeasure(Metric.METHODS, 1);
    SourceCode method3 = new SourceMethod("method3");
    method3.setMeasure(Metric.COMPLEXITY, 1);
    method3.setMeasure(Metric.STATEMENTS, 3);
    method3.setMeasure(Metric.METHODS, 1);
    SourceCode class1 = new SourceClass("class1");
    class1.addChild(method1);
    class1.addChild(method2);
    class1.addChild(method3);

    SourceProject project = new SourceProject("project");
    project.addChild(class1);
    decorate(project);

    assertEquals(3, class1.getInt(Metric.METHODS));
    assertEquals(7, class1.getInt(Metric.COMPLEXITY));
  }

  private SourceCode createTestMethod(SourceCode classResource) {
    SourceCode method = new SourceMethod("test" + idCounter++);
    classResource.addChild(method);
    return method;
  }

  private SourceCode createTestClass() {
    return new SourceClass("class" + idCounter++);
  }

  private SourceCode createTestComplexityMethod(SourceCode classResource, int complexity) {
    SourceCode method = createTestMethod(classResource);
    method.setMeasure(Metric.COMPLEXITY, complexity);
    return method;
  }

  @Test
  public void classMethodComplexityDistribution() {
    SourceCode testClass = createTestClass();
    createTestComplexityMethod(testClass, 4);
    createTestComplexityMethod(testClass, 2);
    createTestComplexityMethod(testClass, 3);
    SourceProject project = new SourceProject("project");
    project.addChild(testClass);
    decorate(project);
  }

  @Test
  public void addPackageMeasures() {
    SourceCode package1 = new SourcePackage("pack1");
    package1.setMeasure(Metric.CLASSES, 12);
    package1.setMeasure(Metric.METHODS, 87);
    package1.setMeasure(Metric.COMPLEXITY, 834);
    package1.setMeasure(Metric.LINES, 1450);
    package1.setMeasure(Metric.PACKAGES, 1);
    SourceCode package2 = new SourcePackage("pack2");
    package2.setMeasure(Metric.CLASSES, 9);
    package2.setMeasure(Metric.METHODS, 73);
    package2.setMeasure(Metric.COMPLEXITY, 287);
    package2.setMeasure(Metric.LINES, 893);
    package2.setMeasure(Metric.PACKAGES, 1);
    SourceCode package3 = new SourcePackage("pack3");
    package3.setMeasure(Metric.CLASSES, 9);
    package3.setMeasure(Metric.METHODS, 73);
    package3.setMeasure(Metric.COMPLEXITY, 287);
    package3.setMeasure(Metric.LINES, 938);
    package3.setMeasure(Metric.PACKAGES, 1);
    SourceProject prj1 = new SourceProject("prj1");
    prj1.addChild(package1);
    prj1.addChild(package2);
    prj1.addChild(package3);
    decorate(prj1);
    assertEquals(3, prj1.getInt(Metric.PACKAGES));
    assertEquals(30, prj1.getInt(Metric.CLASSES));
    assertEquals(233, prj1.getInt(Metric.METHODS));
    assertEquals(3281, prj1.getInt(Metric.LINES));
  }

  private void decorate(SourceProject project) {
    SourceCodeTreeDecorator decorator = new SourceCodeTreeDecorator(project);
    decorator.decorateWith(Metric.values());
  }
}
