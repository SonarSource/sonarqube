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

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.api.design.Dependency;
import org.sonar.api.resources.JavaPackage;
import org.sonar.api.resources.Resource;
import org.sonar.graph.DirectedGraph;
import org.sonar.graph.Dsm;
import org.sonar.graph.DsmManualSorter;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceCodeEdge;
import org.sonar.squid.api.SourceCodeEdgeUsage;
import org.sonar.squid.api.SourcePackage;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class DsmSerializerTest {

  @Test
  public void serializeEmptyDsm() {
    Dsm dsm = new Dsm(new DirectedGraph());
    assertThat(DsmSerializer.serialize(dsm, new DependencyIndex(), new ResourceIndex()), is("[]"));
  }

  @Test
  public void serialize() throws IOException {

    // squid
    SourcePackage foo = new SourcePackage("org/foo");
    SourcePackage bar = new SourcePackage("org/bar");
    SourceCodeEdge edge = new SourceCodeEdge(foo, bar, SourceCodeEdgeUsage.USES) {
      @Override
      public int getWeight() {
        return 5;
      }
    };

    DirectedGraph<SourceCode, SourceCodeEdge> graph = new DirectedGraph<SourceCode, SourceCodeEdge>();
    graph.addVertex(foo);
    graph.addVertex(bar);
    graph.addEdge(edge);

    // sonar
    Resource fooSonar = new JavaPackage("org.foo");
    fooSonar.setId(7);
    Resource barSonar = new JavaPackage("org.bar");
    barSonar.setId(8);

    Dependency dep = new Dependency(fooSonar, barSonar).setId(30l);
    DependencyIndex depIndex = new DependencyIndex();
    depIndex.put(edge, dep);
    ResourceIndex resourceIndex = new ResourceIndex();
    resourceIndex.put(foo, fooSonar);
    resourceIndex.put(bar, barSonar);

    Dsm<SourceCode> dsm = new Dsm<SourceCode>(graph);
    DsmManualSorter.sort(dsm, bar, foo); // for test reproductibility
    String json = IOUtils.toString(getClass().getResourceAsStream("/org/sonar/plugins/squid/bridges/DsmSerializerTest/dsm.json"));
    assertThat(DsmSerializer.serialize(dsm, depIndex, resourceIndex), is(json));
  }
}
