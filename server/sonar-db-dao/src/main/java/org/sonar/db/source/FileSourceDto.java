/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.db.source;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import org.apache.commons.io.IOUtils;
import org.sonar.db.protobuf.DbFileSources;

import static java.lang.String.format;

public class FileSourceDto {

  private static final String SIZE_LIMIT_EXCEEDED_EXCEPTION_MESSAGE = "Protocol message was too large.  May be malicious.  " +
    "Use CodedInputStream.setSizeLimit() to increase the size limit.";

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
  private String revision;

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

  public DbFileSources.Data decodeSourceData(byte[] binaryData) {
    try {
      return decodeRegularSourceData(binaryData);
    } catch (IOException e) {
      throw new IllegalStateException(
        format("Fail to decompress and deserialize source data [id=%s,fileUuid=%s,projectUuid=%s]", id, fileUuid, projectUuid),
        e);
    }
  }

  private static DbFileSources.Data decodeRegularSourceData(byte[] binaryData) throws IOException {
    try (LZ4BlockInputStream lz4Input = new LZ4BlockInputStream(new ByteArrayInputStream(binaryData))) {
      return DbFileSources.Data.parseFrom(lz4Input);
    } catch (InvalidProtocolBufferException e) {
      if (SIZE_LIMIT_EXCEEDED_EXCEPTION_MESSAGE.equals(e.getMessage())) {
        return decodeHugeSourceData(binaryData);
      }
      throw e;
    }
  }

  private static DbFileSources.Data decodeHugeSourceData(byte[] binaryData) throws IOException {
    try (LZ4BlockInputStream lz4Input = new LZ4BlockInputStream(new ByteArrayInputStream(binaryData))) {
      CodedInputStream input = CodedInputStream.newInstance(lz4Input);
      input.setSizeLimit(Integer.MAX_VALUE);
      return DbFileSources.Data.parseFrom(input);
    }
  }

  /**
   * Serialize and compress protobuf message {@link org.sonar.db.protobuf.DbFileSources.Data}
   * in the column BINARY_DATA.
   */
  public static byte[] encodeSourceData(DbFileSources.Data data) {
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

  public static List<DbFileSources.Test> decodeTestData(byte[] binaryData) {
    // stream is always closed
    return decodeTestData(new ByteArrayInputStream(binaryData));
  }

  /**
   * Decompress and deserialize content of column FILE_SOURCES.BINARY_DATA.
   * The parameter "input" is always closed by this method.
   */
  public static List<DbFileSources.Test> decodeTestData(InputStream binaryInput) {
    LZ4BlockInputStream lz4Input = null;
    List<DbFileSources.Test> tests = new ArrayList<>();
    try {
      lz4Input = new LZ4BlockInputStream(binaryInput);

      DbFileSources.Test currentTest;
      do {
        currentTest = DbFileSources.Test.parseDelimitedFrom(lz4Input);
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
   * Serialize and compress protobuf message {@link org.sonar.db.protobuf.DbFileSources.Data}
   * in the column BINARY_DATA.
   */
  public static byte[] encodeTestData(List<DbFileSources.Test> tests) {
    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
    LZ4BlockOutputStream compressedOutput = new LZ4BlockOutputStream(byteOutput);
    try {
      for (DbFileSources.Test test : tests) {
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
   * Compressed value of serialized protobuf message {@link org.sonar.db.protobuf.DbFileSources.Data}
   */
  public byte[] getBinaryData() {
    return binaryData;
  }

  /**
   * Set compressed value of the protobuf message {@link org.sonar.db.protobuf.DbFileSources.Data}
   */
  public FileSourceDto setBinaryData(byte[] data) {
    this.binaryData = data;
    return this;
  }

  /**
   * Compressed value of serialized protobuf message {@link org.sonar.db.protobuf.DbFileSources.Data}
   */
  public DbFileSources.Data getSourceData() {
    return decodeSourceData(binaryData);
  }

  public FileSourceDto setSourceData(DbFileSources.Data data) {
    this.dataType = Type.SOURCE;
    this.binaryData = encodeSourceData(data);
    return this;
  }

  /**
   * Compressed value of serialized protobuf message {@link org.sonar.db.protobuf.DbFileSources.Data}
   */
  public List<DbFileSources.Test> getTestData() {
    return decodeTestData(binaryData);
  }

  public FileSourceDto setTestData(List<DbFileSources.Test> data) {
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

  public String getRevision() {
    return revision;
  }

  public FileSourceDto setRevision(@Nullable String revision) {
    this.revision = revision;
    return this;
  }

  public static class Type {
    public static final String SOURCE = "SOURCE";
    public static final String TEST = "TEST";

    private Type() {
      // utility class
    }
  }
}
