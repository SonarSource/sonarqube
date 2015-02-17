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

import com.google.protobuf.CodedOutputStream;
import org.apache.commons.lang.RandomStringUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.sonar.server.source.db.FileSourceDb;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 * See https://code.google.com/p/xxhash/ and https://github.com/jpountz/lz4-java
 *
 */
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.Throughput)
public class FileSourceDbBenchmark {

  @Param({"10", "100", "1000", "100000"})
  public int linesNumber;

  List<FileSourceDb.Line> lines = new ArrayList<>();
  FileSourceDb.Data data;

  @Setup
  public void setup() throws Exception {
    FileSourceDb.Data.Builder builder = FileSourceDb.Data.newBuilder();
    for (int i = 0; i < linesNumber; i++) {
      FileSourceDb.Line.Builder lineBuilder = builder.addLinesBuilder();
      lines.add(lineBuilder
        .setLine(i + 1)
        .setScmAuthor("charlie")
        .setScmRevision("ABCDE")
        .setScmDate(15000000000L)
        .setUtLineHits(5)
        .setUtConditions(2)
        .setUtCoveredConditions(1)
        .setSource(RandomStringUtils.randomAlphanumeric(10))
        .setHighlighting(RandomStringUtils.randomAlphanumeric(20))
        .setSymbols(RandomStringUtils.randomAlphanumeric(20))
        .addAllDuplications(Arrays.asList(12,13,15))
        .build());
    }
    data = builder.build();
  }

  @Benchmark
  public int container() throws Exception {
    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
    data.writeTo(byteOutput);
    byteOutput.close();
    return byteOutput.toByteArray().length;
  }

  @Benchmark
  public int delimiters() throws Exception {
    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
    for (FileSourceDb.Line line : lines) {
      line.writeDelimitedTo(byteOutput);
    }
    byteOutput.close();
    return byteOutput.toByteArray().length;
  }

  @Benchmark
  public int codedstream_container() throws Exception {
    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
    CodedOutputStream writer = CodedOutputStream.newInstance(byteOutput);
    writer.writeRawVarint32(data.getSerializedSize());
    writer.writeRawBytes(data.toByteArray());
    writer.flush();
    byteOutput.close();
    return byteOutput.toByteArray().length;
  }

  @Benchmark
  public int codedstream_container_known_size() throws Exception {
    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream(data.getSerializedSize());
    CodedOutputStream writer = CodedOutputStream.newInstance(byteOutput);
    writer.writeRawVarint32(data.getSerializedSize());
    writer.writeRawBytes(data.toByteArray());
    writer.flush();
    byteOutput.close();
    return byteOutput.toByteArray().length;
  }

  @Benchmark
  public int codedstream_delimiters() throws Exception {
    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
    CodedOutputStream writer = CodedOutputStream.newInstance(byteOutput);
    for (FileSourceDb.Line line : lines) {
      writer.writeRawVarint32(line.getSerializedSize());
      writer.writeRawBytes(line.toByteArray());
    }
    writer.flush();
    byteOutput.close();
    return byteOutput.toByteArray().length;
  }

  /**
   * You can this benchmark with maven command-line (see run.sh) or by executing this method
   * in IDE
   */
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
      .include(FileSourceDbBenchmark.class.getSimpleName())
      .build();
    new Runner(opt).run();
  }
}
