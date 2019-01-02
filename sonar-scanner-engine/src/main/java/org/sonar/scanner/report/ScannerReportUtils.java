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
package org.sonar.scanner.report;

import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.scanner.protocol.output.ScannerReport.SyntaxHighlightingRule.HighlightingType;

public class ScannerReportUtils {

  private ScannerReportUtils() {
  }

  public static HighlightingType toProtocolType(TypeOfText textType) {
    switch (textType) {
      case ANNOTATION:
        return HighlightingType.ANNOTATION;
      case COMMENT:
        return HighlightingType.COMMENT;
      case CONSTANT:
        return HighlightingType.CONSTANT;
      case CPP_DOC:
        return HighlightingType.CPP_DOC;
      case KEYWORD:
        return HighlightingType.KEYWORD;
      case KEYWORD_LIGHT:
        return HighlightingType.KEYWORD_LIGHT;
      case PREPROCESS_DIRECTIVE:
        return HighlightingType.PREPROCESS_DIRECTIVE;
      case STRING:
        return HighlightingType.HIGHLIGHTING_STRING;
      case STRUCTURED_COMMENT:
        return HighlightingType.STRUCTURED_COMMENT;
      default:
        throw new IllegalArgumentException("Unknow highlighting type: " + textType);
    }
  }

  public static TypeOfText toBatchType(HighlightingType type) {
    switch (type) {
      case ANNOTATION:
        return TypeOfText.ANNOTATION;
      case COMMENT:
        return TypeOfText.COMMENT;
      case CONSTANT:
        return TypeOfText.CONSTANT;
      case CPP_DOC:
        return TypeOfText.CPP_DOC;
      case HIGHLIGHTING_STRING:
        return TypeOfText.STRING;
      case KEYWORD:
        return TypeOfText.KEYWORD;
      case KEYWORD_LIGHT:
        return TypeOfText.KEYWORD_LIGHT;
      case PREPROCESS_DIRECTIVE:
        return TypeOfText.PREPROCESS_DIRECTIVE;
      case STRUCTURED_COMMENT:
        return TypeOfText.STRUCTURED_COMMENT;
      default:
        throw new IllegalArgumentException(type + " is not a valid type");
    }
  }

  public static String toCssClass(HighlightingType type) {
    return toBatchType(type).cssClass();
  }
}
