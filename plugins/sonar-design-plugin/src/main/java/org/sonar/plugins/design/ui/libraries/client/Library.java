/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.plugins.design.ui.libraries.client;

import com.google.gwt.user.client.ui.TreeItem;
import org.sonar.gwt.ui.Icons;
import org.sonar.wsclient.services.DependencyTree;

public class Library extends TreeItem {

  private String keywords;
  private String usage;

  public Library(DependencyTree dep) {
    setHTML(toHTML(dep));
    keywords = toKeywords(dep);
    usage = dep.getUsage();
  }

  private static String toKeywords(DependencyTree dep) {
    String text = dep.getResourceName() + " ";

    if (dep.getResourceKey() != null) {
      text += dep.getResourceKey() + " ";
    }
    if (dep.getResourceVersion() != null) {
      text += dep.getResourceVersion() + " ";
    }
    text += dep.getUsage();
    return text.toUpperCase();
  }

  /**
   * @param keyword upper-case keyword
   */
  public boolean containsKeyword(String keyword) {
    if (keywords.indexOf(keyword) >= 0) {
      return true;
    }
    for (int index = 0; index < getChildCount(); index++) {
      if (((Library) getChild(index)).containsKeyword(keyword)) {
        return true;
      }
    }
    return false;
  }

  private static String toHTML(DependencyTree tree) {
    String html = Icons.forQualifier(tree.getResourceQualifier()).getHTML();
    html += " <span> " + tree.getResourceName() + "</span> ";

    if (tree.getResourceVersion() != null) {
      html += tree.getResourceVersion() + " ";
    }
    html += "(" + tree.getUsage() + ")";
    return html;
  }

  public boolean filter(String keyword, boolean testFiltered) {
    if (testFiltered && "test".equals(usage)) {
      setVisible(false);
      return true;
    }

    boolean filtered = false;
    if (!"".equals(keyword) && !containsKeyword(keyword)) {
      filtered = true;
    }

    boolean hasVisibleChild = false;
    for (int index = 0; index < getChildCount(); index++) {
      hasVisibleChild |= !((Library) getChild(index)).filter(keyword, testFiltered);
    }

    boolean visible = !filtered || hasVisibleChild;
    setVisible(visible);
    return !visible;
  }
}
