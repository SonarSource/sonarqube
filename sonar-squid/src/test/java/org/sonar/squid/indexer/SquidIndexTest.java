/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.squid.indexer;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.sonar.squid.api.SourceClass;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.api.SourcePackage;
import org.sonar.squid.api.SourceProject;
import org.sonar.squid.indexer.QueryByMeasure.Operator;
import org.sonar.squid.measures.Metric;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SquidIndexTest {

  private SquidIndex indexer;
  private SourceProject project;
  private SourcePackage packSquid;
  private SourceFile fileSquid;
  private SourceFile file2Squid;
  private SourceCode classSquid;

  @Before
  public void setup() {
    indexer = new SquidIndex();
    project = new SourceProject("Squid Project");
    indexer.index(project);
    packSquid = new SourcePackage("org.sonar.squid");
    project.addChild(packSquid);
    fileSquid = new SourceFile("org.sonar.squid.Squid.java", "Squid.java");
    packSquid.addChild(fileSquid);
    file2Squid = new SourceFile("org.sonar.squid.SquidConfiguration.java", "SquidConfiguration.java");
    packSquid.addChild(file2Squid);
    classSquid = new SourceClass("org.sonar.squid.Squid", "Squid");
    fileSquid.addChild(classSquid);
  }

  @Test
  public void searchSingleResource() {
    SourceCode squidClass = indexer.search("org.sonar.squid.Squid");
    assertEquals(new SourceClass("org.sonar.squid.Squid", "Squid"), squidClass);
    SourceCode javaNCSSClass = indexer.search("org.sonar.squid.JavaNCSS");
    assertNull(javaNCSSClass);
  }

  @Test
  public void searchByType() {
    Collection<SourceCode> resources = indexer.search(new QueryByType(SourceFile.class));
    assertEquals(2, resources.size());
    resources = indexer.search(new QueryByType(SourceClass.class));
    assertEquals(1, resources.size());
    assertTrue(resources.contains(classSquid));
  }
  
  @Test
  public void searchByName() {
    Collection<SourceCode> resources = indexer.search(new QueryByName("Squid.java"));
    assertEquals(1, resources.size());
    assertTrue(resources.contains(fileSquid));
  }

  @Test
  public void searchByParent() {
    Collection<SourceCode> resources = indexer.search(new QueryByParent(packSquid));
    assertEquals(3, resources.size());
  }

  @Test
  public void searchByParentAndByType() {
    Collection<SourceCode> resources = indexer.search(new QueryByParent(packSquid), new QueryByType(SourceClass.class));
    assertEquals(1, resources.size());
    assertTrue(resources.contains(classSquid));
  }

  @Test
  public void searchByMeasure() {
    fileSquid.add(Metric.COMPLEXITY, 2);
    assertEquals(1, indexer.search(new QueryByMeasure(Metric.COMPLEXITY, Operator.GREATER_THAN, 1)).size());
    assertEquals(1, indexer.search(new QueryByMeasure(Metric.COMPLEXITY, Operator.GREATER_THAN_EQUALS, 2)).size());
    assertEquals(0, indexer.search(new QueryByMeasure(Metric.COMPLEXITY, Operator.GREATER_THAN, 3)).size());
    assertEquals(4, indexer.search(new QueryByMeasure(Metric.COMPLEXITY, Operator.LESS_THAN, 1)).size());
    assertEquals(5, indexer.search(new QueryByMeasure(Metric.COMPLEXITY, Operator.LESS_THAN, 3)).size());
    assertEquals(5, indexer.search(new QueryByMeasure(Metric.COMPLEXITY, Operator.LESS_THAN_EQUALS, 2)).size());
    assertEquals(0, indexer.search(new QueryByMeasure(Metric.COMPLEXITY, Operator.EQUALS, 6)).size());
    assertEquals(1, indexer.search(new QueryByMeasure(Metric.COMPLEXITY, Operator.EQUALS, 2)).size());
  }
}
