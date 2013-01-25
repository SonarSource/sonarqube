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

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.io.graphson.ElementFactory;
import com.tinkerpop.blueprints.util.io.graphson.GraphElementFactory;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONMode;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONTokens;
import com.tinkerpop.blueprints.util.wrappers.batch.BatchGraph;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

/**
 * GraphSONReader reads the data from a TinkerPop JSON stream to a graph.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class GraphSONReader {

  private final Graph graph;

  /**
   * @param graph the graph to populate with the JSON data
   */
  public GraphSONReader(final Graph graph) {
    this.graph = graph;
  }

  /**
   * Input the JSON stream data into the graph.
   * In practice, usually the provided graph is empty.
   *
   * @param graph           the graph to populate with the JSON data
   * @param jsonInputStream an InputStream of JSON data
   * @throws java.io.IOException thrown when the JSON data is not correctly formatted
   */
  public static void inputGraph(final Graph graph, final InputStream jsonInputStream) throws IOException, ParseException {
    inputGraph(graph, jsonInputStream, 1000);
  }

  public static void inputGraph(final Graph inputGraph, final InputStream jsonInputStream, int bufferSize) throws IOException, ParseException {
    inputGraph(inputGraph, jsonInputStream, bufferSize, null, null);
  }

  /**
   * Input the JSON stream data into the graph.
   * More control over how data is streamed is provided by this method.
   *
   * @param inputGraph      the graph to populate with the JSON data
   * @param jsonInputStream an InputStream of JSON data
   * @param bufferSize      the amount of elements to hold in memory before committing a transactions (only valid for TransactionalGraphs)
   * @throws java.io.IOException thrown when the JSON data is not correctly formatted
   */
  public static void inputGraph(final Graph inputGraph, final InputStream jsonInputStream, int bufferSize,
                                final Set<String> edgePropertyKeys, final Set<String> vertexPropertyKeys) throws ParseException, IOException {

    JSONParser parser = new JSONParser();
    JSONObject json = (JSONObject) parser.parse(new InputStreamReader(jsonInputStream));

    // if this is a transactional graph then we're buffering
    final BatchGraph graph = BatchGraph.wrap(inputGraph, bufferSize);

    final ElementFactory elementFactory = new GraphElementFactory(graph);

    final GraphSONMode mode = GraphSONMode.valueOf(json.get(GraphSONTokens.MODE).toString());
    GraphSONUtility graphson = new GraphSONUtility(mode, elementFactory, vertexPropertyKeys, edgePropertyKeys);

    JSONArray vertices = (JSONArray) json.get(GraphSONTokens.VERTICES);
    for (Object vertice : vertices) {
      graphson.vertexFromJson((JSONObject) vertice);
    }

    JSONArray edges = (JSONArray) json.get(GraphSONTokens.EDGES);
    for (Object edgeObject : edges) {
      JSONObject edge = (JSONObject) edgeObject;

      final Vertex inV = graph.getVertex(edge.get(GraphSONTokens._IN_V));
      final Vertex outV = graph.getVertex(edge.get(GraphSONTokens._OUT_V));
      graphson.edgeFromJson(edge, outV, inV);
    }
    graph.shutdown();
  }

  /**
   * Input the JSON stream data into the graph.
   * In practice, usually the provided graph is empty.
   *
   * @param jsonInputStream an InputStream of JSON data
   * @throws java.io.IOException thrown when the JSON data is not correctly formatted
   */
  public void inputGraph(final InputStream jsonInputStream) throws IOException, ParseException {
    GraphSONReader.inputGraph(this.graph, jsonInputStream, 1000);
  }

  /**
   * Input the JSON stream data into the graph.
   * In practice, usually the provided graph is empty.
   *
   * @param jsonInputStream an InputStream of JSON data
   * @param bufferSize      the amount of elements to hold in memory before committing a transactions (only valid for TransactionalGraphs)
   * @throws java.io.IOException thrown when the JSON data is not correctly formatted
   */
  public void inputGraph(final InputStream jsonInputStream, int bufferSize) throws IOException, ParseException {
    GraphSONReader.inputGraph(this.graph, jsonInputStream, bufferSize);
  }


}
