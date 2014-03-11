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

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.Reader;
import java.util.Set;

/**
 * Greatly inspired by the Blueprints implementation based on Jettison/Jackson
 */
public class GraphsonReader {

  public Graph read(Reader jsonInput, Graph toGraph) {
    return read(jsonInput, toGraph, null, null);
  }

  /**
   * Input the JSON stream data into the graph.
   * More control over how data is streamed is provided by this method.
   *
   * @param toGraph    the graph to populate with the JSON data
   * @param input      an InputStream of JSON data
   * @param bufferSize the amount of elements to hold in memory before committing a transactions (only valid for TransactionalGraphs)
   */
  public Graph read(Reader input, Graph toGraph, Set<String> edgePropertyKeys, Set<String> vertexPropertyKeys) {
    try {
      JSONParser parser = new JSONParser();
      JSONObject json = (JSONObject) parser.parse(input);

      ElementFactory elementFactory = new ElementFactory(toGraph);
      GraphsonMode mode = GraphsonMode.valueOf(json.get(GraphsonTokens.MODE).toString());
      GraphsonUtil graphson = new GraphsonUtil(mode, elementFactory, vertexPropertyKeys, edgePropertyKeys);

      JSONArray vertices = (JSONArray) json.get(GraphsonTokens.VERTICES);
      for (Object vertice : vertices) {
        graphson.vertexFromJson((JSONObject) vertice);
      }

      JSONArray edges = (JSONArray) json.get(GraphsonTokens.EDGES);
      for (Object edgeObject : edges) {
        JSONObject edge = (JSONObject) edgeObject;
        Vertex inV = toGraph.getVertex(edge.get(GraphsonTokens._IN_V));
        Vertex outV = toGraph.getVertex(edge.get(GraphsonTokens._OUT_V));
        graphson.edgeFromJson(edge, outV, inV);
      }
      toGraph.shutdown();
      return toGraph;
    } catch (Exception e) {
      throw new GraphsonException("Unable to parse GraphSON", e);
    }
  }
}
