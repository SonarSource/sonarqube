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
package org.sonar.api.batch.sensor.highlighting.internal;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;

public class SyntaxHighlightingRule {

  private final TextRange range;
  private final TypeOfText textType;

  private SyntaxHighlightingRule(TextRange range, TypeOfText textType) {
    this.range = range;
    this.textType = textType;
  }

  public static SyntaxHighlightingRule create(TextRange range, TypeOfText textType) {
    return new SyntaxHighlightingRule(range, textType);
  }

  public TextRange range() {
    return range;
  }

  public TypeOfText getTextType() {
    return textType;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.SIMPLE_STYLE);
  }
}
