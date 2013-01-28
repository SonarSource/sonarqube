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

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.annotation.Nullable;

import java.io.OutputStream;
import java.util.Set;

/**
 * GraphsonWriter writes a Graph to a TinkerPop JSON OutputStream.
 *
 * @author Stephen Mallette
 */
public class GraphsonWriter {

  public void write(Graph graph, OutputStream jsonOutputStream, GraphsonMode mode) {
    write(graph, jsonOutputStream, mode, null, null);
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
  public void write(Graph graph, OutputStream jsonOutputStream, GraphsonMode mode, @Nullable Set<String> vertexPropertyKeys, @Nullable Set<String> edgePropertyKeys) {
    try {
      JSONObject root = new JSONObject();
      GraphsonUtil graphson = new GraphsonUtil(mode, null, vertexPropertyKeys, edgePropertyKeys);

      root.put(GraphsonTokens.MODE, mode.toString());

      JSONArray verticesArray = new JSONArray();
      for (Vertex v : graph.getVertices()) {
        verticesArray.add(graphson.jsonFromElement(v));
      }
      root.put(GraphsonTokens.VERTICES, verticesArray);

      JSONArray edgesArray = new JSONArray();
      for (Edge e : graph.getEdges()) {
        edgesArray.add(graphson.jsonFromElement(e));
      }
      root.put(GraphsonTokens.EDGES, edgesArray);

      jsonOutputStream.write(root.toString().getBytes("UTF-8"));
    } catch (Exception e) {
      throw new GraphsonException("Fail to generate GraphSON", e);
    }
  }

}
