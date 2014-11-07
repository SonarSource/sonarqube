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
package org.sonar.graph;

import org.apache.commons.lang.ArrayUtils;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public final class DsmScanner {

  private final LineNumberReader reader;
  private static final String CELL_SEPARATOR = "|";
  private static final char FEEDBACK_EDGE_FLAG = '*';
  private final DirectedGraph<String, StringEdge> graph = DirectedGraph.createStringDirectedGraph();
  private String[] vertices;
  private Set<Edge> feedbackEdges = new LinkedHashSet<Edge>();

  private DsmScanner(Reader reader) {
    this.reader = new LineNumberReader(reader);
  }

  private Dsm<String> scan() {
    try {
      readColumnHeadersAndcreateDsmBuilder();
      for (int i = 0; i < vertices.length; i++) {
        readRow(i);
      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to read DSM content.", e); // NOSONAR
    }
    Dsm<String> dsm = new Dsm<String>(graph, graph.getVertices(), feedbackEdges);
    DsmManualSorter.sort(dsm, Arrays.asList(vertices));
    return dsm;
  }

  private void readRow(int i) throws IOException {
    String[] tokens = splitLine(reader.readLine());
    for (int j = 1; j < tokens.length - 1; j++) {
      int toVertexIndex = j - 1;
      int weight = extractWeight(tokens[j]);
      if (i != toVertexIndex) {
        StringEdge edge = new StringEdge(vertices[toVertexIndex], vertices[i], weight);
        if (isFeedbackEdge(tokens[j])) {
          feedbackEdges.add(edge);
        }
        graph.addEdge(edge);
      }
    }
  }

  private boolean isFeedbackEdge(String cellContent) {
    return cellContent.indexOf(FEEDBACK_EDGE_FLAG) != -1;
  }

  private int extractWeight(String stringContent) {
    try {
      return Integer.valueOf(stringContent.replace(FEEDBACK_EDGE_FLAG, ' ').trim());
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private void readColumnHeadersAndcreateDsmBuilder() throws IOException {
    String[] tokens = splitLine(reader.readLine());
    if (tokens != null) {
      vertices = new String[tokens.length - 2];
      System.arraycopy(tokens, 1, vertices, 0, tokens.length - 2);
      graph.addVertices(Arrays.asList(vertices));
    }
  }

  private String[] splitLine(String line) {
    if (line == null) {
      return ArrayUtils.EMPTY_STRING_ARRAY;
    }
    String[] tokens = line.split("\\" + CELL_SEPARATOR);
    String[] result = new String[tokens.length];
    for (int i = 0; i < tokens.length; i++) {
      result[i] = tokens[i].trim();
    }
    return result;
  }

  public static Dsm<String> scan(String textDsm) {
    StringReader reader = new StringReader(textDsm);
    return scan(reader);
  }

  public static Dsm<String> scan(Reader dsmReader) {
    DsmScanner scanner = new DsmScanner(dsmReader);
    return scanner.scan();
  }
}
