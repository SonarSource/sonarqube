/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd.cpd;

import java.util.HashMap;
import java.util.Map;

public class TokenEntry implements Comparable<TokenEntry> {

    public static final TokenEntry EOF = new TokenEntry();

    private String tokenSrcID;
    private int beginLine;
    private int index;
    private int identifier;
    private int hashCode;

    private final static Map<String, Integer> Tokens = new HashMap<String, Integer>();
    private static int TokenCount = 0;

    private TokenEntry() {
        this.identifier = 0;
        this.tokenSrcID = "EOFMarker";
    }

    public TokenEntry(String image, String tokenSrcID, int beginLine) {
        Integer i = Tokens.get(image);
        if (i == null) {
            i = Tokens.size() + 1;
            Tokens.put(image, i);
        }
        this.identifier = i.intValue();
        this.tokenSrcID = tokenSrcID;
        this.beginLine = beginLine;
        this.index = TokenCount++;
    }

    public static TokenEntry getEOF() {
        TokenCount++;
        return EOF;
    }

    public static void clearImages() {
        Tokens.clear();
        TokenCount = 0;
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

    public int hashCode() {
        return hashCode;
    }

    public void setHashCode(int hashCode) {
        this.hashCode = hashCode;
    }

    public boolean equals(Object o) {
        if (!(o instanceof TokenEntry)) {
            return false;
        }
        TokenEntry other = (TokenEntry) o;
        return other.hashCode == hashCode;
    }

    public int compareTo(TokenEntry other) {
        return getIndex() - other.getIndex();
    }
}