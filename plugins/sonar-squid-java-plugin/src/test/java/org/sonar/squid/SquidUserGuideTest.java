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
package org.sonar.squid;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.number.OrderingComparisons.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.sonar.java.ast.SquidTestUtils.getFile;

import java.util.Collection;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.graph.IncrementalCyclesAndFESSolver;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.bytecode.BytecodeScanner;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceCodeEdgeUsage;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourceMethod;
import org.sonar.squid.api.SourcePackage;
import org.sonar.squid.api.SourceProject;
import org.sonar.squid.indexer.QueryByMeasure;
import org.sonar.squid.indexer.QueryByType;
import org.sonar.squid.indexer.QueryByMeasure.Operator;
import org.sonar.squid.math.MeasuresDistribution;
import org.sonar.squid.measures.Metric;

public class SquidUserGuideTest {

  private static Squid squid;
  private static SourceProject project;

  @BeforeClass
  public static void setup() {
    squid = new Squid(new JavaSquidConfiguration());
    squid.register(JavaAstScanner.class).scanDirectory(getFile("/commons-collections-3.2.1/src"));
    squid.register(BytecodeScanner.class).scanDirectory(getFile("/commons-collections-3.2.1/bin"));
    project = squid.aggregate();
  }

  @Test
  public void getMeasuresOnProject() {
    assertEquals(12, project.getInt(Metric.PACKAGES));
    assertEquals(273, project.getInt(Metric.FILES));
    assertEquals(37, project.getInt(Metric.ANONYMOUS_INNER_CLASSES));
    assertEquals(412, project.getInt(Metric.CLASSES));
    assertEquals(27, project.getInt(Metric.INTERFACES));
    assertEquals(33, project.getInt(Metric.ABSTRACT_CLASSES));
    assertEquals(3805, project.getInt(Metric.METHODS));
    assertEquals(3805, squid.search(new QueryByType(SourceMethod.class), new QueryByMeasure(Metric.ACCESSORS, Operator.EQUALS, 0)).size());
    assertEquals(69, project.getInt(Metric.ACCESSORS));
    assertEquals(63852, project.getInt(Metric.LINES));
    assertEquals(26323, project.getInt(Metric.LINES_OF_CODE));
    assertEquals(6426, project.getInt(Metric.BLANK_LINES));
    assertEquals(12268, project.getInt(Metric.STATEMENTS));
    assertEquals(8475, project.getInt(Metric.COMPLEXITY));
    assertEquals(4668, project.getInt(Metric.BRANCHES));
    assertEquals(21114, project.getInt(Metric.COMMENT_LINES));
    assertEquals(9995, project.getInt(Metric.COMMENT_BLANK_LINES));
    assertEquals(17838, project.getInt(Metric.COMMENT_LINES_WITHOUT_HEADER));
    assertEquals(0.40, project.getDouble(Metric.COMMENT_LINES_DENSITY), 0.01);

  }

  @Test
  public void findCommentedOutCode() {
    assertEquals(70, project.getInt(Metric.COMMENTED_OUT_CODE_LINES));
    Collection<SourceCode> filesWithCommentedCode = squid.search(new QueryByType(SourceFile.class), new QueryByMeasure(
        Metric.COMMENTED_OUT_CODE_LINES, Operator.GREATER_THAN, 0));
    assertEquals(10, filesWithCommentedCode.size());
    assertTrue(filesWithCommentedCode.contains(new SourceFile("org/apache/commons/collections/map/MultiValueMap.java")));
  }

  @Test
  public void getRobertCMartinOOMetrics() {
    SourceCode bufferPackage = squid.search("org/apache/commons/collections/buffer");
    assertEquals(13, bufferPackage.getInt(Metric.CLASSES));
    assertEquals(1, bufferPackage.getInt(Metric.ABSTRACT_CLASSES));
    assertEquals(1, bufferPackage.getInt(Metric.CA));
    assertEquals(14, bufferPackage.getInt(Metric.CE));
    assertEquals(0.93, bufferPackage.getDouble(Metric.INSTABILITY), 0.01);
    assertEquals(0.07, bufferPackage.getDouble(Metric.ABSTRACTNESS), 0.01);
    assertEquals(0.01, bufferPackage.getDouble(Metric.DISTANCE), 0.01);
  }

  @Test
  public void getChidamberAndKemererMetrics() {
    assertEquals(3, squid.search("org/apache/commons/collections/bag/AbstractBagDecorator").getInt(Metric.NOC));
    assertEquals(4, squid.search("org/apache/commons/collections/bag/PredicatedBag").getInt(Metric.DIT));
    assertEquals(15, squid.search("org/apache/commons/collections/ArrayStack").getInt(Metric.RFC));
    assertEquals(3, squid.search("org/apache/commons/collections/ArrayStack").getInt(Metric.LCOM4));
  }

  @Test
  public void getMeasuresOnPublicAPI() {
    assertEquals(3257, project.getInt(Metric.PUBLIC_API));
    assertEquals(2008, project.getInt(Metric.PUBLIC_DOC_API));
  }

  @Test
  public void getDependenciesBetweenPackages() {
    SourceCode collectionsPackage = squid.search("org/apache/commons/collections");
    SourceCode bufferPackage = squid.search("org/apache/commons/collections/buffer");
    SourceCode bidimapPackage = squid.search("org/apache/commons/collections/bidimap");

    assertEquals(SourceCodeEdgeUsage.USES, squid.getEdge(bidimapPackage, collectionsPackage).getUsage());
    assertEquals(SourceCodeEdgeUsage.USES, squid.getEdge(collectionsPackage, bufferPackage).getUsage());
    assertEquals(7, squid.getEdge(collectionsPackage, bufferPackage).getRootEdges().size());
  }

  @Test
  public void detectDependencyCyclesThenFlagFeedbackEdges() {
    Collection<SourceCode> packages = squid.search(new QueryByType(SourcePackage.class));
    IncrementalCyclesAndFESSolver<SourceCode> cyclesAndFESSolver = new IncrementalCyclesAndFESSolver<SourceCode>(squid, packages);

    assertThat(cyclesAndFESSolver.getCycles().size(), is(42));
    assertThat(cyclesAndFESSolver.getSearchCyclesCalls(), lessThan(400L));

    assertThat(cyclesAndFESSolver.getFeedbackEdgeSet().size(), is(13));
    assertThat(cyclesAndFESSolver.getWeightOfFeedbackEdgeSet(), is(145));
    assertThat(cyclesAndFESSolver.getNumberOfLoops(), lessThan(7000));
  }

  @Test
  public void minimumFeedbackShouldBeApproximatedWhenAboveMaxNumberOfCycles() {
    Collection<SourceCode> packages = squid.search(new QueryByType(SourcePackage.class));
    IncrementalCyclesAndFESSolver<SourceCode> cyclesAndFESSolver = new IncrementalCyclesAndFESSolver<SourceCode>(squid, packages);
    assertThat(cyclesAndFESSolver.getFeedbackEdgeSet().size(), lessThan(27));
    assertThat(cyclesAndFESSolver.getWeightOfFeedbackEdgeSet(), lessThan(200));
  }

  @Test
  public void searchMethodsWhoseComplexityIsGreaterThan11() {
    Collection<SourceCode> methods = squid.search(new QueryByType(SourceMethod.class), new QueryByMeasure(Metric.COMPLEXITY,
        Operator.GREATER_THAN, 20));
    assertEquals(11, methods.size());
  }

  @Test
  public void searchFilesWithAbstractClasses() {
    Collection<SourceCode> files = squid.search(new QueryByType(SourceFile.class), new QueryByMeasure(Metric.ABSTRACT_CLASSES,
        Operator.GREATER_THAN, 0));
    assertEquals(32, files.size());
    assertTrue(files.contains(new SourceFile("org/apache/commons/collections/keyvalue/AbstractMapEntry.java")));
  }

  @Test
  public void getDistributionOfMethodComplexity() {
    MeasuresDistribution complexityDistribution = new MeasuresDistribution(squid.search(new QueryByType(SourceMethod.class),
        new QueryByMeasure(Metric.ACCESSORS, Operator.EQUALS, 0)));
    Map<Integer, Integer> distribution = complexityDistribution.distributeAccordingTo(Metric.COMPLEXITY, 1, 2, 3, 4, 5, 7, 10, 15, 20);
    assertEquals(2265, (int) distribution.get(1));
    assertEquals(195, (int) distribution.get(5));
    assertEquals(12, (int) distribution.get(20));
  }
}
