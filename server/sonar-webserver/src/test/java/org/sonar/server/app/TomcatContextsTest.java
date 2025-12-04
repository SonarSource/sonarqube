/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.app;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.MessageException;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.Property;

public class TomcatContextsTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  Tomcat tomcat = mock(Tomcat.class);

  Properties props = new Properties();
  TomcatContexts underTest = new TomcatContexts();

  @Before
  public void setUp() throws Exception {
    props.setProperty(Property.PATH_DATA.getKey(), temp.newFolder("data").getAbsolutePath());
    when(tomcat.addWebapp(anyString(), anyString())).thenReturn(mock(StandardContext.class));
  }

  @Test
  public void configure_root_webapp() {
    props.setProperty("foo", "bar");
    StandardContext context = mock(StandardContext.class);
    when(tomcat.addWebapp(anyString(), anyString())).thenReturn(context);

    underTest.configure(tomcat, new Props(props));

    // configure webapp with properties
    verify(context).addParameter("foo", "bar");
  }

  @Test
  public void create_dir_and_configure_static_directory() throws Exception {
    File dir = temp.newFolder();
    dir.delete();

    underTest.addStaticDir(tomcat, "/deploy", dir);

    assertThat(dir).isDirectory().exists();
    verify(tomcat).addWebapp("/deploy", dir.getAbsolutePath());
  }

  @Test
  public void cleanup_static_directory_if_already_exists() throws Exception {
    File dir = temp.newFolder();
    FileUtils.touch(new File(dir, "foo.txt"));

    underTest.addStaticDir(tomcat, "/deploy", dir);

    assertThat(dir).isDirectory().exists();
    assertThat(new File(dir, "foo.txt")).doesNotExist();
    assertThat(new File(dir, "WEB-INF")).isDirectory();
    assertThat(new File(dir, "WEB-INF/web.xml")).isFile();
    assertThat(new File(dir, "error.html")).isFile();
  }

  @Test
  public void fail_if_static_directory_can_not_be_initialized() throws Exception {
    File dir = temp.newFolder();

    TomcatContexts.Fs fs = mock(TomcatContexts.Fs.class);
    doThrow(new IOException()).when(fs).createOrCleanupDir(any(File.class));
    TomcatContexts tomcatContexts = new TomcatContexts(fs);

    assertThatThrownBy(() -> tomcatContexts.addStaticDir(tomcat, "/deploy", dir))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Fail to create or clean-up directory " + dir.getAbsolutePath());
  }

  @Test
  public void context_path() {
    props.setProperty("sonar.web.context", "/foo");

    assertThat(TomcatContexts.getContextPath(new Props(props))).isEqualTo("/foo");
  }

  @Test
  public void context_path_must_start_with_slash() {
    props.setProperty("sonar.web.context", "foo");
    Props propsToTest = new Props(props);

    assertThatThrownBy(() -> underTest.configure(tomcat, propsToTest))
      .isInstanceOf(MessageException.class)
      .hasMessageContaining("Value of 'sonar.web.context' must start with a forward slash: 'foo'");
  }

  @Test
  public void root_context_path_must_be_blank() {
    props.setProperty("sonar.web.context", "/");

    assertThat(TomcatContexts.getContextPath(new Props(props))).isEmpty();
  }

  @Test
  public void default_context_path_is_root() {
    String context = TomcatContexts.getContextPath(new Props(new Properties()));
    assertThat(context).isEmpty();
  }

  @Test
  public void configure_creates_root_context_when_web_context_is_not_empty() {
    props.setProperty("sonar.web.context", "/sonarqube");

    underTest.configure(tomcat, new Props(props));

    // Verify root context was created at "" path
    verify(tomcat).addWebapp("", new File(props.getProperty(Property.PATH_DATA.getKey()), "web/root").getAbsolutePath());
  }

  @Test
  public void configure_does_not_create_root_context_when_web_context_is_empty() {
    // Default context is empty (root)
    underTest.configure(tomcat, new Props(props));

    // Should only create the main webapp and deploy contexts, not the root context
    verify(tomcat).addWebapp("", new File(props.getProperty(Property.PATH_HOME.getKey()), "web").getAbsolutePath());
  }

  @Test
  public void addRootContext_creates_directory_structure() {
    props.setProperty("sonar.web.context", "/sonarqube");

    underTest.configure(tomcat, new Props(props));

    File rootDir = new File(props.getProperty(Property.PATH_DATA.getKey()), "web/root");
    assertThat(rootDir).isDirectory().exists();
    assertThat(new File(rootDir, "WEB-INF")).isDirectory().exists();
    assertThat(new File(rootDir, "WEB-INF/web.xml")).isFile().exists();
  }

  @Test
  public void addRootContext_web_xml_contains_servlet_configuration() throws Exception {
    props.setProperty("sonar.web.context", "/sonarqube");

    underTest.configure(tomcat, new Props(props));

    File webXml = new File(props.getProperty(Property.PATH_DATA.getKey()), "web/root/WEB-INF/web.xml");
    String content = FileUtils.readFileToString(webXml, "UTF-8");

    assertThat(content)
      .contains("org.sonar.server.app.RootContextServlet")
      .contains("<servlet-name>root</servlet-name>")
      .contains("<url-pattern>/*</url-pattern>")
      .contains("<param-name>webContext</param-name>")
      .contains("<param-value>/sonarqube</param-value>");
  }

  @Test
  public void addRootContext_fails_gracefully_on_io_error() throws Exception {
    props.setProperty("sonar.web.context", "/sonarqube");

    TomcatContexts.Fs fs = mock(TomcatContexts.Fs.class);
    doThrow(new IOException("Cannot create directory")).when(fs).createOrCleanupDir(any(File.class));
    TomcatContexts tomcatContexts = new TomcatContexts(fs);
    Props propsToTest = new Props(props);

    assertThatThrownBy(() -> tomcatContexts.configure(tomcat, propsToTest))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Failed to configure ROOT context")
      .hasCauseInstanceOf(IOException.class);
  }
}
