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
package org.sonar.batch.indexer;

import org.junit.Ignore;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.DefaultResourceCreationLock;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.api.design.Dependency;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Resource;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DefaultSonarIndexTest extends AbstractDbUnitTestCase {

  @Test
  public void indexDependencies() {
    DefaultSonarIndex index = new DefaultSonarIndex(getSession(), null, new DefaultResourceCreationLock());

    Resource from = new JavaFile("org.foo.Foo");
    Resource to = new JavaFile("org.bar.Bar");
    Dependency dependency = new Dependency(from, to);

    index.registerDependency(dependency);

    assertThat(index.getDependencies().size(), is(1));
    assertTrue(index.getDependencies().contains(dependency));
    assertThat(index.getEdge(from, to), is(dependency));

    assertThat(index.getIncomingEdges(to).size(), is(1));
    assertTrue(index.getIncomingEdges(to).contains(dependency));
    assertThat(index.getIncomingEdges(from).isEmpty(), is(true));

    assertThat(index.getOutgoingEdges(from).size(), is(1));
    assertTrue(index.getOutgoingEdges(from).contains(dependency));
    assertThat(index.getOutgoingEdges(to).isEmpty(), is(true));
  }

  @Test(expected = SonarException.class)
  @Ignore("Temporarily log warnings instead of throwing an exception")
  public void failIfLockedAndAddingMeasureOnUnknownResource() {
    DefaultResourceCreationLock lock = new DefaultResourceCreationLock();
    lock.lock();

    DefaultSonarIndex index = new DefaultSonarIndex(getSession(), null, lock);
    index.saveMeasure(new JavaFile("org.foo.Bar"), new Measure(CoreMetrics.LINES, 200.0));
  }
}
