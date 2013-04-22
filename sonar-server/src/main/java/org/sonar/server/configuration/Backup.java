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
package org.sonar.server.configuration;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.basic.DateConverter;
import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.database.DatabaseSession;
import org.sonar.core.persistence.DatabaseVersion;
import org.sonar.server.platform.PersistentSettings;

import javax.annotation.Nullable;
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

  public Backup(DatabaseSession session, PersistentSettings persistentSettings) {
    this();
    this.session = session;

    backupables.add(new MetricsBackup(session));
    backupables.add(new PropertiesBackup(persistentSettings));
    // Note that order is important, because profile can have reference to rule
    backupables.add(new RulesBackup(session));
    backupables.add(new ProfilesBackup(session));
  }

  /**
   * For unit tests
   */
  Backup(List<Backupable> backupables) {
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
      doImportXml(xml);
      LoggerFactory.getLogger(getClass()).info("Backup restored");
    } finally {
      stopDb();
    }
  }

  void doImportXml(String xml) {
    SonarConfig sonarConfig = getSonarConfigFromXml(xml);
    importBackupablesXml(sonarConfig);
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
    return DatabaseVersion.LAST_VERSION;
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
            protected void writeText(QuickWriter writer, @Nullable String text) {
              if (text != null) {
                writer.write("<![CDATA[");
                /*
                * See http://jira.codehaus.org/browse/SONAR-1605 According to XML specification (
                * http://www.w3.org/TR/REC-xml/#sec-cdata-sect ) CData section may contain everything except of sequence ']]>' so we will
                * split all occurrences of this sequence into two CDATA first one would contain ']]' and second '>'
                */
                text = StringUtils.replace(text, "]]>", "]]]]><![CDATA[>");
                writer.write(text);
                writer.write("]]>");
              }
            }
          };
        }
      });

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
