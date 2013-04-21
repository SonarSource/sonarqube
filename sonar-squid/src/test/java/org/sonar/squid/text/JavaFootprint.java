/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.squid.text;

import org.sonar.squid.recognizer.*;

import java.util.HashSet;
import java.util.Set;

public class JavaFootprint implements LanguageFootprint {

  private final Set<Detector> detectors = new HashSet<Detector>();

  public JavaFootprint() {
    detectors.add(new EndWithDetector(0.95, '}', ';', '{')); // NOSONAR Magic number is suitable in that case
    detectors.add(new KeywordsDetector(0.7, "||", "&&")); // NOSONAR
    detectors.add(new KeywordsDetector(0.3, "public", "abstract", "class", "implements", "extends", "return","throw",// NOSONAR
        "private", "protected", "enum", "continue", "assert", "package", "synchronized", "boolean", "this", "double", "instanceof",
        "final", "interface", "static", "void", "long", "int", "float", "super", "true", "case:"));
    detectors.add(new ContainsDetector(0.95, "++", "for(", "if(", "while(", "catch(", "switch(", "try{", "else{"));// NOSONAR
    detectors.add(new CamelCaseDetector(0.5));// NOSONAR
  }

  public Set<Detector> getDetectors() {
    return detectors;
  }
}
