/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.source;

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

/**
 * Central point for sonar-colorizer extensions
 */
public class CodeColorizers implements ServerExtension {

  private final Map<String, CodeColorizerFormat> byLang;

  public CodeColorizers(List<CodeColorizerFormat> formats) {
    byLang = new HashMap<String, CodeColorizerFormat>();
    for (CodeColorizerFormat format : formats) {
      byLang.put(format.getLanguageKey(), format);
    }

    Logs.INFO.info("Code colorizer, supported languages: " + StringUtils.join(byLang.keySet(), ","));
  }

  public String toHtml(String code, String language) {
    CodeColorizerFormat format = byLang.get(language);
    List<Tokenizer> tokenizers;
    if (format == null) {
      tokenizers = Collections.emptyList();
    } else {
      tokenizers = format.getTokenizers();
    }
    return new CodeColorizer(tokenizers).toHtml(new StringReader(code), HtmlOptions.ONLY_SYNTAX);
  }
}
