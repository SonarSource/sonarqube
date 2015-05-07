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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerSide;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.api.server.debt.internal.DefaultDebtCharacteristic;
import org.xml.sax.InputSource;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Export characteristics and rule debt definitions to XML
 */
@ServerSide
public class DebtModelXMLExporter {

  private static final String ROOT = "sqale";
  private static final String DEFAULT_INDENT = "2";

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
  public static final String PROPERTY_COEFFICIENT = "remediationFactor";
  public static final String PROPERTY_OFFSET = "offset";

  protected String export(DebtModel debtModel, List<RuleDebt> allRules) {
    StringBuilder sb = new StringBuilder();
    sb.append("<" + ROOT + ">");
    for (DebtCharacteristic characteristic : debtModel.rootCharacteristics()) {
      processCharacteristic(debtModel, characteristic, allRules, sb);
    }
    sb.append("</" + ROOT + ">");
    String xml = sb.toString();
    xml = prettyFormatXml(xml);
    return xml;
  }

  private void processCharacteristic(DebtModel debtModel, DebtCharacteristic characteristic, List<RuleDebt> allRules, StringBuilder xml) {
    xml.append("<" + CHARACTERISTIC + ">");
    if (StringUtils.isNotBlank(characteristic.key())) {
      xml.append("<" + CHARACTERISTIC_KEY + ">");
      xml.append(StringEscapeUtils.escapeXml(characteristic.key()));
      xml.append("</" + CHARACTERISTIC_KEY + "><" + CHARACTERISTIC_NAME + ">");
      xml.append(StringEscapeUtils.escapeXml(characteristic.name()));
      xml.append("</" + CHARACTERISTIC_NAME + ">");
    }

    if (characteristic.isSub()) {
      List<RuleDebt> rules = rules(allRules, characteristic.key());
      for (RuleDebt ruleDto : rules) {
        processRule(ruleDto, xml);
      }
    } else {
      for (DebtCharacteristic child : debtModel.subCharacteristics(characteristic.key())) {
        processCharacteristic(debtModel, child, allRules, xml);
      }
    }
    xml.append("</" + CHARACTERISTIC + ">");
  }

  private void processRule(RuleDebt rule, StringBuilder xml) {
    xml.append("<" + CHARACTERISTIC + ">");
    xml.append("<" + REPOSITORY_KEY + ">");
    xml.append(StringEscapeUtils.escapeXml(rule.ruleKey().repository()));
    xml.append("</" + REPOSITORY_KEY + "><" + RULE_KEY + ">");
    xml.append(StringEscapeUtils.escapeXml(rule.ruleKey().rule()));
    xml.append("</" + RULE_KEY + ">");

    String coefficient = rule.coefficient();
    String offset = rule.offset();

    processProperty(PROPERTY_FUNCTION, null, rule.function(), xml);
    if (coefficient != null) {
      String[] values = getValues(coefficient);
      processProperty(PROPERTY_COEFFICIENT, values[0], values[1], xml);
    }
    if (offset != null) {
      String[] values = getValues(offset);
      processProperty(PROPERTY_OFFSET, values[0], values[1], xml);
    }
    xml.append("</" + CHARACTERISTIC + ">");
  }

  private static String[] getValues(String factorOrOffset) {
    String[] result = new String[2];
    Pattern pattern = Pattern.compile("(\\d+)(\\w+)");
    Matcher matcher = pattern.matcher(factorOrOffset);
    if (matcher.find()) {
      String value = matcher.group(1);
      String unit = matcher.group(2);
      result[0] = value;
      result[1] = unit;
    }
    return result;
  }

  private void processProperty(String key, @Nullable String val, String text, StringBuilder xml) {
    xml.append("<" + PROPERTY + "><" + PROPERTY_KEY + ">");
    xml.append(StringEscapeUtils.escapeXml(key));
    xml.append("</" + PROPERTY_KEY + ">");
    if (val != null) {
      xml.append("<" + PROPERTY_VALUE + ">");
      xml.append(val);
      xml.append("</" + PROPERTY_VALUE + ">");
    }
    if (StringUtils.isNotEmpty(text)) {
      xml.append("<" + PROPERTY_TEXT_VALUE + ">");
      xml.append(StringEscapeUtils.escapeXml(text));
      xml.append("</" + PROPERTY_TEXT_VALUE + ">");
    }
    xml.append("</" + PROPERTY + ">");
  }

  private String prettyFormatXml(String xml) {
    try {
      Transformer serializer = SAXTransformerFactory.newInstance().newTransformer();
      serializer.setOutputProperty(OutputKeys.INDENT, "yes");
      serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", DEFAULT_INDENT);
      Source xmlSource = new SAXSource(new InputSource(new ByteArrayInputStream(xml.getBytes(Charsets.UTF_8))));
      StreamResult res = new StreamResult(new ByteArrayOutputStream());
      serializer.transform(xmlSource, res);
      return new String(((ByteArrayOutputStream) res.getOutputStream()).toByteArray(), Charsets.UTF_8);
    } catch (TransformerException ignored) {
      // Ignore, raw XML will be returned
    }
    return xml;
  }

  private List<RuleDebt> rules(List<RuleDebt> rules, final String parentKey) {
    return newArrayList(Iterables.filter(rules, new Predicate<RuleDebt>() {
      @Override
      public boolean apply(RuleDebt input) {
        return parentKey.equals(input.subCharacteristicKey());
      }
    }));
  }

  public static class DebtModel {

    private Multimap<String, DebtCharacteristic> characteristicsByKey;

    public DebtModel() {
      characteristicsByKey = ArrayListMultimap.create();
    }

    public DebtModel addRootCharacteristic(DebtCharacteristic characteristic) {
      characteristicsByKey.put(null, characteristic);
      return this;
    }

    public DebtModel addSubCharacteristic(DebtCharacteristic subCharacteristic, String characteristicKey) {
      characteristicsByKey.put(characteristicKey, subCharacteristic);
      return this;
    }

    /**
     * @return root characteristics sorted by order
     */
    public List<DebtCharacteristic> rootCharacteristics() {
      return sortByOrder(newArrayList(characteristicsByKey.get(null)));
    }

    /**
     * @return root characteristics sorted by name
     */
    public List<DebtCharacteristic> subCharacteristics(String characteristicKey) {
      return sortByName(newArrayList(characteristicsByKey.get(characteristicKey)));
    }

    @CheckForNull
    public DebtCharacteristic characteristicByKey(final String key) {
      return Iterables.find(characteristicsByKey.values(), new Predicate<DebtCharacteristic>() {
        @Override
        public boolean apply(DebtCharacteristic input) {
          return key.equals(input.key());
        }
      }, null);
    }

    public DebtCharacteristic characteristicById(final Integer id) {
      return Iterables.find(characteristicsByKey.values(), new Predicate<DebtCharacteristic>() {
        @Override
        public boolean apply(DebtCharacteristic input) {
          return id.equals(((DefaultDebtCharacteristic) input).id());
        }
      });
    }

    private List<DebtCharacteristic> sortByOrder(List<DebtCharacteristic> characteristics) {
      Collections.sort(characteristics, new Ordering<DebtCharacteristic>() {
        @Override
        public int compare(@Nullable DebtCharacteristic left, @Nullable DebtCharacteristic right) {
          if (left == null || left.order() == null || right == null || right.order() == null) {
            return -1;
          }
          return left.order() - right.order();
        }
      });
      return characteristics;
    }

    private List<DebtCharacteristic> sortByName(List<DebtCharacteristic> characteristics) {
      Collections.sort(characteristics, new Ordering<DebtCharacteristic>() {
        @Override
        public int compare(@Nullable DebtCharacteristic left, @Nullable DebtCharacteristic right) {
          if (left == null || right == null) {
            return -1;
          }
          return StringUtils.defaultString(left.name()).compareTo(StringUtils.defaultString(right.name()));
        }
      });
      return characteristics;
    }
  }

  public static class RuleDebt {
    private RuleKey ruleKey;
    private String subCharacteristicKey;
    private String function;
    private String coefficient;
    private String offset;

    public RuleKey ruleKey() {
      return ruleKey;
    }

    public RuleDebt setRuleKey(RuleKey ruleKey) {
      this.ruleKey = ruleKey;
      return this;
    }

    public String subCharacteristicKey() {
      return subCharacteristicKey;
    }

    public RuleDebt setSubCharacteristicKey(String subCharacteristicKey) {
      this.subCharacteristicKey = subCharacteristicKey;
      return this;
    }

    public String function() {
      return function;
    }

    public RuleDebt setFunction(String function) {
      this.function = function;
      return this;
    }

    @CheckForNull
    public String coefficient() {
      return coefficient;
    }

    public RuleDebt setCoefficient(@Nullable String coefficient) {
      this.coefficient = coefficient;
      return this;
    }

    @CheckForNull
    public String offset() {
      return offset;
    }

    public RuleDebt setOffset(@Nullable String offset) {
      this.offset = offset;
      return this;
    }

    @Override
    public String toString() {
      return "RuleDebt{" +
        "ruleKey=" + ruleKey +
        ", subCharacteristicKey='" + subCharacteristicKey + '\'' +
        ", function=" + function +
        ", coefficient='" + coefficient + '\'' +
        ", offset='" + offset + '\'' +
        '}';
    }
  }

}
