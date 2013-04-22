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

package org.sonar.squid.recognizer;

import java.util.ArrayList;
import java.util.List;

public class CodeRecognizer {

  private LanguageFootprint language;
  private double            threshold;

  public CodeRecognizer(double threshold, LanguageFootprint language) {
    this.language = language;
    this.threshold = threshold;
  }

  public final double recognition(String line) {
    double probability = 0;
    for (Detector pattern : language.getDetectors()) {
      probability = 1 - ((1 - probability) * (1 - pattern.recognition(line)));
    }
    return probability;
  }

  public final List<String> extractCodeLines(List<String> lines) {
    ArrayList<String> codeLines = new ArrayList<String>();
    for (String line : lines) {
      if (recognition(line) >= threshold) {
        codeLines.add(line);
      }
    }
    return codeLines;
  }

  public final boolean isLineOfCode(String line) {
    return recognition(line) - threshold > 0;
  }
}
