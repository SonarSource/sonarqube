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
package org.sonar.core.graph.graphson;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.tg.TinkerGraphFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static org.fest.assertions.Assertions.assertThat;

public class GraphsonWriterTest {

  @Test
  public void outputGraphNoEmbeddedTypes() throws Exception {
    Graph g = TinkerGraphFactory.createTinkerGraph();

    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    GraphonWriter writer = new GraphonWriter();
    writer.write(g, stream, GraphonMode.NORMAL);

    stream.flush();
    stream.close();

    String jsonString = new String(stream.toByteArray());

    JSONObject rootNode = (JSONObject) JSONValue.parse(jsonString);

    // ensure that the JSON conforms to basic structure and that the right
    // number of graph elements are present.  other tests already cover element formatting
    assertThat(rootNode).isNotNull();
    assertThat(rootNode.containsKey(GraphonTokens.MODE)).isTrue();
    assertThat(rootNode.get(GraphonTokens.MODE)).isEqualTo("NORMAL");

    assertThat(rootNode.containsKey(GraphonTokens.VERTICES)).isTrue();

    JSONArray vertices = (JSONArray) rootNode.get(GraphonTokens.VERTICES);
    assertThat(vertices).hasSize(6);

    assertThat(rootNode.containsKey(GraphonTokens.EDGES)).isTrue();

    JSONArray edges = (JSONArray) rootNode.get(GraphonTokens.EDGES);
    assertThat(edges).hasSize(6);
  }

  @Test
  public void outputGraphWithEmbeddedTypes() throws Exception {
    Graph g = TinkerGraphFactory.createTinkerGraph();

    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    GraphonWriter writer = new GraphonWriter();
    writer.write(g, stream, GraphonMode.EXTENDED);

    stream.flush();
    stream.close();

    String jsonString = new String(stream.toByteArray());

    JSONObject rootNode = (JSONObject) JSONValue.parse(jsonString);

    // ensure that the JSON conforms to basic structure and that the right
    // number of graph elements are present.  other tests already cover element formatting
    assertThat(rootNode).isNotNull();
    assertThat(rootNode.containsKey(GraphonTokens.MODE)).isTrue();
    assertThat(rootNode.get(GraphonTokens.MODE)).isEqualTo("EXTENDED");

    assertThat(rootNode.containsKey(GraphonTokens.VERTICES)).isTrue();

    JSONArray vertices = (JSONArray) rootNode.get(GraphonTokens.VERTICES);
    assertThat(vertices).hasSize(6);

    assertThat(rootNode.containsKey(GraphonTokens.EDGES)).isTrue();

    JSONArray edges = (JSONArray) rootNode.get(GraphonTokens.EDGES);
    assertThat(edges).hasSize(6);
  }

  @Test
  public void outputGraphWithCompact() throws Exception {
    Graph g = TinkerGraphFactory.createTinkerGraph();

    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    GraphonWriter writer = new GraphonWriter();
    writer.write(g, stream, GraphonMode.COMPACT);

    stream.flush();
    stream.close();

    String jsonString = new String(stream.toByteArray());

    JSONObject rootNode = (JSONObject) JSONValue.parse(jsonString);

    // ensure that the JSON conforms to basic structure and that the right
    // number of graph elements are present.  other tests already cover element formatting
    assertThat(rootNode).isNotNull();
    assertThat(rootNode.containsKey(GraphonTokens.MODE)).isTrue();
    assertThat(rootNode.get(GraphonTokens.MODE)).isEqualTo("COMPACT");

    assertThat(rootNode.containsKey(GraphonTokens.VERTICES)).isTrue();

    JSONArray vertices = (JSONArray) rootNode.get(GraphonTokens.VERTICES);
    assertThat(vertices).hasSize(6);

    assertThat(rootNode.containsKey(GraphonTokens.EDGES)).isTrue();

    JSONArray edges = (JSONArray) rootNode.get(GraphonTokens.EDGES);
    assertThat(edges).hasSize(6);
  }
}
