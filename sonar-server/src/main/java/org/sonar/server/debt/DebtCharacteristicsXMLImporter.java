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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.ServerExtension;
import org.sonar.api.technicaldebt.batch.internal.DefaultCharacteristic;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class DebtCharacteristicsXMLImporter implements ServerExtension {

  public static final String CHARACTERISTIC = "chc";
  public static final String CHARACTERISTIC_KEY = "key";
  public static final String CHARACTERISTIC_NAME = "name";

  public DebtModel importXML(String xml) {
    return importXML(new StringReader(xml));
  }

  public DebtModel importXML(Reader xml) {
    DebtModel model = new DebtModel();
    try {
      SMInputFactory inputFactory = initStax();
      SMHierarchicCursor cursor = inputFactory.rootElementCursor(xml);

      // advance to <sqale>
      cursor.advance();
      SMInputCursor chcCursor = cursor.childElementCursor(CHARACTERISTIC);

      while (chcCursor.getNext() != null) {
        processCharacteristic(model, null, chcCursor);
      }

      cursor.getStreamReader().closeCompletely();

    } catch (XMLStreamException e) {
      throw new IllegalStateException("XML is not valid", e);
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

  @CheckForNull
  private DefaultCharacteristic processCharacteristic(DebtModel model, @Nullable DefaultCharacteristic parent, SMInputCursor chcCursor) throws XMLStreamException {
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
        processCharacteristic(model, characteristic, cursor);
      }
    }

    if (StringUtils.isNotBlank(characteristic.key()) && characteristic.isRoot()) {
      characteristic.setOrder(model.rootCharacteristics().size() + 1);
      model.addRootCharacteristic(characteristic);
      return characteristic;
    }
    return null;
  }

  static class DebtModel {

    private Collection<DefaultCharacteristic> rootCharacteristics;

    public DebtModel() {
      rootCharacteristics = newArrayList();
    }

    public DebtModel addRootCharacteristic(DefaultCharacteristic characteristic) {
      rootCharacteristics.add(characteristic);
      return this;
    }

    public List<DefaultCharacteristic> rootCharacteristics() {
      return newArrayList(Iterables.filter(rootCharacteristics, new Predicate<DefaultCharacteristic>() {
        @Override
        public boolean apply(DefaultCharacteristic input) {
          return input.isRoot();
        }
      }));
    }

    @CheckForNull
    public DefaultCharacteristic characteristicByKey(final String key) {
      return Iterables.find(characteristics(), new Predicate<DefaultCharacteristic>() {
        @Override
        public boolean apply(DefaultCharacteristic input) {
          return input.key().equals(key);
        }
      }, null);
    }

    public List<DefaultCharacteristic> characteristics() {
      List<DefaultCharacteristic> flatCharacteristics = newArrayList();
      for (DefaultCharacteristic rootCharacteristic : rootCharacteristics) {
        flatCharacteristics.add(rootCharacteristic);
        for (DefaultCharacteristic characteristic : rootCharacteristic.children()) {
          flatCharacteristics.add(characteristic);
        }
      }
      return flatCharacteristics;
    }

    public boolean isEmpty() {
      return rootCharacteristics.isEmpty();
    }

  }

}
