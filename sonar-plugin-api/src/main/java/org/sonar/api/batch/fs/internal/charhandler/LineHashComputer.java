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
package org.sonar.api.batch.fs.internal.charhandler;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.apache.commons.codec.digest.DigestUtils;
import org.sonar.api.batch.fs.internal.FileMetadata.LineHashConsumer;

public class LineHashComputer extends CharHandler {
  private final MessageDigest lineMd5Digest = DigestUtils.getMd5Digest();
  private final CharsetEncoder encoder;
  private final StringBuilder sb = new StringBuilder();
  private final LineHashConsumer consumer;
  private final File file;
  private int line = 1;

  public LineHashComputer(LineHashConsumer consumer, File f) {
    this.consumer = consumer;
    this.file = f;
    this.encoder = StandardCharsets.UTF_8.newEncoder()
      .onMalformedInput(CodingErrorAction.REPLACE)
      .onUnmappableCharacter(CodingErrorAction.REPLACE);
  }

  @Override
  public void handleIgnoreEoL(char c) {
    if (!Character.isWhitespace(c)) {
      sb.append(c);
    }
  }

  @Override
  public void newLine() {
    processBuffer();
    sb.setLength(0);
    line++;
  }

  @Override
  public void eof() {
    if (this.line > 0) {
      processBuffer();
    }
  }

  private void processBuffer() {
    try {
      if (sb.length() > 0) {
        ByteBuffer encoded = encoder.encode(CharBuffer.wrap(sb));
        lineMd5Digest.update(encoded.array(), 0, encoded.limit());
        consumer.consume(line, lineMd5Digest.digest());
      }
    } catch (CharacterCodingException e) {
      throw new IllegalStateException("Error encoding line hash in file: " + file.getAbsolutePath(), e);
    }
  }
}
