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
package org.sonar.microbenchmark;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;

public class SerializationBenchmarkTest {

  SerializationBenchmark benchmark = new SerializationBenchmark();

  @Before
  public void setUp() throws Exception {
    benchmark.setup();
  }

  @Test
  public void size_of_gson_output() throws Exception {
    benchmark.write_gson();
    System.out.println("GSON: " + sizeOf(benchmark.outputFile));
    System.out.println("GSON (zipped): " + sizeOf(zipFile(benchmark.outputFile)));
  }

  @Test
  public void size_of_protobuf_output() throws Exception {
    benchmark.write_protobuf();
    System.out.println("Protocol Buffers: " + sizeOf(benchmark.outputFile));
    System.out.println("Protocol Buffers (zipped): " + sizeOf(zipFile(benchmark.outputFile)));
  }

  @Test
  public void size_of_serializable_output() throws Exception {
    benchmark.write_serializable();
    System.out.println("java.io.Serializable: " + sizeOf(benchmark.outputFile));
    System.out.println("java.io.Serializable (zipped): " + sizeOf(zipFile(benchmark.outputFile)));
  }

  @Test
  public void size_of_externalizable_output() throws Exception {
    benchmark.write_externalizable();
    System.out.println("java.io.Externalizable: " + sizeOf(benchmark.outputFile));
    System.out.println("java.io.Externalizable (zipped): " + sizeOf(zipFile(benchmark.outputFile)));
  }

  @Test
  public void size_of_kryo_output() throws Exception {
    benchmark.write_kryo();
    System.out.println("Kryo: " + sizeOf(benchmark.outputFile));
    System.out.println("Kryo (zipped): " + sizeOf(zipFile(benchmark.outputFile)));
  }

  private String sizeOf(File file) {
    return FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(file));
  }

  private File zipFile(File input) throws Exception {
    File zipFile = new File(input.getAbsolutePath() + ".zip");
    Deflater deflater = new Deflater();
    byte[] content = FileUtils.readFileToByteArray(input);
    deflater.setInput(content);
    try (OutputStream outputStream = new FileOutputStream(zipFile)) {
      deflater.finish();
      byte[] buffer = new byte[1024];
      while (!deflater.finished()) {
        int count = deflater.deflate(buffer); // returns the generated code... index
        outputStream.write(buffer, 0, count);
      }
    }
    deflater.end();

    return zipFile;
  }
}
