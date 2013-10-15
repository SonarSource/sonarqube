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

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerExtension;
import org.sonar.api.qualitymodel.Characteristic;
import org.sonar.api.qualitymodel.Model;
import org.sonar.api.rules.Rule;
import org.sonar.api.utils.ValidationMessages;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

public class TechnicalDebtXMLImporter implements ServerExtension {

  private static final Logger LOG = LoggerFactory.getLogger(TechnicalDebtXMLImporter.class);

  private static final String CHARACTERISTIC = "chc";
  private static final String CHARACTERISTIC_KEY = "key";
  private static final String CHARACTERISTIC_NAME = "name";
  private static final String CHARACTERISTIC_DESCRIPTION = "desc";
  private static final String PROPERTY = "prop";
  private static final String PROPERTY_KEY = "key";
  private static final String PROPERTY_VALUE = "val";
  private static final String PROPERTY_TEXT_VALUE = "txt";

  public Model importXML(String xml, ValidationMessages messages, TechnicalDebtRuleCache technicalDebtRuleCache) {
    return importXML(new StringReader(xml), messages, technicalDebtRuleCache);
  }

  public Model importXML(Reader xml, ValidationMessages messages, TechnicalDebtRuleCache repositoryCache) {
    Model model = Model.createByName(TechnicalDebtModel.MODEL_NAME);
    try {
      SMInputFactory inputFactory = initStax();
      SMHierarchicCursor cursor = inputFactory.rootElementCursor(xml);

      // advance to <sqale>
      cursor.advance();
      SMInputCursor chcCursor = cursor.childElementCursor(CHARACTERISTIC);

      while (chcCursor.getNext() != null) {
        processCharacteristic(model, chcCursor, messages, repositoryCache);
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

  private Characteristic processCharacteristic(Model model, SMInputCursor chcCursor, ValidationMessages messages, TechnicalDebtRuleCache technicalDebtRuleCache)
    throws XMLStreamException {
    Characteristic characteristic = Characteristic.create();
    SMInputCursor cursor = chcCursor.childElementCursor();

    String ruleRepositoryKey = null, ruleKey = null;
    List<Characteristic> children = Lists.newArrayList();
    while (cursor.getNext() != null) {
      String node = cursor.getLocalName();
      if (StringUtils.equals(node, CHARACTERISTIC_KEY)) {
        characteristic.setKey(cursor.collectDescendantText().trim());

      } else if (StringUtils.equals(node, CHARACTERISTIC_NAME)) {
        characteristic.setName(cursor.collectDescendantText().trim(), false);

      } else if (StringUtils.equals(node, CHARACTERISTIC_DESCRIPTION)) {
        characteristic.setDescription(cursor.collectDescendantText().trim());

      } else if (StringUtils.equals(node, PROPERTY)) {
        processProperty(characteristic, cursor, messages);

      } else if (StringUtils.equals(node, CHARACTERISTIC)) {
        children.add(processCharacteristic(model, cursor, messages, technicalDebtRuleCache));

      } else if (StringUtils.equals(node, "rule-repo")) {
        ruleRepositoryKey = cursor.collectDescendantText().trim();

      } else if (StringUtils.equals(node, "rule-key")) {
        ruleKey = cursor.collectDescendantText().trim();
      }
    }
    fillRule(characteristic, ruleRepositoryKey, ruleKey, messages, technicalDebtRuleCache);

    if (StringUtils.isNotBlank(characteristic.getKey()) || characteristic.getRule() != null) {
      addCharacteristicToModel(model, characteristic, children);
      return characteristic;
    }
    return null;
  }

  private void fillRule(Characteristic characteristic, String ruleRepositoryKey, String ruleKey, ValidationMessages messages,
                        TechnicalDebtRuleCache technicalDebtRuleCache) {
    if (StringUtils.isNotBlank(ruleRepositoryKey) && StringUtils.isNotBlank(ruleKey)) {
      Rule rule = technicalDebtRuleCache.getRule(ruleRepositoryKey, ruleKey);
      if (rule != null) {
        characteristic.setRule(rule);
      } else {
        messages.addWarningText("Rule not found: [repository=" + ruleRepositoryKey + ", key=" + ruleKey + "]");
      }
    }
  }

  private void addCharacteristicToModel(Model model, Characteristic characteristic, List<Characteristic> children) {
    model.addCharacteristic(characteristic);
    for (Characteristic child : children) {
      if (child != null) {
        model.addCharacteristic(child);
        characteristic.addChild(child);
      }
    }
  }

  private void processProperty(Characteristic characteristic, SMInputCursor cursor, ValidationMessages messages) throws XMLStreamException {
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
          String message = String.format("Cannot import value '%s' for field %s - Expected a numeric value instead", s, key);
          LOG.error(message, ex);
          messages.addErrorText(message);
        }
      } else if (StringUtils.equals(node, PROPERTY_TEXT_VALUE)) {
        textValue = c.collectDescendantText().trim();
      }
    }
    if (StringUtils.isNotBlank(key)) {
      characteristic.setProperty(key, textValue).setValue(value);
    }
  }
}
