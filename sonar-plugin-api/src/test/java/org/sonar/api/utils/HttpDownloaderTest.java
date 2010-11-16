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
package org.sonar.api.utils;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.testing.ServletTester;
import org.sonar.api.platform.Server;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Properties;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpDownloaderTest {

  private static ServletTester tester;
  private static String baseUrl;

  @BeforeClass
  public static void startServer() throws Exception {
    tester = new ServletTester();
    tester.setContextPath("/");
    tester.addServlet(RedirectServlet.class, "/redirect/");
    tester.addServlet(FakeServlet.class, "/");
    baseUrl = tester.createSocketConnector(true);
    tester.start();
  }

  @AfterClass
  public static void stopServer() throws Exception {
    /* workaround for a jetty deadlock :
      1. stopping server logs a SLF4J/Logback message, but the message calls AbstractConnector.toString, which is synchronized on getLocalPort()
      2. a jetty thread tries also to stop and is locked on the same synchronized block.

      "3862294@qtp-17303670-1":
	at ch.qos.logback.core.AppenderBase.doAppend(AppenderBase.java:66)
	- waiting to lock <0x8be33f88> (a ch.qos.logback.core.ConsoleAppender)
	at ch.qos.logback.core.spi.AppenderAttachableImpl.appendLoopOnAppenders(AppenderAttachableImpl.java:60)
	at ch.qos.logback.classic.Logger.appendLoopOnAppenders(Logger.java:270)
	at ch.qos.logback.classic.Logger.callAppenders(Logger.java:257)
	at ch.qos.logback.classic.Logger.buildLoggingEventAndAppend(Logger.java:439)
	at ch.qos.logback.classic.Logger.filterAndLog_2(Logger.java:430)
	at ch.qos.logback.classic.Logger.debug(Logger.java:508)
	at org.mortbay.log.Slf4jLog.debug(Slf4jLog.java:40)
	at org.mortbay.log.Log.debug(Log.java:112)
	at org.mortbay.component.AbstractLifeCycle.stop(AbstractLifeCycle.java:77)
	- locked <0x8b8cf980> (a java.lang.Object)
	at org.mortbay.jetty.nio.SelectChannelConnector.close(SelectChannelConnector.java:136)
	- locked <0x8b8c0150> (a org.mortbay.jetty.nio.SelectChannelConnector)
	at org.mortbay.jetty.AbstractConnector$Acceptor.run(AbstractConnector.java:735)
	at org.mortbay.thread.QueuedThreadPool$PoolThread.run(QueuedThreadPool.java:582)
"main":
	at org.mortbay.jetty.nio.SelectChannelConnector.getLocalPort(SelectChannelConnector.java:188)
	- waiting to lock <0x8b8c0150> (a org.mortbay.jetty.nio.SelectChannelConnector)
	at org.mortbay.jetty.AbstractConnector.toString(AbstractConnector.java:669)
	at java.lang.String.valueOf(String.java:2826)
	at java.lang.StringBuffer.append(StringBuffer.java:219)
	- locked <0x8b7e8618> (a java.lang.StringBuffer)
	at org.slf4j.helpers.MessageFormatter.deeplyAppendParameter(MessageFormatter.java:237)
	at org.slf4j.helpers.MessageFormatter.arrayFormat(MessageFormatter.java:196)
	at ch.qos.logback.classic.spi.LoggingEvent.getFormattedMessage(LoggingEvent.java:282)
	at ch.qos.logback.classic.pattern.MessageConverter.convert(MessageConverter.java:22)
	at ch.qos.logback.classic.pattern.MessageConverter.convert(MessageConverter.java:19)
	at ch.qos.logback.core.pattern.FormattingConverter.write(FormattingConverter.java:32)
	at ch.qos.logback.core.pattern.PatternLayoutBase.writeLoopOnConverters(PatternLayoutBase.java:110)
	at ch.qos.logback.classic.PatternLayout.doLayout(PatternLayout.java:132)
	at ch.qos.logback.classic.PatternLayout.doLayout(PatternLayout.java:51)
	at ch.qos.logback.core.WriterAppender.subAppend(WriterAppender.java:261)
	at ch.qos.logback.core.WriterAppender.append(WriterAppender.java:114)
	at ch.qos.logback.core.AppenderBase.doAppend(AppenderBase.java:87)
	- locked <0x8be33f88> (a ch.qos.logback.core.ConsoleAppender)
	at ch.qos.logback.core.spi.AppenderAttachableImpl.appendLoopOnAppenders(AppenderAttachableImpl.java:60)
	at ch.qos.logback.classic.Logger.appendLoopOnAppenders(Logger.java:270)
	at ch.qos.logback.classic.Logger.callAppenders(Logger.java:257)
	at ch.qos.logback.classic.Logger.buildLoggingEventAndAppend(Logger.java:439)
	at ch.qos.logback.classic.Logger.filterAndLog_2(Logger.java:430)
	at ch.qos.logback.classic.Logger.info(Logger.java:605)
	at org.mortbay.log.Slf4jLog.info(Slf4jLog.java:67)
	at org.mortbay.log.Log.info(Log.java:154)
	at org.mortbay.jetty.AbstractConnector.doStop(AbstractConnector.java:313)
	at org.mortbay.jetty.nio.SelectChannelConnector.doStop(SelectChannelConnector.java:326)
	at org.mortbay.component.AbstractLifeCycle.stop(AbstractLifeCycle.java:76)
	- locked <0x8b8ce798> (a java.lang.Object)
	at org.mortbay.jetty.Server.doStop(Server.java:280)
	at org.mortbay.component.AbstractLifeCycle.stop(AbstractLifeCycle.java:76)
	- locked <0x8b8cf7b8> (a java.lang.Object)
	at org.mortbay.jetty.testing.ServletTester.stop(ServletTester.java:92)
	at org.sonar.api.utils.HttpDownloaderTest.stopServer(HttpDownloaderTest.java:62)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
	at java.lang.reflect.Method.invoke(Method.java:597)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:44)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:15)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:41)
	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:37)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:76)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:50)
	at org.junit.runners.ParentRunner$3.run(ParentRunner.java:193)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:52)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:191)
	at org.junit.runners.ParentRunner.access$000(ParentRunner.java:42)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:184)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:236)
	at org.apache.maven.surefire.junit4.JUnit4TestSet.execute(JUnit4TestSet.java:59)
	at org.apache.maven.surefire.suite.AbstractDirectoryTestSuite.executeTestSet(AbstractDirectoryTestSuite.java:120)
	at org.apache.maven.surefire.suite.AbstractDirectoryTestSuite.execute(AbstractDirectoryTestSuite.java:103)
	at org.apache.maven.surefire.Surefire.run(Surefire.java:169)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
	at java.lang.reflect.Method.invoke(Method.java:597)
	at org.apache.maven.surefire.booter.SurefireBooter.runSuitesInProcess(SurefireBooter.java:350)
	at org.apache.maven.surefire.booter.SurefireBooter.main(SurefireBooter.java:1021)



	For this reason the ugly workaround is to wait the end of the thread before stopping...

	We'll have to check if Jetty 7 resolves this toString() issue.
    */
    Thread.sleep(1000);
    tester.stop();
  }

  @Test
  public void downloadBytes() throws URISyntaxException {
    byte[] bytes = new HttpDownloader().download(new URI(baseUrl));
    assertThat(bytes.length, greaterThan(10));
  }

  @Test(expected = SonarException.class)
  public void failIfServerDown() throws URISyntaxException {
    // I hope that the port 1 is not used !
    new HttpDownloader().download(new URI("http://localhost:1/unknown"));
  }

  @Test
  public void downloadToFile() throws URISyntaxException, IOException {
    File toDir = new File("target/test-tmp/org/sonar/api/utils/DownloaderTest/");
    FileUtils.forceMkdir(toDir);
    FileUtils.cleanDirectory(toDir);
    File toFile = new File(toDir, "downloadToFile.txt");

    new HttpDownloader().download(new URI(baseUrl), toFile);
    assertThat(toFile.exists(), is(true));
    assertThat(toFile.length(), greaterThan(10l));
  }

  @Test
  public void shouldNotCreateFileIfFailToDownload() throws Exception {
    File toDir = new File("target/test-tmp/org/sonar/api/utils/DownloaderTest/");
    FileUtils.forceMkdir(toDir);
    FileUtils.cleanDirectory(toDir);
    File toFile = new File(toDir, "downloadToFile.txt");

    try {
      // I hope that the port 1 is not used !
      new HttpDownloader().download(new URI("http://localhost:1/unknown"), toFile);
    } catch (SonarException e) {
      assertThat(toFile.exists(), is(false));
    }
  }

  @Test
  public void userAgentIsSonarVersion() throws URISyntaxException, IOException {
    Server server = mock(Server.class);
    when(server.getVersion()).thenReturn("2.2");

    byte[] bytes = new HttpDownloader(server, new PropertiesConfiguration()).download(new URI(baseUrl));
    Properties props = new Properties();
    props.load(IOUtils.toInputStream(new String(bytes)));
    assertThat(props.getProperty("agent"), is("Sonar 2.2"));
  }

  @Test
  public void followRedirect() throws URISyntaxException {
    byte[] bytes = new HttpDownloader().download(new URI(baseUrl + "/redirect/"));
    assertThat(new String(bytes), containsString("count"));
  }

  @Test
  public void shouldGetDirectProxySynthesis() throws URISyntaxException {
    ProxySelector proxySelector = mock(ProxySelector.class);
    when(proxySelector.select((URI) anyObject())).thenReturn(Arrays.asList(Proxy.NO_PROXY));
    assertThat(HttpDownloader.getProxySynthesis(new URI("http://an_url"), proxySelector), is("no proxy"));
  }

  @Test
  public void shouldGetProxySynthesis() throws URISyntaxException {
    ProxySelector proxySelector = mock(ProxySelector.class);
    when(proxySelector.select((URI) anyObject())).thenReturn(Arrays.asList((Proxy) new FakeProxy()));
    assertThat(HttpDownloader.getProxySynthesis(new URI("http://an_url"), proxySelector), is("proxy: http://proxy_url:4040"));
  }
}

class FakeProxy extends Proxy {
  public FakeProxy() {
    super(Type.HTTP, new InetSocketAddress("http://proxy_url", 4040));
  }
}
