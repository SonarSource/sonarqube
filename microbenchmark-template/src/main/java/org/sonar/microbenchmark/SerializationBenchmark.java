/*
 * markdown-benchmark
 * Copyright (C) 2009 ${owner}
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.microbenchmark;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchOutput;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.Externalizable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@State(Scope.Thread)
public class SerializationBenchmark {

  File outputFile;
  private final Gson gson = new GsonBuilder().create();

  @Setup
  public void setup() throws Exception {
    outputFile = File.createTempFile("microbenchmark", ".out");
  }

  @Benchmark
  public void write_gson() throws Exception {
    JsonWriter writer = new JsonWriter(new BufferedWriter(new FileWriter(outputFile, false)));
    writer.beginArray();
    for (int i = 0; i < 10000; i++) {
      Issue issue = new Issue();
      issue.uuid = "UUID_" + i;
      issue.severity = "BLOCKER";
      issue.message = "this is the message of issue " + i;
      issue.line = i;
      issue.author = "someone";
      issue.tags = Arrays.asList("tag" + i, "othertag" + i);
      gson.toJson(issue, Issue.class, writer);
    }
    writer.endArray();
    writer.close();
  }

  @Benchmark
  public void write_protobuf() throws Exception {
    // Write stream of objects with delimiter
    // An alternative can be http://stackoverflow.com/a/21870564/229031
    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile, false))) {
      for (int i = 0; i < 10000; i++) {
        BatchOutput.ReportIssue.Builder issueBuilder = BatchOutput.ReportIssue.newBuilder();
        issueBuilder.setUuid("UUID_" + i);
        issueBuilder.setSeverity(Constants.Severity.BLOCKER);
        issueBuilder.setMsg("this is the message of issue " + i);
        issueBuilder.setLine(i);
        issueBuilder.setAuthorLogin("someone");
        issueBuilder.addAllTags(Arrays.asList("tag" + i, "othertag" + i));
        issueBuilder.build().writeDelimitedTo(out);
      }
    }
  }

  @Benchmark
  public void write_serializable() throws Exception {
    try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile, false)))) {
      for (int i = 0; i < 10000; i++) {
        Issue issue = new Issue();
        issue.uuid = "UUID_" + i;
        issue.severity = "BLOCKER";
        issue.message = "this is the message of issue " + i;
        issue.line = i;
        issue.author = "someone";
        issue.tags = Arrays.asList("tag" + i, "othertag" + i);
        out.writeObject(issue);
      }
    }
  }

  @Benchmark
  public void write_externalizable() throws Exception {
    try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile, false)))) {
      for (int i = 0; i < 10000; i++) {
        ExternalizableIssue issue = new ExternalizableIssue();
        issue.uuid = "UUID_" + i;
        issue.severity = "BLOCKER";
        issue.message = "this is the message of issue " + i;
        issue.line = i;
        issue.author = "someone";
        issue.tags = Arrays.asList("tag" + i, "othertag" + i);
        out.writeObject(issue);
      }
    }
  }

  @Benchmark
  public void write_kryo() throws Exception {
    Kryo kryo = new Kryo();
    OutputStream stream = new BufferedOutputStream(new FileOutputStream(outputFile, false));
    Output output = new Output(stream);
    for (int i = 0; i < 10000; i++) {
      Issue issue = new Issue();
      issue.uuid = "UUID_" + i;
      issue.severity = "BLOCKER";
      issue.message = "this is the message of issue " + i;
      issue.line = i;
      issue.author = "someone";
      issue.tags = Arrays.asList("tag" + i, "othertag" + i);
      kryo.writeObject(output, issue);
    }
    output.close();
  }

  public static class Issue implements Serializable {
    String uuid, severity, message, author;
    int line;
    List<String> tags;
  }

  public static class ExternalizableIssue implements Externalizable {
    String uuid, severity, message, author;
    int line;
    List<String> tags;

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
      out.writeUTF(uuid);
      out.writeUTF(severity);
      out.writeUTF(message);
      out.writeUTF(author);
      out.writeInt(line);

      // write number of tags then tags
      out.writeByte(tags.size());
      for (String tag : tags) {
        out.writeUTF(tag);
      }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * You can this benchmark with maven command-line (see run.sh) or by executing this method
   * in IDE
   */
  public static void main(String[] args) throws RunnerException {
    Options opt = new OptionsBuilder()
      .include(SerializationBenchmark.class.getSimpleName())
      .warmupIterations(5)
      .measurementIterations(5)
      .forks(5)
      .build();
    new Runner(opt).run();
  }
}
