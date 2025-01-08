/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.sonar.ce.task.projectexport.steps.DumpElement.METADATA;
import static org.sonar.ce.task.util.Files2.FILES2;
import static org.sonar.ce.task.util.Protobuf2.PROTOBUF2;

public class MutableDumpReaderImpl implements MutableDumpReader {

  private File zipFile = null;
  private File tempRootDir = null;

  @Override
  public void setZipFile(File file) {
    checkNotNull(file, "Project dump file can not be null");
    checkArgument(file.exists(), "Project dump file does not exist: %s", file);
    checkArgument(file.isFile(), "Project dump is not a file: %s", file);
    this.zipFile = file;
  }

  @Override
  public void setTempRootDir(File d) {
    checkNotNull(d, "Dump extraction directory can not be null");
    checkArgument(d.exists(), "Dump extraction directory does not exist: %s", d);
    checkArgument(d.isDirectory(), "Dump extraction is not a directory: %s", d);
    this.tempRootDir = d;
  }

  @Override
  public File zipFile() {
    checkState(zipFile != null, "Project dump file has not been set");
    return zipFile;
  }

  @Override
  public File tempRootDir() {
    checkState(tempRootDir != null, "Dump file has not been extracted");
    return tempRootDir;
  }

  @Override
  public ProjectDump.Metadata metadata() {
    File file = new File(tempRootDir(), METADATA.filename());
    checkState(file.exists(), "Missing metadata file: %s", file);
    try (FileInputStream input = FILES2.openInputStream(file)) {
      return PROTOBUF2.parseFrom(METADATA.parser(), input);
    } catch (IOException e) {
      throw new IllegalStateException("Can not read file " + file, e);
    }
  }

  @Override
  public <M extends Message> MessageStream<M> stream(DumpElement<M> elt) {
    File file = new File(tempRootDir(), elt.filename());
    checkState(file.exists(), "Missing file: %s", file);

    InputStream input = FILES2.openInputStream(file);
    return new MessageStreamImpl<>(input, elt.parser());
  }
}
