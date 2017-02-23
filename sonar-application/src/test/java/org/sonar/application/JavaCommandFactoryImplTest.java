/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.application;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.ProcessId;
import org.sonar.process.Props;
import org.sonar.process.monitor.JavaCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class JavaCommandFactoryImplTest {
  private static final String SEARCH_JAVA_OPTS = "sonar.search.javaOpts";
  private static final String SEARCH_JAVA_ADDITIONAL_OPTS = "sonar.search.javaAdditionalOpts";
  private static final String WEB_JAVA_OPTS = "sonar.web.javaOpts";
  private static final String WEB_JAVA_ADDITIONAL_OPTS = "sonar.web.javaAdditionalOpts";
  private static final String CE_JAVA_OPTS = "sonar.ce.javaOpts";
  private static final String CE_JAVA_ADDITIONAL_OPTS = "sonar.ce.javaAdditionalOpts";
  private static final String PATH_LOGS = "sonar.path.logs";
  private static final String JDBC_DRIVER_PATH = "sonar.jdbc.driverPath";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private File homeDir;
  private JavaCommandFactoryImpl underTest = new JavaCommandFactoryImpl();

  @Before
  public void setUp() throws Exception {
    homeDir = temp.newFolder();
  }

  @Test
  public void createEsCommand_fails_if_search_javaOpts_property_is_not_set() {
    expectMissingPropertyIAE(SEARCH_JAVA_OPTS);

    underTest.createESCommand(newProps(), homeDir);
  }

  @Test
  public void createEsCommand_fails_if_search_javaAdditionalOpts_property_is_not_set() {
    expectMissingPropertyIAE(SEARCH_JAVA_ADDITIONAL_OPTS);

    underTest.createESCommand(newProps(SEARCH_JAVA_OPTS, "foo"), homeDir);
  }

  @Test
  public void createEsCommand_sets_SearchServer_for_className() {
    JavaCommand javaCommand = underTest.createESCommand(newPropsWithRequiredProperties(), homeDir);

    assertThat(javaCommand.getClassName()).isEqualTo("org.sonar.search.SearchServer");
  }

  @Test
  public void createESCommand_puts_common_and_search_lib_directories_in_classpath() {
    JavaCommand javaCommand = underTest.createESCommand(newPropsWithRequiredProperties(), homeDir);

    assertThat(javaCommand.getClasspath()).containsOnly("./lib/common/*", "./lib/search/*");
  }

  @Test
  public void createESCommand_adds_headless_java_option() {
    JavaCommand javaCommand = underTest.createESCommand(newPropsWithRequiredProperties(), homeDir);

    assertThat(javaCommand.getJavaOptions()).contains("-Djava.awt.headless=true");
  }

  @Test
  public void createESCommand_adds_search_javaOpts_and_javaAdditionalOpts_java_options() {
    JavaCommand javaCommand = underTest.createESCommand(newPropsWithRequiredProperties(), homeDir);

    assertThat(javaCommand.getJavaOptions()).contains(mockValueFor(SEARCH_JAVA_OPTS), mockValueFor(SEARCH_JAVA_ADDITIONAL_OPTS));
  }

  @Test
  public void createESCommand_sets_ES_processId() {
    JavaCommand javaCommand = underTest.createESCommand(newPropsWithRequiredProperties(), homeDir);

    assertThat(javaCommand.getProcessId()).isSameAs(ProcessId.ELASTICSEARCH);
  }

  @Test
  public void createESCommand_sets_workdir_to_argument() {
    JavaCommand javaCommand = underTest.createESCommand(newPropsWithRequiredProperties(), homeDir);

    assertThat(javaCommand.getWorkDir()).isSameAs(homeDir);
  }

  @Test
  public void createESCommand_add_options_for_http_and_https_proxies_from_props() throws Exception {
    addOptionsForHttpAndHttpsProxiesFromProps((props, fileDir) -> underTest.createESCommand(props, fileDir));
  }

  @Test
  public void createESCommand_use_http_properties_from_props_as_defaults_for_https_properties() throws Exception {
    userHttpPropertiesFromPropsAsDefaultForHttpsProperties((props, file) -> underTest.createESCommand(props, file));
  }

  @Test
  public void createEsCommand_add_no_proxy_option_if_no_proxy_property_in_props() throws Exception {
    noProxyOptionIfNoProxyPropertyInProps((props, file) -> underTest.createESCommand(props, file));
  }

  @Test
  public void createEsCommand_passes_rawProperties_of_Props_argument_as_argument_of_javaCommand() {
    passesRawPropertiesOfPropsAsArgumentsOfJavaCommand((props, fileDir) -> underTest.createESCommand(props, fileDir));
  }

  @Test
  public void createWebCommand_fails_if_web_javaOpts_property_is_not_set() {
    expectMissingPropertyIAE(WEB_JAVA_OPTS);

    underTest.createWebCommand(newProps(), homeDir);
  }

  @Test
  public void createWebCommand_fails_if_web_javaAdditionalOpts_property_is_not_set() {
    expectMissingPropertyIAE(WEB_JAVA_ADDITIONAL_OPTS);

    underTest.createWebCommand(newProps(WEB_JAVA_OPTS, "foo"), homeDir);
  }

  @Test
  public void createWebCommand_fails_if_log_dir_path_property_is_not_set() {
    expectMissingPropertyIAE(PATH_LOGS);

    underTest.createWebCommand(newProps(WEB_JAVA_OPTS, "foo", WEB_JAVA_ADDITIONAL_OPTS, "bar"), homeDir);
  }

  @Test
  public void createWebCommand_sets_SearchServer_for_className() {
    JavaCommand javaCommand = underTest.createWebCommand(newPropsWithRequiredProperties(), homeDir);

    assertThat(javaCommand.getClassName()).isEqualTo("org.sonar.server.app.WebServer");
  }

  @Test
  public void createWebCommand_puts_common_and_server_lib_directories_in_classpath() {
    JavaCommand javaCommand = underTest.createWebCommand(newPropsWithRequiredProperties(), homeDir);

    assertThat(javaCommand.getClasspath()).containsOnly("./lib/common/*", "./lib/server/*");
  }

  @Test
  public void createWebCommand_adds_jdbc_driver_to_classpath_if_property_is_set_in_props() {
    JavaCommand javaCommand = underTest.createWebCommand(newPropsWithRequiredProperties(JDBC_DRIVER_PATH, "foo"), homeDir);

    assertThat(javaCommand.getClasspath()).contains("foo");
  }

  @Test
  public void createWebCommand_set_env_variable_for_path_to_log_dir() {
    JavaCommand javaCommand = underTest.createWebCommand(newPropsWithRequiredProperties(), homeDir);

    assertThat(javaCommand.getEnvVariables()).contains(entry("sonar.path.logs", mockValueFor(PATH_LOGS)));
  }

  @Test
  public void createWebCommand_adds_search_javaOpts_and_javaAdditionalOpts_java_options() {
    JavaCommand javaCommand = underTest.createWebCommand(newPropsWithRequiredProperties(), homeDir);

    assertThat(javaCommand.getJavaOptions()).contains(mockValueFor(WEB_JAVA_OPTS), mockValueFor(WEB_JAVA_ADDITIONAL_OPTS));
  }

  @Test
  public void createWebCommand_sets_headless_and_encoding_java_options() {
    JavaCommand javaCommand = underTest.createWebCommand(newPropsWithRequiredProperties(), homeDir);

    assertThat(javaCommand.getJavaOptions()).contains("-Djava.awt.headless=true", "-Dfile.encoding=UTF-8");
  }

  @Test
  public void createWebCommand_sets_WEB_SERVER_processId() {
    JavaCommand javaCommand = underTest.createWebCommand(newPropsWithRequiredProperties(), homeDir);

    assertThat(javaCommand.getProcessId()).isSameAs(ProcessId.WEB_SERVER);
  }

  @Test
  public void createWebCommand_sets_workdir_to_argument() {
    JavaCommand javaCommand = underTest.createWebCommand(newPropsWithRequiredProperties(), homeDir);

    assertThat(javaCommand.getWorkDir()).isSameAs(homeDir);
  }

  @Test
  public void createWebCommand_add_options_fore_http_and_https_proxies_from_props() throws Exception {
    addOptionsForHttpAndHttpsProxiesFromProps((props, fileDir) -> underTest.createWebCommand(props, fileDir));
  }

  @Test
  public void createWebCommand_use_http_properties_from_props_as_defaults_for_https_properties() throws Exception {
    userHttpPropertiesFromPropsAsDefaultForHttpsProperties((props, file) -> underTest.createWebCommand(props, file));
  }

  @Test
  public void createWebCommand_add_no_proxy_option_if_no_proxy_property_in_props() throws Exception {
    noProxyOptionIfNoProxyPropertyInProps((props, file) -> underTest.createWebCommand(props, file));
  }

  @Test
  public void createWebCommand_passes_rawProperties_of_Props_argument_as_argument_of_javaCommand() {
    passesRawPropertiesOfPropsAsArgumentsOfJavaCommand((props, fileDir) -> underTest.createWebCommand(props, fileDir));
  }

  @Test
  public void createCeCommand_fails_if_web_javaOpts_property_is_not_set() {
    expectMissingPropertyIAE(CE_JAVA_OPTS);

    underTest.createCeCommand(newProps(), homeDir);
  }

  @Test
  public void createCeCommand_fails_if_web_javaAdditionalOpts_property_is_not_set() {
    expectMissingPropertyIAE(CE_JAVA_ADDITIONAL_OPTS);

    underTest.createCeCommand(newProps(CE_JAVA_OPTS, "foo"), homeDir);
  }

  @Test
  public void createCeCommand_sets_SearchServer_for_className() {
    JavaCommand javaCommand = underTest.createCeCommand(newPropsWithRequiredProperties(), homeDir);

    assertThat(javaCommand.getClassName()).isEqualTo("org.sonar.ce.app.CeServer");
  }

  @Test
  public void createCeCommand_puts_common_server_and_ce_lib_directories_in_classpath() {
    JavaCommand javaCommand = underTest.createCeCommand(newPropsWithRequiredProperties(), homeDir);

    assertThat(javaCommand.getClasspath()).containsOnly("./lib/common/*", "./lib/server/*", "./lib/ce/*");
  }

  @Test
  public void createCeCommand_adds_jdbc_driver_to_classpath_if_property_is_set_in_props() {
    JavaCommand javaCommand = underTest.createCeCommand(newPropsWithRequiredProperties(JDBC_DRIVER_PATH, "foo"), homeDir);

    assertThat(javaCommand.getClasspath()).contains("foo");
  }

  @Test
  public void createCeCommand_sets_headless_and_encoding_java_options() {
    JavaCommand javaCommand = underTest.createCeCommand(newPropsWithRequiredProperties(), homeDir);

    assertThat(javaCommand.getJavaOptions()).contains("-Djava.awt.headless=true", "-Dfile.encoding=UTF-8");
  }

  @Test
  public void createCeCommand_adds_search_javaOpts_and_javaAdditionalOpts_java_options() {
    JavaCommand javaCommand = underTest.createCeCommand(newPropsWithRequiredProperties(), homeDir);

    assertThat(javaCommand.getJavaOptions()).contains(mockValueFor(CE_JAVA_OPTS), mockValueFor(CE_JAVA_ADDITIONAL_OPTS));
  }

  @Test
  public void createCeCommand_sets_workdir_to_argument() {
    JavaCommand javaCommand = underTest.createCeCommand(newPropsWithRequiredProperties(), homeDir);

    assertThat(javaCommand.getWorkDir()).isSameAs(homeDir);
  }

  @Test
  public void createCeCommand_sets_COMPUTE_ENGINE_processId() {
    JavaCommand javaCommand = underTest.createCeCommand(newPropsWithRequiredProperties(), homeDir);

    assertThat(javaCommand.getProcessId()).isSameAs(ProcessId.COMPUTE_ENGINE);
  }

  @Test
  public void createCeCommand_add_options_for_http_and_https_proxies_from_props() throws Exception {
    addOptionsForHttpAndHttpsProxiesFromProps((props, fileDir) -> underTest.createCeCommand(props, fileDir));
  }

  @Test
  public void createCeCommand_use_http_properties_from_props_as_defaults_for_https_properties() throws Exception {
    userHttpPropertiesFromPropsAsDefaultForHttpsProperties((props, file) -> underTest.createCeCommand(props, file));
  }

  @Test
  public void createCeCommand_passes_rawProperties_of_Props_argument_as_argument_of_javaCommand() {
    passesRawPropertiesOfPropsAsArgumentsOfJavaCommand((props, fileDir) -> underTest.createCeCommand(props, fileDir));
  }

  private void addOptionsForHttpAndHttpsProxiesFromProps(BiFunction<Props, File, JavaCommand> callCreateMethod) {
    Props props = newPropsWithRequiredProperties();

    // These properties can be defined in conf/sonar.properties.
    // They must be propagated to JVM.
    props.set("http.proxyHost", "1.2.3.4");
    props.set("http.proxyPort", "80");
    props.set("https.proxyHost", "5.6.7.8");
    props.set("https.proxyPort", "443");

    JavaCommand command = callCreateMethod.apply(props, homeDir);
    assertThat(command.getJavaOptions()).contains("-Dhttp.proxyHost=1.2.3.4");
    assertThat(command.getJavaOptions()).contains("-Dhttp.proxyPort=80");
    assertThat(command.getJavaOptions()).contains("-Dhttps.proxyHost=5.6.7.8");
    assertThat(command.getJavaOptions()).contains("-Dhttps.proxyPort=443");
  }

  @Test
  public void createCeCommand_add_no_proxy_option_if_no_proxy_property_in_props() throws Exception {
    noProxyOptionIfNoProxyPropertyInProps((props, file) -> underTest.createCeCommand(props, file));
  }

  private void userHttpPropertiesFromPropsAsDefaultForHttpsProperties(BiFunction<Props, File, JavaCommand> callCreateMethod) {
    Props props = newPropsWithRequiredProperties();
    props.set("http.proxyHost", "1.2.3.4");
    props.set("http.proxyPort", "80");

    JavaCommand command = callCreateMethod.apply(props, homeDir);
    assertThat(command.getJavaOptions()).contains("-Dhttp.proxyHost=1.2.3.4");
    assertThat(command.getJavaOptions()).contains("-Dhttp.proxyPort=80");
    assertThat(command.getJavaOptions()).contains("-Dhttps.proxyHost=1.2.3.4");
    assertThat(command.getJavaOptions()).contains("-Dhttps.proxyPort=80");
  }

  private void passesRawPropertiesOfPropsAsArgumentsOfJavaCommand(BiFunction<Props, File, JavaCommand> callCreateMethod) {
    Props props = newPropsWithRequiredProperties("cryptedProperty", "{AES}AAAAA");
    JavaCommand javaCommand = callCreateMethod.apply(props, homeDir);

    Map<String, String> rawProperties = (Map<String, String>) ((Map) props.rawProperties());
    assertThat(javaCommand.getArguments()).containsAllEntriesOf(rawProperties);
  }

  private void noProxyOptionIfNoProxyPropertyInProps(BiFunction<Props, File, JavaCommand> callCreateMethod) {
    JavaCommand command = callCreateMethod.apply(newPropsWithRequiredProperties(), homeDir);

    assertThat(command.getJavaOptions()).doesNotContain("http.proxyHost");
    assertThat(command.getJavaOptions()).doesNotContain("https.proxyHost");
    assertThat(command.getJavaOptions()).doesNotContain("http.proxyPort");
    assertThat(command.getJavaOptions()).doesNotContain("https.proxyPort");
  }

  private void expectMissingPropertyIAE(String property) {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Missing property: " + property);
  }

  private Props newPropsWithRequiredProperties(String... properties) {
    return newProps(
      (props) -> addProperties(props, properties),
      SEARCH_JAVA_OPTS, mockValueFor(SEARCH_JAVA_OPTS),
      SEARCH_JAVA_ADDITIONAL_OPTS, mockValueFor(SEARCH_JAVA_ADDITIONAL_OPTS),
      WEB_JAVA_OPTS, mockValueFor(WEB_JAVA_OPTS),
      WEB_JAVA_ADDITIONAL_OPTS, mockValueFor(WEB_JAVA_ADDITIONAL_OPTS),
      PATH_LOGS, mockValueFor(PATH_LOGS),
      CE_JAVA_OPTS, mockValueFor(CE_JAVA_OPTS),
      CE_JAVA_ADDITIONAL_OPTS, mockValueFor(CE_JAVA_ADDITIONAL_OPTS));
  }

  private static String mockValueFor(String str) {
    return str + "_value";
  }

  private Props newProps(String... properties) {
    return newProps((props) -> {
    }, properties);
  }

  private Props newProps(Consumer<Properties> extraConf, String... properties) {
    Properties props = new Properties();
    addProperties(props, properties);
    extraConf.accept(props);
    return new Props(props);
  }

  private void addProperties(Properties props, String[] properties) {
    if (properties.length % 2 != 0) {
      throw new IllegalArgumentException("Properties must all have key and value");
    }
    for (int i = 0; i < properties.length; i++) {
      props.setProperty(properties[i], properties[i + 1]);
      i++;
    }
  }

}
