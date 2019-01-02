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
package org.sonar.api.batch.fs.internal.charhandler;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

public class FileHashComputer extends CharHandler {
  private static final char LINE_FEED = '\n';

  
  private MessageDigest globalMd5Digest = DigestUtils.getMd5Digest();
  private StringBuilder sb = new StringBuilder();
  private final CharsetEncoder encoder;
  private final String filePath;

  public FileHashComputer(String filePath) {
    encoder = StandardCharsets.UTF_8.newEncoder()
      .onMalformedInput(CodingErrorAction.REPLACE)
      .onUnmappableCharacter(CodingErrorAction.REPLACE);
    this.filePath = filePath;
  }

  @Override
  public void handleIgnoreEoL(char c) {
    sb.append(c);
  }

  @Override
  public void newLine() {
    sb.append(LINE_FEED);
    processBuffer();
    sb.setLength(0);
  }

  @Override
  public void eof() {
    if (sb.length() > 0) {
      processBuffer();
    }
  }

  private void processBuffer() {
    try {
      if (sb.length() > 0) {
        ByteBuffer encoded = encoder.encode(CharBuffer.wrap(sb));
        globalMd5Digest.update(encoded.array(), 0, encoded.limit());
      }
    } catch (CharacterCodingException e) {
      throw new IllegalStateException("Error encoding line hash in file: " + filePath, e);
    }
  }

  public String getHash() {
    return Hex.encodeHexString(globalMd5Digest.digest());
  }
}
