/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.ce.task.projectexport.steps;

import com.google.protobuf.Message;
import com.sonarsource.governance.projectdump.protobuf.ProjectDump;

/**
 * Writes project dump on disk
 */
public interface DumpWriter {

  /**
   * Writes a single metadata message. Can be called only once.
   * @throws IllegalStateException if metadata has already been written
   * @throws IllegalStateException if already published (see {@link #publish()})
   */
  void write(ProjectDump.Metadata metadata);

  /**
   * Writes a stream of protobuf objects. Streams are appended.
   * @throws IllegalStateException if already published (see {@link #publish()})
   */
  <M extends Message> StreamWriter<M> newStreamWriter(DumpElement<M> elt);

  /**
   * Publishes the dump file by zipping directory and moving zip file to directory /data.
   * @throws IllegalStateException if metadata has not been written
   * @throws IllegalStateException if already published
   */
  void publish();

}
