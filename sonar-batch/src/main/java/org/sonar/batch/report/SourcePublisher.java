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
package org.sonar.batch.report;

import com.google.common.base.Charsets;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.batch.index.BatchResource;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.protocol.output.BatchReportWriter;

import java.io.*;

public class SourcePublisher implements ReportPublisherStep {

  private final ResourceCache resourceCache;

  public SourcePublisher(ResourceCache resourceCache) {
    this.resourceCache = resourceCache;
  }

  @Override
  public void publish(BatchReportWriter writer) {
    for (final BatchResource resource : resourceCache.all()) {
      if (!resource.isFile()) {
        continue;
      }

      DefaultInputFile inputFile = (DefaultInputFile) resource.inputPath();
      File iofile = writer.getSourceFile(resource.batchId());
      int line = 0;
      try (FileOutputStream output = new FileOutputStream(iofile); BOMInputStream bomIn = new BOMInputStream(new FileInputStream(inputFile.file()),
        ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_32LE, ByteOrderMark.UTF_32BE);
        BufferedReader reader = new BufferedReader(new InputStreamReader(bomIn, inputFile.charset()))) {
        String lineStr = reader.readLine();
        while (lineStr != null) {
          IOUtils.write(lineStr, output, Charsets.UTF_8);
          line++;
          if (line < inputFile.lines()) {
            IOUtils.write("\n", output, Charsets.UTF_8);
          }
          lineStr = reader.readLine();
        }
      } catch (IOException e) {
        throw new IllegalStateException("Unable to store file source in the report", e);
      }
    }
  }
}
