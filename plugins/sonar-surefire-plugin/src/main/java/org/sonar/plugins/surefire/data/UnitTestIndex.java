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
package org.sonar.plugins.surefire.data;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.*;

/**
 * @since 2.8
 */
public class UnitTestIndex {

  private Map<String, UnitTestClassReport> indexByClassname;

  public UnitTestIndex() {
    this.indexByClassname = Maps.newHashMap();
  }

  public UnitTestClassReport index(String classname) {
    UnitTestClassReport classReport = indexByClassname.get(classname);
    if (classReport == null) {
      classReport = new UnitTestClassReport();
      indexByClassname.put(classname, classReport);
    }
    return classReport;
  }

  public UnitTestClassReport get(String classname) {
    return indexByClassname.get(classname);
  }

  public Set<String> getClassnames() {
    return Sets.newHashSet(indexByClassname.keySet());
  }

  public Map<String, UnitTestClassReport> getIndexByClassname() {
    return indexByClassname;
  }

  public int size() {
    return indexByClassname.size();
  }

  public UnitTestClassReport merge(String classname, String intoClassname) {
    UnitTestClassReport from = indexByClassname.get(classname);
    if (from!=null) {
      UnitTestClassReport to = index(intoClassname);
      to.add(from);
      indexByClassname.remove(classname);
      return to;
    }
    return null;
  }

  public void remove(String classname) {
    indexByClassname.remove(classname);
  }


}
