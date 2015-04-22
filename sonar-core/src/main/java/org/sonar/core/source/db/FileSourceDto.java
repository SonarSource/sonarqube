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
package org.sonar.core.source.db;

import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import org.apache.commons.io.IOUtils;
import org.sonar.server.source.db.FileSourceDb;
import org.sonar.server.source.db.FileSourceDb.Test;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FileSourceDto {

  private Long id;
  private String projectUuid;
  private String fileUuid;
  private long createdAt;
  private long updatedAt;
  private String lineHashes;
  private String srcHash;
  private byte[] binaryData;
  private String dataType;
  private String dataHash;

  public Long getId() {
    return id;
  }

  public FileSourceDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public FileSourceDto setProjectUuid(String projectUuid) {
    this.projectUuid = projectUuid;
    return this;
  }

  public String getFileUuid() {
    return fileUuid;
  }

  public FileSourceDto setFileUuid(String fileUuid) {
    this.fileUuid = fileUuid;
    return this;
  }

  @CheckForNull
  public String getDataHash() {
    return dataHash;
  }

  /**
   * MD5 of column BINARY_DATA. Used to know to detect data changes and need for update.
   */
  public FileSourceDto setDataHash(String s) {
    this.dataHash = s;
    return this;
  }

  public static FileSourceDb.Data decodeSourceData(byte[] binaryData) {
    // stream is always closed
    return decodeSourceData(new ByteArrayInputStream(binaryData));
  }

  /**
   * Decompress and deserialize content of column FILE_SOURCES.BINARY_DATA.
   * The parameter "input" is always closed by this method.
   */
  public static FileSourceDb.Data decodeSourceData(InputStream binaryInput) {
    LZ4BlockInputStream lz4Input = null;
    try {
      lz4Input = new LZ4BlockInputStream(binaryInput);
      return FileSourceDb.Data.parseFrom(lz4Input);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to decompress and deserialize source data", e);
    } finally {
      IOUtils.closeQuietly(lz4Input);
    }
  }

  /**
   * Serialize and compress protobuf message {@link org.sonar.server.source.db.FileSourceDb.Data}
   * in the column BINARY_DATA.
   */
  public static byte[] encodeSourceData(FileSourceDb.Data data) {
    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
    LZ4BlockOutputStream compressedOutput = new LZ4BlockOutputStream(byteOutput);
    try {
      data.writeTo(compressedOutput);
      compressedOutput.close();
      return byteOutput.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Fail to serialize and compress source data", e);
    } finally {
      IOUtils.closeQuietly(compressedOutput);
    }
  }

  public static List<Test> decodeTestData(byte[] binaryData) {
    // stream is always closed
    return decodeTestData(new ByteArrayInputStream(binaryData));
  }

  /**
   * Decompress and deserialize content of column FILE_SOURCES.BINARY_DATA.
   * The parameter "input" is always closed by this method.
   */
  public static List<Test> decodeTestData(InputStream binaryInput) {
    LZ4BlockInputStream lz4Input = null;
    List<Test> tests = new ArrayList<>();
    try {
      lz4Input = new LZ4BlockInputStream(binaryInput);

      Test currentTest;
      do {
        currentTest = Test.parseDelimitedFrom(lz4Input);
        if (currentTest != null) {
          tests.add(currentTest);
        }
      } while (currentTest != null);
      return tests;
    } catch (IOException e) {
      throw new IllegalStateException("Fail to decompress and deserialize source data", e);
    } finally {
      IOUtils.closeQuietly(lz4Input);
    }
  }

  /**
   * Serialize and compress protobuf message {@link org.sonar.server.source.db.FileSourceDb.Data}
   * in the column BINARY_DATA.
   */
  public static byte[] encodeTestData(List<Test> tests) {
    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
    LZ4BlockOutputStream compressedOutput = new LZ4BlockOutputStream(byteOutput);
    try {
      for (Test test : tests) {
        test.writeDelimitedTo(compressedOutput);
      }
      compressedOutput.close();
      return byteOutput.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Fail to serialize and compress source tests", e);
    } finally {
      IOUtils.closeQuietly(compressedOutput);
    }
  }

  /**
   * Compressed value of serialized protobuf message {@link org.sonar.server.source.db.FileSourceDb.Data}
   */
  public byte[] getBinaryData() {
    return binaryData;
  }

  /**
   * Set compressed value of the protobuf message {@link org.sonar.server.source.db.FileSourceDb.Data}
   */
  public FileSourceDto setBinaryData(byte[] data) {
    this.binaryData = data;
    return this;
  }

  /**
   * Compressed value of serialized protobuf message {@link org.sonar.server.source.db.FileSourceDb.Data}
   */
  public FileSourceDb.Data getSourceData() {
    return decodeSourceData(binaryData);
  }

  public FileSourceDto setSourceData(FileSourceDb.Data data) {
    this.dataType = Type.SOURCE;
    this.binaryData = encodeSourceData(data);
    return this;
  }

  /**
   * Compressed value of serialized protobuf message {@link org.sonar.server.source.db.FileSourceDb.Data}
   */
  public List<Test> getTestData() {
    return decodeTestData(binaryData);
  }

  public FileSourceDto setTestData(List<Test> data) {
    this.dataType = Type.TEST;
    this.binaryData = encodeTestData(data);
    return this;
  }

  @CheckForNull
  public String getLineHashes() {
    return lineHashes;
  }

  public FileSourceDto setLineHashes(@Nullable String lineHashes) {
    this.lineHashes = lineHashes;
    return this;
  }

  @CheckForNull
  public String getSrcHash() {
    return srcHash;
  }

  /**
   * Hash of file content. Value is computed by batch.
   */
  public FileSourceDto setSrcHash(@Nullable String srcHash) {
    this.srcHash = srcHash;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public FileSourceDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public FileSourceDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public String getDataType() {
    return dataType;
  }

  public FileSourceDto setDataType(String dataType) {
    this.dataType = dataType;
    return this;
  }

  public static class Type {
    public static final String SOURCE = "SOURCE";
    public static final String TEST = "TEST";
  }
}
