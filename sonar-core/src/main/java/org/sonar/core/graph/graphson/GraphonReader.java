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
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.wrappers.batch.BatchGraph;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

/**
 * Greatly inspired by the Blueprints implementation based on Jettison/Jackson
 */
public class GraphonReader {

  public Graph read(InputStream jsonInput, Graph toGraph) {
    return read(jsonInput, toGraph, 1000, null, null);
  }

  /**
   * Input the JSON stream data into the graph.
   * More control over how data is streamed is provided by this method.
   *
   * @param toGraph    the graph to populate with the JSON data
   * @param jsonInput  an InputStream of JSON data
   * @param bufferSize the amount of elements to hold in memory before committing a transactions (only valid for TransactionalGraphs)
   */
  public Graph read(InputStream jsonInput, Graph toGraph, int bufferSize, Set<String> edgePropertyKeys, Set<String> vertexPropertyKeys) {
    try {
      JSONParser parser = new JSONParser();
      JSONObject json = (JSONObject) parser.parse(new InputStreamReader(jsonInput));

      // if this is a transactional graph then we're buffering
      final BatchGraph batchGraph = BatchGraph.wrap(toGraph, bufferSize);

      ElementFactory elementFactory = new ElementFactory(batchGraph);

      final GraphonMode mode = GraphonMode.valueOf(json.get(GraphonTokens.MODE).toString());
      GraphsonUtil graphson = new GraphsonUtil(mode, elementFactory, vertexPropertyKeys, edgePropertyKeys);

      JSONArray vertices = (JSONArray) json.get(GraphonTokens.VERTICES);
      for (Object vertice : vertices) {
        graphson.vertexFromJson((JSONObject) vertice);
      }

      JSONArray edges = (JSONArray) json.get(GraphonTokens.EDGES);
      for (Object edgeObject : edges) {
        JSONObject edge = (JSONObject) edgeObject;
        final Vertex inV = batchGraph.getVertex(edge.get(GraphonTokens._IN_V));
        final Vertex outV = batchGraph.getVertex(edge.get(GraphonTokens._OUT_V));
        graphson.edgeFromJson(edge, outV, inV);
      }
      batchGraph.shutdown();
      return toGraph;
    } catch (Exception e) {
      throw new GraphonException("Unable to parse GraphSON", e);
    }
  }
}
