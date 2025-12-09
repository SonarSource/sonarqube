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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import org.sonar.api.utils.System2;

import static org.sonar.ce.task.util.Files2.FILES2;
import static org.sonar.ce.task.util.Protobuf2.PROTOBUF2;

public class StreamWriterImpl<M extends Message> implements StreamWriter<M> {

  private final OutputStream output;

  private StreamWriterImpl(OutputStream output) {
    this.output = new BufferedOutputStream(output);
  }

  @Override
  public void write(M message) {
    PROTOBUF2.writeDelimitedTo(message, output);
  }

  @Override
  public void close() {
    System2.INSTANCE.close(output);
  }

  public static <M extends Message> StreamWriterImpl<M> create(File file) {
    FileOutputStream output = FILES2.openOutputStream(file, true);
    return new StreamWriterImpl<>(output);
  }
}
