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

package org.sonar.server.computation.design;

import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import org.apache.commons.io.IOUtils;
import org.sonar.server.design.db.DsmDb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class DsmDataEncoder {

  private DsmDataEncoder() {
    // Only static methods
  }

  /**
   * Serialize and compress protobuf message {@link org.sonar.server.design.db.DsmDb.Data
   */
  public static byte[] encodeSourceData(DsmDb.Data data) {
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

  public static DsmDb.Data decodeDsmData(byte[] binaryData) {
    // stream is always closed
    return decodeDsmData(new ByteArrayInputStream(binaryData));
  }

  /**
   * Decompress and deserialize DSM data
   */
  private static DsmDb.Data decodeDsmData(InputStream binaryInput) {
    LZ4BlockInputStream lz4Input = null;
    try {
      lz4Input = new LZ4BlockInputStream(binaryInput);
      return DsmDb.Data.parseFrom(lz4Input);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to decompress and deserialize dsm data", e);
    } finally {
      IOUtils.closeQuietly(lz4Input);
    }
  }
}
