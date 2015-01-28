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
package org.sonar.batch.issue.tracking;

import com.google.common.base.Charsets;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Collection;

/**
 * Wraps a {@link Sequence} to assign hash codes to elements.
 */
public final class FileHashes {

  private final String[] hashes;
  private final Multimap<String, Integer> linesByHash;

  private FileHashes(String[] hashes, Multimap<String, Integer> linesByHash) {
    this.hashes = hashes;
    this.linesByHash = linesByHash;
  }

  public static FileHashes create(String[] hashes) {
    int size = hashes.length;
    Multimap<String, Integer> linesByHash = LinkedHashMultimap.create();
    for (int i = 0; i < size; i++) {
      // indices in array are shifted one line before
      linesByHash.put(hashes[i], i + 1);
    }
    return new FileHashes(hashes, linesByHash);
  }

  public static FileHashes create(DefaultInputFile f) {
    byte[][] hashes = new byte[f.lines()][];
    try {
      BufferedReader reader = Files.newBufferedReader(f.path(), f.charset());
      MessageDigest lineMd5Digest = DigestUtils.getMd5Digest();
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < f.lines(); i++) {
        String lineStr = reader.readLine();
        if (lineStr != null) {
          for (int j = 0; j < lineStr.length(); j++) {
            char c = lineStr.charAt(j);
            if (!Character.isWhitespace(c)) {
              sb.append(c);
            }
          }
        }
        hashes[i] = sb.length() > 0 ? lineMd5Digest.digest(sb.toString().getBytes(Charsets.UTF_8)) : null;
        sb.setLength(0);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Unable to compute line hashes of file " + f, e);
    }

    int size = hashes.length;
    Multimap<String, Integer> linesByHash = LinkedHashMultimap.create();
    String[] hexHashes = new String[size];
    for (int i = 0; i < size; i++) {
      String hash = hashes[i] != null ? Hex.encodeHexString(hashes[i]) : "";
      hexHashes[i] = hash;
      // indices in array are shifted one line before
      linesByHash.put(hash, i + 1);
    }
    return new FileHashes(hexHashes, linesByHash);
  }

  public int length() {
    return hashes.length;
  }

  public Collection<Integer> getLinesForHash(String hash) {
    return linesByHash.get(hash);
  }

  public String getHash(int line) {
    // indices in array are shifted one line before
    return (String) ObjectUtils.defaultIfNull(hashes[line - 1], "");
  }
}
