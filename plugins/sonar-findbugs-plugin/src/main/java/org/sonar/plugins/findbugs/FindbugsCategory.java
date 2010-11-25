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
package org.sonar.plugins.findbugs;

import java.util.HashMap;
import java.util.Map;

public final class FindbugsCategory {
  private final static Map<String, String> findbugsToSonar = new HashMap<String, String>();

  static {
    findbugsToSonar.put("BAD_PRACTICE", "Bad practice");
    findbugsToSonar.put("CORRECTNESS", "Correctness");
    findbugsToSonar.put("MT_CORRECTNESS", "Multithreaded correctness");
    findbugsToSonar.put("I18N", "Internationalization");
    findbugsToSonar.put("EXPERIMENTAL", "Experimental");
    findbugsToSonar.put("MALICIOUS_CODE", "Malicious code");
    findbugsToSonar.put("PERFORMANCE", "Performance");
    findbugsToSonar.put("SECURITY", "Security");
    findbugsToSonar.put("STYLE", "Style");
  }

  public static String findbugsToSonar(String findbugsCategKey) {
    return findbugsToSonar.get(findbugsCategKey);
  }

  private FindbugsCategory() {
  }
}