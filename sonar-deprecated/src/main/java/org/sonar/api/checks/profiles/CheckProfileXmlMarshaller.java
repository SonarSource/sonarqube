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
package org.sonar.api.checks.profiles;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;
import org.apache.commons.io.IOUtils;
import org.sonar.check.Priority;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

/**
 * @since 2.1 (experimental)
 * @deprecated since 2.3
 */
@Deprecated
public final class CheckProfileXmlMarshaller {

  public static void toXml(CheckProfile profile, Writer writer) {
    getXStream().toXML(profile, writer);
  }

  public static CheckProfile fromXml(Reader xml) {
    return (CheckProfile) getXStream().fromXML(xml);
  }

  public static CheckProfile fromXmlInClasspath(String pathToXml) {
    return fromXmlInClasspath(pathToXml, CheckProfileXmlMarshaller.class);
  }

  public static CheckProfile fromXmlInClasspath(String pathToXml, Class clazz) {
    Reader reader = new InputStreamReader(clazz.getResourceAsStream(pathToXml));
    try {
      return fromXml(reader);
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  private static XStream getXStream() {
    XStream xstream = new XStream(new CompactDriver());
    xstream.setClassLoader(CheckProfileXmlMarshaller.class.getClassLoader());
    xstream.registerConverter(new CheckConverter());
    xstream.alias("profile", CheckProfile.class);
    xstream.alias("check", Check.class);
    xstream.addImplicitCollection(CheckProfile.class, "checks");
    return xstream;
  }

  private static class CompactDriver extends XppDriver {
    @Override
    public HierarchicalStreamWriter createWriter(Writer out) {
      return new XDataPrintWriter(out);
    }
  }


  private static class XDataPrintWriter extends CompactWriter {
    public XDataPrintWriter(Writer writer) {
      super(writer, XML_1_0);
    }
  }

  private static class CheckConverter implements Converter {

    public boolean canConvert(Class clazz) {
      return clazz.equals(Check.class);
    }

    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
      Check check = (Check) value;
      writer.startNode("repository");
      writer.setValue(check.getRepositoryKey());
      writer.endNode();
      writer.startNode("template");
      writer.setValue(check.getTemplateKey());
      writer.endNode();
      writer.startNode("priority");
      writer.setValue(check.getPriority().toString());
      writer.endNode();
      for (Map.Entry<String, String> entry : check.getProperties().entrySet()) {
        if (entry.getValue() != null) {
          writer.startNode("property");
          writer.startNode("key");
          writer.setValue(entry.getKey());
          writer.endNode();
          writer.startNode("value");
          // TODO is escaping automatically supported by xstream ?
          writer.setValue(entry.getValue());
          writer.endNode();
          writer.endNode();
        }
      }
    }

    public Object unmarshal(HierarchicalStreamReader reader,
                            UnmarshallingContext context) {
      Check check = new Check();
      while (reader.hasMoreChildren()) {
        reader.moveDown();
        readValue(reader, check);
        reader.moveUp();
      }
      return check;
    }

    private void readValue(HierarchicalStreamReader reader, Check check) {
      if (reader.getNodeName().equals("repository")) {
        check.setRepositoryKey(reader.getValue());

      } else if (reader.getNodeName().equals("template")) {
        check.setTemplateKey(reader.getValue());

      } else if (reader.getNodeName().equals("priority")) {
        check.setPriority(Priority.valueOf(reader.getValue()));

      } else if (reader.getNodeName().equals("property")) {
        reader.moveDown();
        String key = reader.getValue();
        reader.moveUp();

        reader.moveDown();
        String value = reader.getValue();
        reader.moveUp();
        check.addProperty(key, value);
      }
    }

  }
}
