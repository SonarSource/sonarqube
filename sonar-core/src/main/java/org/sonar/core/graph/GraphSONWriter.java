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

package org.sonar.core.graph;


import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONMode;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONTokens;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

/**
 * GraphSONWriter writes a Graph to a TinkerPop JSON OutputStream.
 *
 * @author Stephen Mallette
 */
public class GraphSONWriter {

  private final Graph graph;

  /**
   * @param graph the Graph to pull the data from
   */
  public GraphSONWriter(final Graph graph) {
    this.graph = graph;
  }

  /**
   * Write the data in a Graph to a JSON OutputStream. All keys are written to JSON. Utilizing
   * GraphSONMode.NORMAL.
   *
   * @param graph            the graph to serialize to JSON
   * @param jsonOutputStream the JSON OutputStream to write the Graph data to
   * @throws java.io.IOException thrown if there is an error generating the JSON data
   */
  public static void outputGraph(final Graph graph, final OutputStream jsonOutputStream) throws IOException {
    final GraphSONWriter writer = new GraphSONWriter(graph);
    writer.outputGraph(jsonOutputStream, null, null, GraphSONMode.NORMAL);
  }

  /**
   * Write the data in a Graph to a JSON OutputStream. All keys are written to JSON.
   *
   * @param graph            the graph to serialize to JSON
   * @param jsonOutputStream the JSON OutputStream to write the Graph data to
   * @param mode             determines the format of the GraphSON
   * @throws java.io.IOException thrown if there is an error generating the JSON data
   */
  public static void outputGraph(final Graph graph, final OutputStream jsonOutputStream,
                                 final GraphSONMode mode) throws IOException {
    final GraphSONWriter writer = new GraphSONWriter(graph);
    writer.outputGraph(jsonOutputStream, null, null, mode);
  }

  /**
   * Write the data in a Graph to a JSON OutputStream.
   *
   * @param graph              the graph to serialize to JSON
   * @param jsonOutputStream   the JSON OutputStream to write the Graph data to
   * @param vertexPropertyKeys the keys of the vertex elements to write to JSON
   * @param edgePropertyKeys   the keys of the edge elements to write to JSON
   * @param mode               determines the format of the GraphSON
   * @throws java.io.IOException thrown if there is an error generating the JSON data
   */
  public static void outputGraph(final Graph graph, final OutputStream jsonOutputStream,
                                 final Set<String> vertexPropertyKeys, final Set<String> edgePropertyKeys,
                                 final GraphSONMode mode) throws IOException {
    final GraphSONWriter writer = new GraphSONWriter(graph);
    writer.outputGraph(jsonOutputStream, vertexPropertyKeys, edgePropertyKeys, mode);
  }

  /**
   * Write the data in a Graph to a JSON OutputStream.
   *
   * @param jsonOutputStream   the JSON OutputStream to write the Graph data to
   * @param vertexPropertyKeys the keys of the vertex elements to write to JSON
   * @param edgePropertyKeys   the keys of the edge elements to write to JSON
   * @param mode               determines the format of the GraphSON
   * @throws java.io.IOException thrown if there is an error generating the JSON data
   */
  public void outputGraph(final OutputStream jsonOutputStream, final Set<String> vertexPropertyKeys,
                          final Set<String> edgePropertyKeys, final GraphSONMode mode) throws IOException {

    JSONObject root = new JSONObject();

    final GraphSONUtility graphson = new GraphSONUtility(mode, null, vertexPropertyKeys, edgePropertyKeys);

    root.put(GraphSONTokens.MODE, mode.toString());

    JSONArray verticesArray = new JSONArray();
    for (Vertex v : this.graph.getVertices()) {
      verticesArray.add(graphson.objectNodeFromElement(v));
    }
    root.put(GraphSONTokens.VERTICES, verticesArray);

    JSONArray edgesArray = new JSONArray();
    for (Edge e : this.graph.getEdges()) {
      edgesArray.add(graphson.objectNodeFromElement(e));
    }
    root.put(GraphSONTokens.EDGES, edgesArray);

    jsonOutputStream.write(root.toString().getBytes());
  }

}
