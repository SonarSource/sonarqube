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

import com.google.common.collect.ImmutableList;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

/**
 * Computes the hash of each line of a given file by simply added lines of that file one by one in order with
 * {@link #addLine(String)}.
 */
public class SourceLinesHashesComputer {
  private final MessageDigest md5Digest = DigestUtils.getMd5Digest();
  private final List<String> lineHashes;

  public SourceLinesHashesComputer() {
    this.lineHashes = new ArrayList<>();
  }

  public SourceLinesHashesComputer(int expectedLineCount) {
    this.lineHashes = new ArrayList<>(expectedLineCount);
  }

  public void addLine(String line) {
    requireNonNull(line, "line can not be null");
    lineHashes.add(computeHash(line));
  }

  public List<String> getLineHashes() {
    return ImmutableList.copyOf(lineHashes);
  }

  private String computeHash(String line) {
    String reducedLine = StringUtils.replaceChars(line, "\t ", "");
    if (reducedLine.isEmpty()) {
      return "";
    }
    return Hex.encodeHexString(md5Digest.digest(reducedLine.getBytes(UTF_8)));
  }
}
