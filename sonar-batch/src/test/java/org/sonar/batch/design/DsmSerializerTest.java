/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.batch.design;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.api.design.Dependency;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.Resource;
import org.sonar.graph.DirectedGraph;
import org.sonar.graph.Dsm;
import org.sonar.graph.DsmManualSorter;
import org.sonar.graph.Edge;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class DsmSerializerTest {
  @Test
  public void serializeEmptyDsm() {
    Dsm<Resource> dsm = new Dsm<Resource>(new DirectedGraph<Resource, Edge<Resource>>());
    assertThat(DsmSerializer.serialize(dsm), is("[]"));
  }

  @Test
  public void serialize() throws IOException {
    Resource foo = Directory.create("src/org/foo").setId(7);
    Resource bar = Directory.create("src/org/bar").setId(8);
    Dependency dep = new Dependency(foo, bar).setId(30l).setWeight(1);

    DirectedGraph<Resource, Dependency> graph = new DirectedGraph<Resource, Dependency>();
    graph.addVertex(foo);
    graph.addVertex(bar);
    graph.addEdge(dep);

    Dsm<Resource> dsm = new Dsm<Resource>(graph);
    DsmManualSorter.sort(dsm, bar, foo); // for test reproductibility
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/batch/design/DsmSerializerTest/dsm.json")).trim();
    assertThat(DsmSerializer.serialize(dsm), is(json));
  }
}
