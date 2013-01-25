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
import org.apache.commons.io.IOUtils;
import org.sonar.core.graph.graphson.GraphsonMode;
import org.sonar.core.graph.graphson.GraphsonWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class GraphWriter {

  public String write(Graph graph) {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try {
      new GraphsonWriter().write(graph, output, GraphsonMode.COMPACT);
      output.flush();
      output.close();
      return new String(output.toByteArray());
    } catch (IOException e) {
      throw new IllegalStateException("Fail to export graph to JSON", e);
    } finally {
      IOUtils.closeQuietly(output);
    }
  }
}
