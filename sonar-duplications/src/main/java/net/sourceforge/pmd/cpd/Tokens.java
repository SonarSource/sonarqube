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

/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd.cpd;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>Not intended to be instantiated by clients.</p>
 *
 * @since 2.2
 */
public class Tokens {

  private List<TokenEntry> entries = new ArrayList<>();

  public void add(TokenEntry tokenEntry) {
    this.entries.add(tokenEntry);
  }

  public Iterator<TokenEntry> iterator() {
    return entries.iterator();
  }

  public int size() {
    return entries.size();
  }

  public List<TokenEntry> getTokens() {
    return entries;
  }

}
