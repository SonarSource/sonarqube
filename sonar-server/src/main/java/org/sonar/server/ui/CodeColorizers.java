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
package org.sonar.server.ui;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerExtension;
import org.sonar.api.utils.Logs;
import org.sonar.api.web.CodeColorizerFormat;
import org.sonar.colorizer.CodeColorizer;
import org.sonar.colorizer.HtmlOptions;
import org.sonar.colorizer.Tokenizer;

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeColorizers implements ServerExtension {

  private Map<String, CodeColorizerFormat> formatPerLanguage;

  public CodeColorizers(List<CodeColorizerFormat> formats) {
    formatPerLanguage = new HashMap<String, CodeColorizerFormat>();
    for (CodeColorizerFormat format : formats) {
      formatPerLanguage.put(format.getLanguageKey(), format);
    }

    Logs.INFO.info("Code colorizer, supported languages: " + StringUtils.join(formatPerLanguage.keySet(), ","));
  }

  public String toHtml(String code, String language) {
    List<Tokenizer> tokenizers;
    CodeColorizerFormat format = formatPerLanguage.get(language);
    if (format == null) {
      tokenizers = Collections.emptyList();
    } else {
      tokenizers = format.getTokenizers();
    }
    return new CodeColorizer(tokenizers).toHtml(new StringReader(code), HtmlOptions.ONLY_SYNTAX);
  }
}
