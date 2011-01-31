/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import org.sonar.duplications.cpd.Match;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Tokens {

    private List<TokenEntry> tokens = new ArrayList<TokenEntry>();

    public void add(TokenEntry tokenEntry) {
        this.tokens.add(tokenEntry);
    }

    public Iterator<TokenEntry> iterator() {
        return tokens.iterator();
    }

    private TokenEntry get(int index) {
        return tokens.get(index);
    }

    public int size() {
        return tokens.size();
    }

    public int getLineCount(TokenEntry mark, Match match) {
        TokenEntry endTok = get(mark.getIndex() + match.getTokenCount() - 1);
        if (endTok == TokenEntry.EOF) {
            endTok = get(mark.getIndex() + match.getTokenCount() - 2);
        }
        return endTok.getBeginLine() - mark.getBeginLine() + 1;
    }

    public List<TokenEntry> getTokens() {
        return tokens;
    }

}
