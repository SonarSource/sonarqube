/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sonar.api.utils.TempFolder;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.projectexport.taskprocessor.ProjectDescriptor;
import org.sonar.ce.task.projectexport.util.ProjectExportDumpFS;

import static org.apache.commons.io.FileUtils.sizeOf;
import static org.sonar.ce.task.projectexport.steps.DumpElement.METADATA;
import static org.sonar.ce.task.util.Files2.FILES2;
import static org.sonar.ce.task.util.Protobuf2.PROTOBUF2;
import static org.sonar.core.util.FileUtils.humanReadableByteCountSI;

public class DumpWriterImpl implements DumpWriter {

  private final ProjectDescriptor descriptor;
  private final ProjectExportDumpFS projectExportDumpFS;
  private final TempFolder tempFolder;
  private final File rootDir;

  private final AtomicBoolean metadataWritten = new AtomicBoolean(false);
  private final AtomicBoolean published = new AtomicBoolean(false);

  public DumpWriterImpl(ProjectDescriptor descriptor, ProjectExportDumpFS projectExportDumpFS, TempFolder tempFolder) {
    this.descriptor = descriptor;
    this.projectExportDumpFS = projectExportDumpFS;
    this.tempFolder = tempFolder;
    this.rootDir = tempFolder.newDir();
  }

  @Override
  public void write(ProjectDump.Metadata metadata) {
    checkNotPublished();
    if (metadataWritten.get()) {
      throw new IllegalStateException("Metadata has already been written");
    }
    File file = new File(rootDir, METADATA.filename());
    try (FileOutputStream output = FILES2.openOutputStream(file, false)) {
      PROTOBUF2.writeTo(metadata, output);
      metadataWritten.set(true);
    } catch (IOException e) {
      throw new IllegalStateException("Can not write to file " + file, e);
    }
  }

  @Override
  public <M extends Message> StreamWriter<M> newStreamWriter(DumpElement<M> elt) {
    checkNotPublished();
    File file = new File(rootDir, elt.filename());
    return StreamWriterImpl.create(file);
  }

  @Override
  public void publish() {
    checkNotPublished();
    if (!metadataWritten.get()) {
      throw new IllegalStateException("Metadata is missing");
    }
    File zip = tempFolder.newFile();
    FILES2.zipDir(rootDir, zip);

    File targetZip = projectExportDumpFS.exportDumpOf(descriptor);
    FILES2.deleteIfExists(targetZip);
    FILES2.moveFile(zip, targetZip);
    FILES2.deleteIfExists(rootDir);
    LoggerFactory.getLogger(getClass()).info("Dump file published | size={} | path={}", humanReadableByteCountSI(sizeOf(targetZip)), targetZip.getAbsolutePath());
    published.set(true);
  }

  private void checkNotPublished() {
    if (published.get()) {
      throw new IllegalStateException("Dump is already published");
    }
  }
}
