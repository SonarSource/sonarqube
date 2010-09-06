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
package org.sonar.api.checks.templates;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.rules.StandardRulesXmlParser;
import org.sonar.api.utils.SonarException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * EXPERIMENTAL - will be used in version 2.2
 * @since 2.1
 */
public class XmlCheckTemplateFactory {

  public List<CheckTemplate> parseXml(String xml) {
    InputStream input = null;
    try {
      input = IOUtils.toInputStream(xml, CharEncoding.UTF_8);
      return parse(input);

    } catch (IOException e) {
      throw new SonarException("Can't parse xml file", e);

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  public List<CheckTemplate> parse(Reader reader) {
    StandardRulesXmlParser parser = new StandardRulesXmlParser();
    List<Rule> rules = parser.parse(reader);
    return toCheckTemplates(rules);

  }

  public List<CheckTemplate> parse(InputStream input) {
    StandardRulesXmlParser parser = new StandardRulesXmlParser();
    List<Rule> rules = parser.parse(input);
    return toCheckTemplates(rules);

  }

  private List<CheckTemplate> toCheckTemplates(List<Rule> rules) {
    List<CheckTemplate> templates = new ArrayList<CheckTemplate>();
    if (rules != null) {
      for (Rule rule : rules) {
        DefaultCheckTemplate template = new DefaultCheckTemplate(rule.getKey());
        templates.add(template);

        template.setConfigKey(rule.getConfigKey());
        template.setDescription(rule.getDescription());
        template.setIsoCategory(rule.getRulesCategory().toIsoCategory());
        template.setPriority(rule.getPriority().toCheckPriority());
        template.setTitle(rule.getName());

        if (rule.getParams() != null) {
          for (RuleParam param : rule.getParams()) {
            template.addProperty(toProperty(param));
          }
        }
      }
    }
    return templates;
  }

  private CheckTemplateProperty toProperty(RuleParam param) {
    DefaultCheckTemplateProperty property = new DefaultCheckTemplateProperty();
    property.setKey(param.getKey());
    property.setTitle(param.getKey());
    property.setDescription(param.getDescription());
    return property;
  }

}