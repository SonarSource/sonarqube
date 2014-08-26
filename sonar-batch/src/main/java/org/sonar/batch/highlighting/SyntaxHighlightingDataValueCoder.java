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
package org.sonar.batch.highlighting;

import com.persistit.Value;
import com.persistit.encoding.CoderContext;
import com.persistit.encoding.ValueCoder;

import java.util.ArrayList;

public class SyntaxHighlightingDataValueCoder implements ValueCoder {

  private SyntaxHighlightingRuleValueCoder rulesCoder = new SyntaxHighlightingRuleValueCoder();

  @Override
  public void put(Value value, Object object, CoderContext context) {
    SyntaxHighlightingData data = (SyntaxHighlightingData) object;
    value.put(data.syntaxHighlightingRuleSet().size());
    for (SyntaxHighlightingRule rule : data.syntaxHighlightingRuleSet()) {
      rulesCoder.put(value, rule, context);
    }
  }

  @Override
  public Object get(Value value, Class clazz, CoderContext context) {
    int count = value.getInt();
    ArrayList<SyntaxHighlightingRule> rules = new ArrayList<SyntaxHighlightingRule>(count);
    for (int i = 0; i < count; i++) {
      rules.add((SyntaxHighlightingRule) rulesCoder.get(value, SyntaxHighlightingRule.class, context));
    }
    return new SyntaxHighlightingData(rules);
  }
}
