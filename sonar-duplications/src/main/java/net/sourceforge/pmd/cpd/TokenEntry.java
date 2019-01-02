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
/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd.cpd;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 2.2
 * @deprecated since 5.5
 */
@Deprecated
public class TokenEntry implements Comparable<TokenEntry> {

  private static final Map<String, Integer> TOKENS = new HashMap<>();
  private static int tokenCount = 0;

  /**
   * Shared instance of end-of-file token.
   *
   * <p>Not intended to be used by clients - {@link #getEOF()} should be used instead.</p>
   */
  public static final TokenEntry EOF = new TokenEntry();

  private String tokenSrcID;
  private int beginLine;
  private int index;
  private int identifier;
  private int hashCode;

  private final String value;

  private TokenEntry() {
    this.identifier = 0;
    this.tokenSrcID = "EOFMarker";
    this.value = "";
  }

  /**
   * @param image string representation of token
   * @param tokenSrcID within Sonar Ecosystem - absolute path to file, otherwise current implementation of sonar-cpd-plugin will not work
   * @param beginLine number of line
   */
  public TokenEntry(String image, String tokenSrcID, int beginLine) {
    Integer i = TOKENS.get(image);
    if (i == null) {
      i = TOKENS.size() + 1;
      TOKENS.put(image, i);
    }
    this.identifier = i.intValue();
    this.tokenSrcID = tokenSrcID;
    this.beginLine = beginLine;
    this.index = tokenCount++;
    this.value = image;
  }

  /**
   * For internal use only.
   *
   * @since 2.14
   */
  public String getValue() {
    return value;
  }

  /**
   * End-of-file token.
   */
  public static TokenEntry getEOF() {
    tokenCount++;
    return EOF;
  }

  public static void clearImages() {
    TOKENS.clear();
    tokenCount = 0;
  }

  public String getTokenSrcID() {
    return tokenSrcID;
  }

  public int getBeginLine() {
    return beginLine;
  }

  public int getIdentifier() {
    return this.identifier;
  }

  public int getIndex() {
    return this.index;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  public void setHashCode(int hashCode) {
    this.hashCode = hashCode;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof TokenEntry)) {
      return false;
    }
    TokenEntry other = (TokenEntry) o;
    return other.hashCode == hashCode;
  }

  @Override
  public int compareTo(TokenEntry other) {
    return getIndex() - other.getIndex();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("TokenEntry{");
    sb.append("tokenSrcID='").append(tokenSrcID).append('\'');
    sb.append(", beginLine=").append(beginLine);
    sb.append(", index=").append(index);
    sb.append(", identifier=").append(identifier);
    sb.append(", hashCode=").append(hashCode);
    sb.append(", value='").append(value).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
