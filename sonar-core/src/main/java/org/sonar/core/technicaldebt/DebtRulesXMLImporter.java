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

package org.sonar.core.technicaldebt;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerExtension;
import org.sonar.api.rule.RemediationFunction;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Duration;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class DebtRulesXMLImporter implements ServerExtension {

  private static final Logger LOG = LoggerFactory.getLogger(DebtRulesXMLImporter.class);

  public static final String CHARACTERISTIC = "chc";
  public static final String CHARACTERISTIC_KEY = "key";
  public static final String PROPERTY = "prop";

  public static final String PROPERTY_KEY = "key";
  public static final String PROPERTY_VALUE = "val";
  public static final String PROPERTY_TEXT_VALUE = "txt";

  public static final String REPOSITORY_KEY = "rule-repo";
  public static final String RULE_KEY = "rule-key";

  public static final String PROPERTY_FUNCTION = "remediationFunction";
  public static final String PROPERTY_FACTOR = "remediationFactor";
  public static final String PROPERTY_OFFSET = "offset";

  public List<RuleDebt> importXML(String xml) {
    return importXML(new StringReader(xml));
  }

  public List<RuleDebt> importXML(Reader xml) {
    List<RuleDebt> ruleDebts = newArrayList();
    try {
      SMInputFactory inputFactory = initStax();
      SMHierarchicCursor cursor = inputFactory.rootElementCursor(xml);

      // advance to <sqale>
      cursor.advance();
      SMInputCursor rootCursor = cursor.childElementCursor(CHARACTERISTIC);
      while (rootCursor.getNext() != null) {
        processCharacteristic(ruleDebts, null, null, rootCursor);
      }

      cursor.getStreamReader().closeCompletely();
    } catch (XMLStreamException e) {
      LOG.error("XML is not valid", e);
    }

    return ruleDebts;
  }

  private SMInputFactory initStax() {
    XMLInputFactory xmlFactory = XMLInputFactory2.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    return new SMInputFactory(xmlFactory);
  }

  private void processCharacteristic(List<RuleDebt> ruleDebts, @Nullable String rootKey, @Nullable String parentKey, SMInputCursor chcCursor) throws XMLStreamException {
    String currentCharacteristicKey = null;
    SMInputCursor cursor = chcCursor.childElementCursor();
    while (cursor.getNext() != null) {
      String node = cursor.getLocalName();
      if (StringUtils.equals(node, CHARACTERISTIC_KEY)) {
        currentCharacteristicKey = cursor.collectDescendantText().trim();
      } else if (StringUtils.equals(node, CHARACTERISTIC)) {
        processCharacteristic(ruleDebts, parentKey, currentCharacteristicKey, cursor);
      } else if (StringUtils.equals(node, REPOSITORY_KEY)) {
        RuleDebt ruleDebt = processRule(cursor);
        if (ruleDebt != null) {
          if (rootKey != null) {
            ruleDebt.characteristicKey = parentKey;
            ruleDebts.add(ruleDebt);
          } else {
            LOG.warn("Rule '" + ruleDebt.ruleKey + "' is ignored because it's defined directly under a root characteristic.");
          }
        }
      }
    }
  }

  private RuleDebt processRule(SMInputCursor cursor)
    throws XMLStreamException {

    RuleDebt ruleDebt = new RuleDebt();
    String ruleRepositoryKey = cursor.collectDescendantText().trim();
    String ruleKey = null;
    Properties properties = new Properties();
    while (cursor.getNext() != null) {
      String node = cursor.getLocalName();
      if (StringUtils.equals(node, PROPERTY)) {
        properties.add(processProperty(cursor));
      } else if (StringUtils.equals(node, RULE_KEY)) {
        ruleKey = cursor.collectDescendantText().trim();
      }
    }
    if (StringUtils.isNotBlank(ruleRepositoryKey) && StringUtils.isNotBlank(ruleKey)) {
      ruleDebt.ruleKey = RuleKey.of(ruleRepositoryKey, ruleKey);
    } else {
      return null;
    }
    return processFunctionsOnRequirement(ruleDebt, properties);
  }

  private Property processProperty(SMInputCursor cursor) throws XMLStreamException {
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
          LOG.error(String.format("Cannot import value '%s' for field %s - Expected a numeric value instead", s, key));
        }
      } else if (StringUtils.equals(node, PROPERTY_TEXT_VALUE)) {
        textValue = c.collectDescendantText().trim();
      }
    }
    return new Property(key, value, textValue);
  }

  @CheckForNull
  private RuleDebt processFunctionsOnRequirement(RuleDebt requirement, Properties properties) {
    Property function = properties.function();
    Property factor = properties.factor();
    Property offset = properties.offset();

    if (function != null) {
      // Init with default values
      requirement.factor = "0" + Duration.DAY;
      requirement.offset = "0" + Duration.DAY;

      String functionKey = function.getTextValue();
      if ("linear_threshold".equals(functionKey)) {
        function.setTextValue(RemediationFunction.LINEAR.name().toLowerCase());
        offset.setValue(0);
        offset.setTextValue(Duration.DAY);
        LOG.warn(String.format("Linear with threshold function is no longer used, remediation function of '%s' is replaced by linear.", requirement.ruleKey));
      } else if ("constant_resource".equals(functionKey)) {
        LOG.warn(String.format("Constant/file function is no longer used, technical debt definitions on '%s' are ignored.", requirement.ruleKey));
        return null;
      }

      requirement.function = RemediationFunction.valueOf(function.getTextValue().toUpperCase());
      if (factor != null) {
        requirement.factor = Integer.toString(factor.getValue());
        requirement.factor += !Strings.isNullOrEmpty(factor.getTextValue()) ? factor.getTextValue() : Duration.DAY;
      }
      if (offset != null) {
        requirement.offset = Integer.toString(offset.getValue());
        requirement.offset += !Strings.isNullOrEmpty(offset.getTextValue()) ? offset.getTextValue() : Duration.DAY;
      }
      return requirement;
    }
    return null;
  }

  private static class Properties {
    List<Property> properties;

    public Properties() {
      this.properties = newArrayList();
    }

    public Properties add(Property property) {
      this.properties.add(property);
      return this;
    }

    public Property function() {
      return find(PROPERTY_FUNCTION);
    }

    public Property factor() {
      return find(PROPERTY_FACTOR);
    }

    public Property offset() {
      return find(PROPERTY_OFFSET);
    }

    private Property find(final String key) {
      return Iterables.find(properties, new Predicate<Property>() {
        @Override
        public boolean apply(Property input) {
          return input.getKey().equals(key);
        }
      }, null);
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

    private Property setValue(int value) {
      this.value = value;
      return this;
    }

    private Property setTextValue(String textValue) {
      this.textValue = textValue;
      return this;
    }

    private String getKey() {
      return key;
    }

    private int getValue() {
      return value;
    }

    private String getTextValue() {
      return "mn".equals(textValue) ? Duration.MINUTE : textValue;
    }
  }

  public static class RuleDebt {
    private RuleKey ruleKey;
    private String characteristicKey;
    private RemediationFunction function;
    private String factor;
    private String offset;

    public RuleKey ruleKey() {
      return ruleKey;
    }

    public RuleDebt setRuleKey(RuleKey ruleKey) {
      this.ruleKey = ruleKey;
      return this;
    }

    public String characteristicKey() {
      return characteristicKey;
    }

    public RuleDebt setCharacteristicKey(String characteristicKey) {
      this.characteristicKey = characteristicKey;
      return this;
    }

    public RemediationFunction function() {
      return function;
    }

    public RuleDebt setFunction(RemediationFunction function) {
      this.function = function;
      return this;
    }

    public String factor() {
      return factor;
    }

    public RuleDebt setFactor(String factor) {
      this.factor = factor;
      return this;
    }

    public String offset() {
      return offset;
    }

    public RuleDebt setOffset(String offset) {
      this.offset = offset;
      return this;
    }
  }

}
