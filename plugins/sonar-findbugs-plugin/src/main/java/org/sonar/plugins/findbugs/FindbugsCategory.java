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
package org.sonar.plugins.findbugs;

import java.util.HashMap;
import java.util.Map;

public final class FindbugsCategory {
  private final static Map<String, String> FINDBUGS_TO_SONAR = new HashMap<String, String>();

  static {
    FINDBUGS_TO_SONAR.put("BAD_PRACTICE", "Bad practice");
    FINDBUGS_TO_SONAR.put("CORRECTNESS", "Correctness");
    FINDBUGS_TO_SONAR.put("MT_CORRECTNESS", "Multithreaded correctness");
    FINDBUGS_TO_SONAR.put("I18N", "Internationalization");
    FINDBUGS_TO_SONAR.put("EXPERIMENTAL", "Experimental");
    FINDBUGS_TO_SONAR.put("MALICIOUS_CODE", "Malicious code");
    FINDBUGS_TO_SONAR.put("PERFORMANCE", "Performance");
    FINDBUGS_TO_SONAR.put("SECURITY", "Security");
    FINDBUGS_TO_SONAR.put("STYLE", "Style");
  }

  public static String findbugsToSonar(String findbugsCategKey) {
    return FINDBUGS_TO_SONAR.get(findbugsCategKey);
  }

  private FindbugsCategory() {
  }
}
