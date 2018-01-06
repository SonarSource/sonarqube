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
package org.sonar.core.hash;

import java.security.MessageDigest;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Computes the hash of the source lines of a file by simply added lines of that file one by one in order with
 * {@link #addLine(String, boolean)}.
 */
public class SourceHashComputer {
  private final MessageDigest md5Digest = DigestUtils.getMd5Digest();

  public void addLine(String line, boolean hasNextLine) {
    String lineToHash = hasNextLine ? (line + '\n') : line;
    this.md5Digest.update(lineToHash.getBytes(UTF_8));
  }

  public String getHash() {
    return Hex.encodeHexString(md5Digest.digest());
  }
}
