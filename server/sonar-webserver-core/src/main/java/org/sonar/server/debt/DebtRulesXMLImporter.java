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
package org.sonar.server.debt;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.server.debt.DebtModelXMLExporter.RuleDebt;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static javax.xml.stream.XMLInputFactory.IS_COALESCING;
import static javax.xml.stream.XMLInputFactory.IS_NAMESPACE_AWARE;
import static javax.xml.stream.XMLInputFactory.IS_VALIDATING;
import static javax.xml.stream.XMLInputFactory.SUPPORT_DTD;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.sonar.api.server.debt.DebtRemediationFunction.Type.CONSTANT_ISSUE;
import static org.sonar.api.server.debt.DebtRemediationFunction.Type.LINEAR;
import static org.sonar.api.utils.Duration.MINUTE;
import static org.sonar.server.debt.DebtModelXMLExporter.CHARACTERISTIC;
import static org.sonar.server.debt.DebtModelXMLExporter.PROPERTY;
import static org.sonar.server.debt.DebtModelXMLExporter.PROPERTY_COEFFICIENT;
import static org.sonar.server.debt.DebtModelXMLExporter.PROPERTY_FUNCTION;
import static org.sonar.server.debt.DebtModelXMLExporter.PROPERTY_KEY;
import static org.sonar.server.debt.DebtModelXMLExporter.PROPERTY_OFFSET;
import static org.sonar.server.debt.DebtModelXMLExporter.PROPERTY_TEXT_VALUE;
import static org.sonar.server.debt.DebtModelXMLExporter.PROPERTY_VALUE;
import static org.sonar.server.debt.DebtModelXMLExporter.REPOSITORY_KEY;
import static org.sonar.server.debt.DebtModelXMLExporter.RULE_KEY;

/**
 * Import rules debt definitions from an XML
 */
@ServerSide
public class DebtRulesXMLImporter {

  public List<RuleDebt> importXML(String xml, ValidationMessages validationMessages) {
    return importXML(new StringReader(xml), validationMessages);
  }

  public List<RuleDebt> importXML(Reader xml, ValidationMessages validationMessages) {
    List<RuleDebt> ruleDebts = newArrayList();
    try {
      SMInputFactory inputFactory = initStax();
      SMHierarchicCursor cursor = inputFactory.rootElementCursor(xml);

      // advance to <sqale>
      cursor.advance();
      SMInputCursor rootCursor = cursor.childElementCursor(CHARACTERISTIC);
      while (rootCursor.getNext() != null) {
        process(ruleDebts, validationMessages, rootCursor);
      }

      cursor.getStreamReader().closeCompletely();
    } catch (XMLStreamException e) {
      throw new IllegalStateException("XML is not valid", e);
    }
    return ruleDebts;
  }

  private static SMInputFactory initStax() {
    XMLInputFactory xmlFactory = XMLInputFactory2.newInstance();
    xmlFactory.setProperty(IS_COALESCING, TRUE);
    xmlFactory.setProperty(IS_NAMESPACE_AWARE, FALSE);
    xmlFactory.setProperty(SUPPORT_DTD, FALSE);
    xmlFactory.setProperty(IS_VALIDATING, FALSE);
    return new SMInputFactory(xmlFactory);
  }

  private static void process(List<RuleDebt> ruleDebts,
    ValidationMessages validationMessages, SMInputCursor chcCursor) throws XMLStreamException {
    SMInputCursor cursor = chcCursor.childElementCursor();
    while (cursor.getNext() != null) {
      String node = cursor.getLocalName();
      if (StringUtils.equals(node, CHARACTERISTIC)) {
        process(ruleDebts, validationMessages, cursor);
      } else if (StringUtils.equals(node, REPOSITORY_KEY)) {
        RuleDebt ruleDebt = processRule(validationMessages, cursor);
        if (ruleDebt != null) {
          ruleDebts.add(ruleDebt);
        }
      }
    }
  }

  @CheckForNull
  private static RuleDebt processRule(ValidationMessages validationMessages, SMInputCursor cursor) throws XMLStreamException {
    String ruleRepositoryKey = cursor.collectDescendantText().trim();
    String ruleKey = null;
    Properties properties = new Properties();
    while (cursor.getNext() != null) {
      String node = cursor.getLocalName();
      if (StringUtils.equals(node, PROPERTY)) {
        properties.add(processProperty(validationMessages, cursor));
      } else if (StringUtils.equals(node, RULE_KEY)) {
        ruleKey = cursor.collectDescendantText().trim();
      }
    }
    if (isNotBlank(ruleRepositoryKey) && isNotBlank(ruleKey)) {
      return createRule(RuleKey.of(ruleRepositoryKey, ruleKey), properties, validationMessages);
    }
    return null;
  }

  private static Property processProperty(ValidationMessages validationMessages, SMInputCursor cursor) throws XMLStreamException {
    SMInputCursor c = cursor.childElementCursor();
    String key = null;
    int value = 0;
    String textValue = null;
    while (c.getNext() != null) {
      String node = c.getLocalName();
      if (StringUtils.equals(node, PROPERTY_KEY)) {
        key = c.collectDescendantText().trim();

      } else if (StringUtils.equals(node, PROPERTY_VALUE)) {
        String s = c.collectDescendantText().trim();
        try {
          Double valueDouble = NumberUtils.createDouble(s);
          value = valueDouble.intValue();
        } catch (NumberFormatException ex) {
          validationMessages.addErrorText(String.format("Cannot import value '%s' for field %s - Expected a numeric value instead", s, key));
        }
      } else if (StringUtils.equals(node, PROPERTY_TEXT_VALUE)) {
        textValue = c.collectDescendantText().trim();
        textValue = "mn".equals(textValue) ? MINUTE : textValue;
      }
    }
    return new Property(key, value, textValue);
  }

  @CheckForNull
  private static RuleDebt createRule(RuleKey ruleKey, Properties properties, ValidationMessages validationMessages) {
    Property function = properties.function();
    Property coefficientProperty = properties.coefficient();
    String coefficient = coefficientProperty == null ? null : coefficientProperty.toDuration();
    Property offsetProperty = properties.offset();
    String offset = offsetProperty == null ? null : offsetProperty.toDuration();
    if (function != null && (coefficient != null || offset != null)) {
      return createRuleDebt(ruleKey, function.getTextValue(), coefficient, offset, validationMessages);
    }
    return null;
  }

  @CheckForNull
  private static RuleDebt createRuleDebt(RuleKey ruleKey, String function, @Nullable String coefficient, @Nullable String offset, ValidationMessages validationMessages) {
    if ("constant_resource".equals(function)) {
      validationMessages.addWarningText(String.format("Constant/file function is no longer used, technical debt definitions on '%s' are ignored.", ruleKey));
      return null;
    }
    if ("linear_threshold".equals(function) && coefficient != null) {
      validationMessages.addWarningText(String.format("Linear with threshold function is no longer used, remediation function of '%s' is replaced by linear.", ruleKey));
      return createRuleDebt(ruleKey, LINEAR.name(), coefficient, null, validationMessages);
    }
    if (CONSTANT_ISSUE.name().equalsIgnoreCase(function) && coefficient != null && offset == null) {
      return createRuleDebt(ruleKey, CONSTANT_ISSUE.name(), null, coefficient, validationMessages);
    }
    return new RuleDebt().setRuleKey(ruleKey).setFunction(function.toUpperCase()).setCoefficient(coefficient).setOffset(offset);
  }

  private static class Properties {
    List<Property> list;

    public Properties() {
      this.list = newArrayList();
    }

    public Properties add(Property property) {
      this.list.add(property);
      return this;
    }

    public Property function() {
      return find(PROPERTY_FUNCTION);
    }

    public Property coefficient() {
      return find(PROPERTY_COEFFICIENT);
    }

    public Property offset() {
      return find(PROPERTY_OFFSET);
    }

    private Property find(String key) {
      return Iterables.find(list, new PropertyMatchKey(key), null);
    }
  }

  private static class Property {
    String key;
    int value;
    String textValue;

    private Property(String key, int value, String textValue) {
      this.key = key;
      this.value = value;
      this.textValue = textValue;
    }

    private String getKey() {
      return key;
    }

    private int getValue() {
      return value;
    }

    private String getTextValue() {
      return textValue;
    }

    @CheckForNull
    public String toDuration() {
      if (key != null && getValue() > 0) {
        String duration = Integer.toString(getValue());
        duration += !Strings.isNullOrEmpty(getTextValue()) ? getTextValue() : Duration.DAY;
        return duration;
      }
      return null;
    }
  }

  private static class PropertyMatchKey implements Predicate<Property> {
    private final String key;

    public PropertyMatchKey(String key) {
      this.key = key;
    }

    @Override
    public boolean apply(@Nonnull Property input) {
      return input.getKey().equals(key);
    }
  }

}
