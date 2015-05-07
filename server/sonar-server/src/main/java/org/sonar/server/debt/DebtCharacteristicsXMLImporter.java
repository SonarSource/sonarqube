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

package org.sonar.server.debt;

import org.apache.commons.lang.StringUtils;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.ServerSide;
import org.sonar.api.server.debt.internal.DefaultDebtCharacteristic;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.server.debt.DebtModelXMLExporter.DebtModel;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import java.io.Reader;
import java.io.StringReader;

import static org.sonar.server.debt.DebtModelXMLExporter.CHARACTERISTIC;
import static org.sonar.server.debt.DebtModelXMLExporter.CHARACTERISTIC_KEY;
import static org.sonar.server.debt.DebtModelXMLExporter.CHARACTERISTIC_NAME;

/**
 * Import characteristics from an xml
 */
@ServerSide
public class DebtCharacteristicsXMLImporter {

  public DebtModel importXML(String xml) {
    return importXML(new StringReader(xml));
  }

  public DebtModel importXML(Reader xml) {
    DebtModel debtModel = new DebtModel();
    try {
      SMInputFactory inputFactory = initStax();
      SMHierarchicCursor cursor = inputFactory.rootElementCursor(xml);

      // advance to <sqale>
      cursor.advance();
      SMInputCursor chcCursor = cursor.childElementCursor(CHARACTERISTIC);

      while (chcCursor.getNext() != null) {
        process(debtModel, null, chcCursor);
      }

      cursor.getStreamReader().closeCompletely();

    } catch (XMLStreamException e) {
      throw new IllegalStateException("XML is not valid", e);
    }
    return debtModel;
  }

  private SMInputFactory initStax() {
    XMLInputFactory xmlFactory = XMLInputFactory2.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    return new SMInputFactory(xmlFactory);
  }

  @CheckForNull
  private void process(DebtModel debtModel, @Nullable String parent, SMInputCursor chcCursor) throws XMLStreamException {
    DefaultDebtCharacteristic characteristic = new DefaultDebtCharacteristic();
    SMInputCursor cursor = chcCursor.childElementCursor();
    while (cursor.getNext() != null) {
      String node = cursor.getLocalName();
      if (StringUtils.equals(node, CHARACTERISTIC_KEY)) {
        characteristic.setKey(convertKey(cursor.collectDescendantText().trim()));
        if (parent == null) {
          characteristic.setOrder(debtModel.rootCharacteristics().size() + 1);
          debtModel.addRootCharacteristic(characteristic);
        } else {
          debtModel.addSubCharacteristic(characteristic, parent);
        }

      } else if (StringUtils.equals(node, CHARACTERISTIC_NAME)) {
        characteristic.setName(cursor.collectDescendantText().trim());

        // <chc> can contain characteristics or requirements
      } else if (StringUtils.equals(node, CHARACTERISTIC)) {
        process(debtModel, characteristic.key(), cursor);
      }
    }
  }

  static String convertKey(String key) {
    if ("NETWORK_USE_EFFICIENCY".equals(key)) {
      return RulesDefinition.SubCharacteristics.NETWORK_USE;
    }
    return key;
  }

}
