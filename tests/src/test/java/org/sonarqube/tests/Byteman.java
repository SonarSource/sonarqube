/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
 package org.sonarqube.tests;

import com.sonar.orchestrator.OrchestratorBuilder;
import java.io.File;
import java.net.InetAddress;
import java.util.Collections;
import org.jboss.byteman.agent.submit.Submit;
import org.sonar.process.NetworkUtilsImpl;

import static java.lang.String.format;

/**
 * byteman.jboss.org is used by resilience tests in order to
 * change behavior of server at runtime.
 */
public class Byteman {

  private final int port;
  private final OrchestratorBuilder builder;

  public enum Process {
    WEB("sonar.web.javaAdditionalOpts"), CE("sonar.ce.javaAdditionalOpts");

    private final String argument;

    Process(String argument) {
      this.argument = argument;
    }
  }

  public Byteman(OrchestratorBuilder builder, Process process) {
    this.builder = builder;
    String jar = findBytemanJar();
    port = NetworkUtilsImpl.INSTANCE.getNextAvailablePort(InetAddress.getLoopbackAddress());
    String bytemanArg = format("-javaagent:%s=boot:%s,port:%d", jar, jar, port);
    builder.setServerProperty(process.argument, bytemanArg);
  }

  private static String findBytemanJar() {
    // see pom.xml, Maven copies and renames the artifact.
    File jar = new File("target/byteman.jar");
    if (!jar.exists()) {
      throw new IllegalStateException("Can't find " + jar + ". Please execute 'mvn generate-test-resources' once in directory tests/.");
    }
    return jar.getAbsolutePath();
  }

  public void activateScript(String filename) throws Exception {
    String bytemanScript = findBytemanScript(filename);
    Submit submit = new Submit(InetAddress.getLoopbackAddress().getHostAddress(), port);
    submit.addRulesFromFiles(Collections.singletonList(bytemanScript));
  }

  private static String findBytemanScript(String filename) {
    // see pom.xml, Maven copies and renames the artifact.
    File script = new File( filename);
    if (!script.exists()) {
      throw new IllegalStateException("Can't find " + script);
    }
    return script.getAbsolutePath();
  }

  public void deactivateAllRules() throws Exception {
    Submit submit = new Submit(InetAddress.getLoopbackAddress().getHostAddress(), port);
    try {
      submit.deleteAllRules();
    } catch (java.lang.Exception e) {
      if (e.getMessage() == null || !e.getMessage().contains("No rule scripts to remove")) {
        throw e;
      }
    }
  }

    public OrchestratorBuilder getOrchestratorBuilder() {
      return builder;
    }
}
