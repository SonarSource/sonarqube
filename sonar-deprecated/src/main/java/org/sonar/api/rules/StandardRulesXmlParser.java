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
package org.sonar.api.rules;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.SonarException;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;

@Deprecated
public class StandardRulesXmlParser {

  /**
   * see the XML format into the unit test src/test/java/.../StandardRulesXmlParserTest
   */
  public List<Rule> parse(String xml) {
    InputStream inputStream = null;
    try {
      inputStream = IOUtils.toInputStream(xml, CharEncoding.UTF_8);
      return setDefaultRulePriorities((List<Rule>) getXStream().fromXML(inputStream));

    } catch (IOException e) {
      throw new SonarException("Can't parse xml file", e);

    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  public List<Rule> parse(Reader reader) {
    return setDefaultRulePriorities((List<Rule>) getXStream().fromXML(reader));
  }

  public List<Rule> parse(InputStream input) {
    try {
      return setDefaultRulePriorities((List<Rule>) getXStream().fromXML(new InputStreamReader(input, CharEncoding.UTF_8)));

    } catch (UnsupportedEncodingException e) {
      throw new SonarException("Can't parse xml file", e);
    }
  }

  private List<Rule> setDefaultRulePriorities(List<Rule> rules) {
    for (Rule rule : rules) {
      if (rule.getPriority() == null) {
        rule.setPriority(RulePriority.MAJOR);
      }
    }
    return rules;
  }

  public String toXml(List<Rule> rules) {
    return getXStream().toXML(rules);
  }

  private static class CDataXppDriver extends XppDriver {
    @Override
    public HierarchicalStreamWriter createWriter(Writer out) {
      return new XDataPrintWriter(out);
    }
  }

  private static class XDataPrintWriter extends PrettyPrintWriter {
    public XDataPrintWriter(Writer writer) {
      super(writer);
    }

    @Override
    protected void writeText(QuickWriter writer, String text) {
      writer.write("<![CDATA[");
      writer.write(text);
      writer.write("]]>");
    }
  }

  private XStream getXStream() {
    XStream xstream = new XStream(new CDataXppDriver());
    xstream.registerConverter(new TrimStringConverter());
    xstream.alias("rules", ArrayList.class);

    xstream.alias("categ", RulesCategory.class);
    xstream.useAttributeFor(RulesCategory.class, "name");
    xstream.aliasField("category", Rule.class, "rulesCategory");

    xstream.alias("rule", Rule.class);
    xstream.useAttributeFor(Rule.class, "key");
    xstream.useAttributeFor("priority", RulePriority.class);

    xstream.addImplicitCollection(Rule.class, "params");

    xstream.alias("param", RuleParam.class);
    xstream.useAttributeFor(RuleParam.class, "key");
    xstream.useAttributeFor(RuleParam.class, "type");

    // only for backward compatibility with sonar 1.4.
    xstream.omitField(RuleParam.class, "defaultValue");
    return xstream;
  }

  /**
   * See http://svn.codehaus.org/xstream/trunk/xstream/src/java/com/thoughtworks/xstream/converters/basic/StringConverter.java
   */
  public static class TrimStringConverter extends AbstractSingleValueConverter {

    private final Map cache;

    public TrimStringConverter(final Map map) {
      cache = map;
    }

    public TrimStringConverter() {
      this(Collections.synchronizedMap(new WeakHashMap()));
    }

    public boolean canConvert(final Class type) {
      return type.equals(String.class);
    }

    public Object fromString(final String str) {
      String trim = StringUtils.trim(str);
      final WeakReference ref = (WeakReference) cache.get(trim);
      String s = (String) (ref == null ? null : ref.get());

      if (s == null) {
        // fill cache
        cache.put(str, new WeakReference(trim));
        s = trim;
      }

      return s;
    }
  }
}
