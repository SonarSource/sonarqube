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
package org.sonar.duplications.detector.suffixtree;

import org.sonar.duplications.detector.suffixtree.AbstractText;
import org.sonar.duplications.detector.suffixtree.Text;

/**
 * Implementation of {@link Text} based on {@link String}.
 */
public class StringText extends AbstractText {

  public StringText(String text) {
    super(text.length());
    for (int i = 0; i < text.length(); i++) {
      symbols.add(Character.valueOf(text.charAt(i)));
    }
  }

}
