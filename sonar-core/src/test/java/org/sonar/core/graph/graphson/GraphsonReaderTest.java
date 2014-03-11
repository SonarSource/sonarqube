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
package org.sonar.core.graph.graphson;

import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GraphsonReaderTest {

  @Test
  public void inputGraphModeExtended() throws Exception {
    TinkerGraph graph = new TinkerGraph();

    String json = "{ \"mode\":\"EXTENDED\", \"vertices\": [ {\"_id\":1, \"_type\":\"vertex\", \"test\": { \"type\":\"string\", \"value\":\"please work\"}, \"testlist\":{\"type\":\"list\", \"value\":[{\"type\":\"int\", \"value\":1}, {\"type\":\"int\",\"value\":2}, {\"type\":\"int\",\"value\":3}, {\"type\":\"unknown\",\"value\":null}]}, \"testmap\":{\"type\":\"map\", \"value\":{\"big\":{\"type\":\"long\", \"value\":10000000000}, \"small\":{\"type\":\"double\", \"value\":0.4954959595959}, \"nullKey\":{\"type\":\"unknown\", \"value\":null}}}}, {\"_id\":2, \"_type\":\"vertex\", \"testagain\":{\"type\":\"string\", \"value\":\"please work again\"}}], \"edges\":[{\"_id\":100, \"_type\":\"edge\", \"_outV\":1, \"_inV\":2, \"_label\":\"works\", \"teste\": {\"type\":\"string\", \"value\":\"please worke\"}, \"keyNull\":{\"type\":\"unknown\", \"value\":null}}]}";

    StringReader input = new StringReader(json);
    new GraphsonReader().read(input, graph);

    Assert.assertEquals(2, getIterableCount(graph.getVertices()));
    Assert.assertEquals(1, getIterableCount(graph.getEdges()));

    Vertex v1 = graph.getVertex(1);
    Assert.assertNotNull(v1);
    Assert.assertEquals("please work", v1.getProperty("test"));

    Map map = (Map) v1.getProperty("testmap");
    Assert.assertNotNull(map);
    Assert.assertEquals(10000000000l, Long.parseLong(map.get("big").toString()));
    Assert.assertEquals(0.4954959595959, Double.parseDouble(map.get("small").toString()), 0);
    Assert.assertNull(map.get("nullKey"));

    List list = (List) v1.getProperty("testlist");
    Assert.assertEquals(4, list.size());

    boolean foundNull = false;
    for (int ix = 0; ix < list.size(); ix++) {
      if (list.get(ix) == null) {
        foundNull = true;
        break;
      }
    }

    Assert.assertTrue(foundNull);

    Vertex v2 = graph.getVertex(2);
    Assert.assertNotNull(v2);
    Assert.assertEquals("please work again", v2.getProperty("testagain"));

    Edge e = graph.getEdge(100);
    Assert.assertNotNull(e);
    Assert.assertEquals("works", e.getLabel());
    Assert.assertEquals(v1, e.getVertex(Direction.OUT));
    Assert.assertEquals(v2, e.getVertex(Direction.IN));
    Assert.assertEquals("please worke", e.getProperty("teste"));
    Assert.assertNull(e.getProperty("keyNull"));

  }

  @Test
  public void inputGraphModeNormal() throws Exception {
    TinkerGraph graph = new TinkerGraph();

    String json = "{ \"mode\":\"NORMAL\",\"vertices\": [ {\"_id\":1, \"_type\":\"vertex\", \"test\": \"please work\", \"testlist\":[1, 2, 3, null], \"testmap\":{\"big\":10000000000, \"small\":0.4954959595959, \"nullKey\":null}}, {\"_id\":2, \"_type\":\"vertex\", \"testagain\":\"please work again\"}], \"edges\":[{\"_id\":100, \"_type\":\"edge\", \"_outV\":1, \"_inV\":2, \"_label\":\"works\", \"teste\": \"please worke\", \"keyNull\":null}]}";

    StringReader input = new StringReader(json);
    new GraphsonReader().read(input, graph);

    Assert.assertEquals(2, getIterableCount(graph.getVertices()));
    Assert.assertEquals(1, getIterableCount(graph.getEdges()));

    Vertex v1 = graph.getVertex(1);
    Assert.assertNotNull(v1);
    Assert.assertEquals("please work", v1.getProperty("test"));

    Map map = (Map) v1.getProperty("testmap");
    Assert.assertNotNull(map);
    Assert.assertEquals(10000000000l, Long.parseLong(map.get("big").toString()));
    Assert.assertEquals(0.4954959595959, Double.parseDouble(map.get("small").toString()), 0);
    Assert.assertNull(map.get("nullKey"));

    List list = (List) v1.getProperty("testlist");
    Assert.assertEquals(4, list.size());

    boolean foundNull = false;
    for (int ix = 0; ix < list.size(); ix++) {
      if (list.get(ix) == null) {
        foundNull = true;
        break;
      }
    }

    Assert.assertTrue(foundNull);

    Vertex v2 = graph.getVertex(2);
    Assert.assertNotNull(v2);
    Assert.assertEquals("please work again", v2.getProperty("testagain"));

    Edge e = graph.getEdge(100);
    Assert.assertNotNull(e);
    Assert.assertEquals("works", e.getLabel());
    Assert.assertEquals(v1, e.getVertex(Direction.OUT));
    Assert.assertEquals(v2, e.getVertex(Direction.IN));
    Assert.assertEquals("please worke", e.getProperty("teste"));
    Assert.assertNull(e.getProperty("keyNull"));

  }

  @Test
  public void inputGraphModeCompact() throws Exception {
    TinkerGraph graph = new TinkerGraph();

    String json = "{ \"mode\":\"COMPACT\",\"vertices\": [ {\"_id\":1, \"test\": \"please work\", \"testlist\":[1, 2, 3, null], \"testmap\":{\"big\":10000000000, \"small\":0.4954959595959, \"nullKey\":null}}, {\"_id\":2, \"testagain\":\"please work again\"}], \"edges\":[{\"_id\":100, \"_outV\":1, \"_inV\":2, \"_label\":\"works\", \"teste\": \"please worke\", \"keyNull\":null}]}";

    StringReader input = new StringReader(json);
    new GraphsonReader().read(input, graph);

    Assert.assertEquals(2, getIterableCount(graph.getVertices()));
    Assert.assertEquals(1, getIterableCount(graph.getEdges()));

    Vertex v1 = graph.getVertex(1);
    Assert.assertNotNull(v1);
    Assert.assertEquals("please work", v1.getProperty("test"));

    Map map = (Map) v1.getProperty("testmap");
    Assert.assertNotNull(map);
    Assert.assertEquals(10000000000l, Long.parseLong(map.get("big").toString()));
    Assert.assertEquals(0.4954959595959, Double.parseDouble(map.get("small").toString()), 0);
    Assert.assertNull(map.get("nullKey"));

    List list = (List) v1.getProperty("testlist");
    Assert.assertEquals(4, list.size());

    boolean foundNull = false;
    for (Object aList : list) {
      if (aList == null) {
        foundNull = true;
        break;
      }
    }

    Assert.assertTrue(foundNull);

    Vertex v2 = graph.getVertex(2);
    Assert.assertNotNull(v2);
    Assert.assertEquals("please work again", v2.getProperty("testagain"));

    Edge e = graph.getEdge(100);
    Assert.assertNotNull(e);
    Assert.assertEquals("works", e.getLabel());
    Assert.assertEquals(v1, e.getVertex(Direction.OUT));
    Assert.assertEquals(v2, e.getVertex(Direction.IN));
    Assert.assertEquals("please worke", e.getProperty("teste"));
    Assert.assertNull(e.getProperty("keyNull"));

  }

  @Test
  public void inputGraphExtendedFullCycle() throws Exception {
    TinkerGraph graph = TinkerGraphFactory.createTinkerGraph();

    StringWriter stream = new StringWriter();

    GraphsonWriter writer = new GraphsonWriter();
    writer.write(graph, stream, GraphsonMode.EXTENDED);

    stream.flush();
    stream.close();

    String jsonString = stream.toString();

    StringReader input = new StringReader(jsonString);

    TinkerGraph emptyGraph = new TinkerGraph();
    new GraphsonReader().read(input, emptyGraph);

    Assert.assertEquals(6, getIterableCount(emptyGraph.getVertices()));
    Assert.assertEquals(6, getIterableCount(emptyGraph.getEdges()));

    for (Vertex v : graph.getVertices()) {
      Vertex found = emptyGraph.getVertex(v.getId());

      Assert.assertNotNull(v);

      for (String key : found.getPropertyKeys()) {
        Assert.assertEquals(v.getProperty(key), found.getProperty(key));
      }
    }

    for (Edge e : graph.getEdges()) {
      Edge found = emptyGraph.getEdge(e.getId());

      Assert.assertNotNull(e);

      for (String key : found.getPropertyKeys()) {
        Assert.assertEquals(e.getProperty(key), found.getProperty(key));
      }
    }

  }

  @Test
  public void inputGraphCompactFullCycle() throws Exception {
    TinkerGraph graph = TinkerGraphFactory.createTinkerGraph();

    StringWriter stream = new StringWriter();

    Set<String> edgeKeys = new HashSet<String>();
    edgeKeys.add(GraphsonTokens._ID);
    edgeKeys.add(GraphsonTokens._IN_V);
    edgeKeys.add(GraphsonTokens._OUT_V);
    edgeKeys.add(GraphsonTokens._LABEL);

    Set<String> vertexKeys = Sets.newHashSet();
    vertexKeys.add(GraphsonTokens._ID);

    GraphsonWriter writer = new GraphsonWriter();
    writer.write(graph, stream, GraphsonMode.EXTENDED, vertexKeys, edgeKeys);

    stream.flush();
    stream.close();

    String jsonString = stream.toString();
    StringReader input = new StringReader(jsonString);

    TinkerGraph emptyGraph = new TinkerGraph();
    new GraphsonReader().read(input, emptyGraph);

    Assert.assertEquals(6, getIterableCount(emptyGraph.getVertices()));
    Assert.assertEquals(6, getIterableCount(emptyGraph.getEdges()));

    for (Vertex v : graph.getVertices()) {
      Vertex found = emptyGraph.getVertex(v.getId());

      Assert.assertNotNull(v);

      for (String key : found.getPropertyKeys()) {
        Assert.assertEquals(v.getProperty(key), found.getProperty(key));
      }

      // no properties should be here
      Assert.assertEquals(null, found.getProperty("name"));
    }

    for (Edge e : graph.getEdges()) {
      Edge found = emptyGraph.getEdge(e.getId());

      Assert.assertNotNull(e);

      for (String key : found.getPropertyKeys()) {
        Assert.assertEquals(e.getProperty(key), found.getProperty(key));
      }

      // no properties should be here
      Assert.assertEquals(null, found.getProperty("weight"));
    }

  }

  @Test(expected = GraphsonException.class)
  public void inputGraphCompactFullCycleBroken() throws Exception {
    TinkerGraph graph = TinkerGraphFactory.createTinkerGraph();

    StringWriter stream = new StringWriter();

    Set<String> edgeKeys = new HashSet<String>();
    edgeKeys.add(GraphsonTokens._IN_V);
    edgeKeys.add(GraphsonTokens._OUT_V);
    edgeKeys.add(GraphsonTokens._LABEL);

    Set<String> vertexKeys = Sets.newHashSet();

    GraphsonWriter writer = new GraphsonWriter();
    writer.write(graph, stream, GraphsonMode.COMPACT, vertexKeys, edgeKeys);

    stream.flush();
    stream.close();

    String jsonString = writer.toString();
    StringReader input = new StringReader(jsonString);

    TinkerGraph emptyGraph = new TinkerGraph();
    new GraphsonReader().read(input, emptyGraph);

  }

  private int getIterableCount(Iterable elements) {
    int counter = 0;

    Iterator iterator = elements.iterator();
    while (iterator.hasNext()) {
      iterator.next();
      counter++;
    }

    return counter;
  }
}
