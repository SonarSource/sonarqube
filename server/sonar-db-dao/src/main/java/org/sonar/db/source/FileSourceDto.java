/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import org.apache.commons.io.IOUtils;
import org.sonar.db.protobuf.DbFileSources;

import static com.google.common.base.Splitter.on;
import static java.lang.String.format;

public class FileSourceDto {

  private static final String SIZE_LIMIT_EXCEEDED_EXCEPTION_MESSAGE = "Protocol message was too large.  May be malicious.  " +
    "Use CodedInputStream.setSizeLimit() to increase the size limit.";
  private static final Joiner LINE_RETURN_JOINER = Joiner.on('\n');
  public static final Splitter LINES_HASHES_SPLITTER = on('\n');
  public static final int LINE_COUNT_NOT_POPULATED = -1;

  private Long id;
  private String projectUuid;
  private String fileUuid;
  private long createdAt;
  private long updatedAt;
  private String lineHashes;
  /**
   * When {@code line_count} column has been added, it's been populated with value {@link #LINE_COUNT_NOT_POPULATED -1},
   * which implies all existing files sources have this value at the time SonarQube is upgraded.
   * <p>
   * Column {@code line_count} is populated with the correct value from every new files and for existing files as the
   * project they belong to is analyzed for the first time after the migration.
   * <p>
   * Method {@link #getLineCount()} hides this migration-only-related complexity by either returning the value
   * of column {@code line_count} when its been populated, or computed the returned value from the value of column
   * {@code line_hashes}.
   */
  private int lineCount = LINE_COUNT_NOT_POPULATED;
  private String srcHash;
  private byte[] binaryData = new byte[0];
  private String dataHash;
  private String revision;
  @Nullable
  private Integer lineHashesVersion;

  public int getLineHashesVersion() {
    return lineHashesVersion != null ? lineHashesVersion : LineHashVersion.WITHOUT_SIGNIFICANT_CODE.getDbValue();
  }

  public FileSourceDto setLineHashesVersion(int lineHashesVersion) {
    this.lineHashesVersion = lineHashesVersion;
    return this;
  }

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
   * Decompressed value of serialized protobuf message {@link org.sonar.db.protobuf.DbFileSources.Data}
   */
  public DbFileSources.Data getSourceData() {
    return decodeSourceData(binaryData);
  }

  public FileSourceDto setSourceData(DbFileSources.Data data) {
    this.binaryData = encodeSourceData(data);
    return this;
  }

  /** Used by MyBatis */
  public String getRawLineHashes() {
    return lineHashes;
  }

  public void setRawLineHashes(@Nullable String lineHashes) {
    this.lineHashes = lineHashes;
  }

  public List<String> getLineHashes() {
    if (lineHashes == null) {
      return Collections.emptyList();
    }
    return LINES_HASHES_SPLITTER.splitToList(lineHashes);
  }

  /**
   * @return the value of column {@code line_count} if populated, otherwise the size of {@link #getLineHashes()}.
   */
  public int getLineCount() {
    if (lineCount == LINE_COUNT_NOT_POPULATED) {
      return getLineHashes().size();
    }
    return lineCount;
  }

  public FileSourceDto setLineHashes(@Nullable List<String> lineHashes) {
    if (lineHashes == null) {
      this.lineHashes = null;
      this.lineCount = 0;
    } else if (lineHashes.isEmpty()) {
      this.lineHashes = null;
      this.lineCount = 1;
    } else {
      this.lineHashes = LINE_RETURN_JOINER.join(lineHashes);
      this.lineCount = lineHashes.size();
    }
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

  public String getRevision() {
    return revision;
  }

  public FileSourceDto setRevision(@Nullable String revision) {
    this.revision = revision;
    return this;
  }

}
