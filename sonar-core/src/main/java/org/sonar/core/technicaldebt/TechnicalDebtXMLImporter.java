/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.technicaldebt.batch.internal.DefaultCharacteristic;
import org.sonar.api.technicaldebt.batch.internal.DefaultRequirement;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.api.utils.WorkUnit;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class TechnicalDebtXMLImporter implements ServerExtension {

  private static final Logger LOG = LoggerFactory.getLogger(TechnicalDebtXMLImporter.class);

  public static final String CHARACTERISTIC = "chc";
  public static final String CHARACTERISTIC_KEY = "key";
  public static final String CHARACTERISTIC_NAME = "name";
  public static final String PROPERTY = "prop";

  public static final String PROPERTY_KEY = "key";
  public static final String PROPERTY_VALUE = "val";
  public static final String PROPERTY_TEXT_VALUE = "txt";

  public static final String REPOSITORY_KEY = "rule-repo";
  public static final String RULE_KEY = "rule-key";

  public static final String PROPERTY_FUNCTION = "remediationFunction";
  public static final String PROPERTY_FACTOR = "remediationFactor";
  public static final String PROPERTY_OFFSET = "offset";

  public DefaultTechnicalDebtModel importXML(String xml, ValidationMessages messages, TechnicalDebtRuleCache technicalDebtRuleCache) {
    return importXML(new StringReader(xml), messages, technicalDebtRuleCache);
  }

  public DefaultTechnicalDebtModel importXML(Reader xml, ValidationMessages messages, TechnicalDebtRuleCache repositoryCache) {
    DefaultTechnicalDebtModel model = new DefaultTechnicalDebtModel();
    try {
      SMInputFactory inputFactory = initStax();
      SMHierarchicCursor cursor = inputFactory.rootElementCursor(xml);

      // advance to <sqale>
      cursor.advance();
      SMInputCursor chcCursor = cursor.childElementCursor(CHARACTERISTIC);

      while (chcCursor.getNext() != null) {
        processCharacteristic(model, null, chcCursor, messages, repositoryCache);
      }

      cursor.getStreamReader().closeCompletely();

    } catch (XMLStreamException e) {
      LOG.error("XML is not valid", e);
      messages.addErrorText("XML is not valid: " + e.getMessage());
    }
    return model;
  }

  private SMInputFactory initStax() {
    XMLInputFactory xmlFactory = XMLInputFactory2.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    return new SMInputFactory(xmlFactory);
  }

  private DefaultCharacteristic processCharacteristic(DefaultTechnicalDebtModel model, DefaultCharacteristic parent, SMInputCursor chcCursor, ValidationMessages messages,
                                                      TechnicalDebtRuleCache technicalDebtRuleCache) throws XMLStreamException {
    DefaultCharacteristic characteristic = new DefaultCharacteristic();
    SMInputCursor cursor = chcCursor.childElementCursor();
    while (cursor.getNext() != null) {
      String node = cursor.getLocalName();
      if (StringUtils.equals(node, CHARACTERISTIC_KEY)) {
        characteristic.setKey(cursor.collectDescendantText().trim());
        // Attached to parent only if a key is existing, otherwise characteristic with empty key can be added.
        characteristic.setParent(parent);

      } else if (StringUtils.equals(node, CHARACTERISTIC_NAME)) {
        characteristic.setName(cursor.collectDescendantText().trim(), false);

        // <chc> can contain characteristics or requirements
      } else if (StringUtils.equals(node, CHARACTERISTIC)) {
        processCharacteristic(model, characteristic, cursor, messages, technicalDebtRuleCache);

      } else if (StringUtils.equals(node, REPOSITORY_KEY)) {
        DefaultRequirement requirement = processRequirement(model, cursor, messages, technicalDebtRuleCache);
        if (requirement != null) {
          if (parent.parent() == null) {
            messages.addWarningText("Requirement '" + requirement.ruleKey()  + "' is ignored because it's defined directly under a root characteristic.");
          } else {
            requirement.setCharacteristic(parent);
            requirement.setRootCharacteristic(parent.parent());
          }
        }
      }
    }

    if (StringUtils.isNotBlank(characteristic.key()) && characteristic.isRoot()) {
      characteristic.setOrder(model.rootCharacteristics().size() + 1);
      model.addRootCharacteristic(characteristic);
      return characteristic;
    }
    return null;
  }

  private DefaultRequirement processRequirement(DefaultTechnicalDebtModel model, SMInputCursor cursor, ValidationMessages messages, TechnicalDebtRuleCache technicalDebtRuleCache)
    throws XMLStreamException {

    DefaultRequirement requirement = new DefaultRequirement();
    String ruleRepositoryKey = cursor.collectDescendantText().trim();
    String ruleKey = null;
    Properties properties = new Properties();
    while (cursor.getNext() != null) {
      String node = cursor.getLocalName();
      if (StringUtils.equals(node, PROPERTY)) {
        properties.add(processProperty(requirement, cursor, messages));
      } else if (StringUtils.equals(node, RULE_KEY)) {
        ruleKey = cursor.collectDescendantText().trim();
      }
    }
    fillRule(requirement, ruleRepositoryKey, ruleKey, messages, technicalDebtRuleCache);
    if (requirement.ruleKey() == null) {
      return null;
    }
    return processFunctionsOnRequirement(requirement, properties, messages);
  }

  private void fillRule(DefaultRequirement requirement, String ruleRepositoryKey, String ruleKey, ValidationMessages messages,
                        TechnicalDebtRuleCache technicalDebtRuleCache) {
    if (StringUtils.isNotBlank(ruleRepositoryKey) && StringUtils.isNotBlank(ruleKey)) {
      Rule rule = technicalDebtRuleCache.getByRuleKey(RuleKey.of(ruleRepositoryKey, ruleKey));
      if (rule != null) {
        requirement.setRuleKey(RuleKey.of(ruleRepositoryKey, ruleKey));
      } else {
        messages.addWarningText("Rule not found: [repository=" + ruleRepositoryKey + ", key=" + ruleKey + "]");
      }
    }
  }

  private Property processProperty(DefaultRequirement requirement, SMInputCursor cursor, ValidationMessages messages) throws XMLStreamException {
    SMInputCursor c = cursor.childElementCursor();
    String key = null;
    Double value = null;
    String textValue = null;
    while (c.getNext() != null) {
      String node = c.getLocalName();
      if (StringUtils.equals(node, PROPERTY_KEY)) {
        key = c.collectDescendantText().trim();

      } else if (StringUtils.equals(node, PROPERTY_VALUE)) {
        String s = c.collectDescendantText().trim();
        try {
          value = NumberUtils.createDouble(s);
        } catch (NumberFormatException ex) {
          messages.addErrorText(String.format("Cannot import value '%s' for field %s - Expected a numeric value instead", s, key));
        }
      } else if (StringUtils.equals(node, PROPERTY_TEXT_VALUE)) {
        textValue = c.collectDescendantText().trim();
      }
    }
    return new Property(key, value, textValue);
  }

  private DefaultRequirement processFunctionsOnRequirement(DefaultRequirement requirement, Properties properties, ValidationMessages messages) {
    Property function = properties.function();
    Property factor = properties.factor();
    Property offset = properties.offset();

    if (function != null) {
      String functionKey = function.getTextValue();
      if ("linear_threshold".equals(functionKey)) {
        function.setTextValue(DefaultRequirement.FUNCTION_LINEAR);
        offset.setValue(0d);
        messages.addWarningText(String.format("Linear with threshold function is no more used, function of the requirement '%s' is replaced by linear.", requirement.ruleKey()));
      } else if ("constant_resource".equals(functionKey)) {
        messages.addWarningText(String.format("Constant/file function is no more used, requirements '%s' are ignored.", requirement.ruleKey()));
        return null;
      }

      requirement.setFunction(function.getTextValue());
      if (factor != null) {
        requirement.setFactor(WorkUnit.create(factor.getValue(), factor.getTextValue()));
      }
      if (offset != null) {
        requirement.setOffset(WorkUnit.create(offset.getValue(), offset.getTextValue()));
      }
      return requirement;
    }
    return null;
  }

  private class Properties {
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

  private class Property {
    String key;
    Double value;
    String textValue;

    private Property(String key, Double value, String textValue) {
      this.key = key;
      this.value = value;
      this.textValue = textValue;
    }

    private Property setValue(Double value) {
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

    private Double getValue() {
      return value;
    }

    private String getTextValue() {
      return textValue;
    }
  }
}
