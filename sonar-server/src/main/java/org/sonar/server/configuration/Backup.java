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
package org.sonar.server.configuration;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.basic.DateConverter;
import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.sonar.api.database.DatabaseSession;
import org.sonar.jpa.entity.SchemaMigration;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class Backup {

  private List<Backupable> backupables;
  private DatabaseSession session;

  protected static final String DATE_FORMAT = "yyyy-MM-dd";

  protected Backup() {
    backupables = new ArrayList<Backupable>();
  }

  public Backup(DatabaseSession session) {
    this();
    this.session = session;

    backupables.add(new MetricsBackup(session));
    backupables.add(new PropertiesBackup(session));
    backupables.add(new ProfilesBackup(session));
  }

  protected Backup(List<Backupable> backupables) {
    this();
    this.backupables = backupables;
  }

  /*
   * Export methods
   */

  public String exportXml() {
    try {
      startDb();
      SonarConfig sonarConfig = new SonarConfig(getVersion(), getCurrentDate());
      return exportXml(sonarConfig);
    } finally {
      stopDb();
    }
  }

  protected String exportXml(SonarConfig sonarConfig) {
    for (Backupable backupable : backupables) {
      backupable.exportXml(sonarConfig);
    }
    String xml = getXmlFromSonarConfig(sonarConfig);
    return addXmlHeader(xml);
  }

  protected String getXmlFromSonarConfig(SonarConfig sonarConfig) {
    XStream xStream = getConfiguredXstream();
    return xStream.toXML(sonarConfig);
  }

  private String addXmlHeader(String xml) {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".concat(xml);
  }

  /*
   * Import methods
   */
  public void importXml(String xml) {
    try {
      startDb();
      SonarConfig sonarConfig = getSonarConfigFromXml(xml);
      importBackupablesXml(sonarConfig);
    } finally {
      stopDb();
    }
  }

  protected void importBackupablesXml(SonarConfig sonarConfig) {
    for (Backupable backupable : backupables) {
      backupable.importXml(sonarConfig);
    }
  }

  protected SonarConfig getSonarConfigFromXml(String xml) {
    try {
      XStream xStream = getConfiguredXstream();
      // Backward compatibility with old levels
      xml = xml.replace("<level><![CDATA[ERROR]]></level>", "<level><![CDATA[MAJOR]]></level>");
      xml = xml.replace("<level><![CDATA[WARNING]]></level>", "<level><![CDATA[INFO]]></level>");
      InputStream inputStream = IOUtils.toInputStream(xml, CharEncoding.UTF_8);

      return (SonarConfig) xStream.fromXML(inputStream);
    } catch (IOException e) {
      throw new RuntimeException("Can't read xml", e);
    }
  }

  /*
   * Utils methods
   */
  protected int getVersion() {
    return SchemaMigration.LAST_VERSION;
  }

  protected Date getCurrentDate() {
    return new Date();
  }

  private XStream getConfiguredXstream() {
    XStream xStream = new XStream(
        new XppDriver() {
          @Override
          public HierarchicalStreamWriter createWriter(Writer out) {
            return new PrettyPrintWriter(out) {
              @Override
              protected void writeText(QuickWriter writer, String text) {
                writer.write("<![CDATA[");
                writer.write(text);
                writer.write("]]>");
              }
            };
          }
        }
    );

    xStream.processAnnotations(SonarConfig.class);
    xStream.addDefaultImplementation(ArrayList.class, Collection.class);
    xStream.registerConverter(new DateConverter(DATE_FORMAT, new String[]{}));

    for (Backupable backupable : backupables) {
      backupable.configure(xStream);
    }
    return xStream;
  }

  private void startDb() {
    session.start();
  }

  private void stopDb() {
    session.stop();
  }

}
