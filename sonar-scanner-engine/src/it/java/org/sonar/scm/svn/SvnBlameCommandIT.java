/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scm.svn;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;
import org.slf4j.event.Level;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.scm.BlameCommand.BlameInput;
import org.sonar.api.batch.scm.BlameCommand.BlameOutput;
import org.sonar.api.batch.scm.BlameLine;
import org.sonar.api.testfixtures.log.LogTester;
import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc2.SvnCheckout;
import org.tmatesoft.svn.core.wc2.SvnTarget;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class SvnBlameCommandIT {

  /*
   * Note about SONARSCSVN-11: The case of a project baseDir is in a subFolder of working copy is part of method tests by default
   */

  private static final String DUMMY_JAVA = "src/main/java/org/dummy/Dummy.java";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  private FileSystem fs;
  private BlameInput input;
  private String serverVersion;
  private int wcVersion;

  @Parameters(name = "SVN server version {0}, WC version {1}")
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][] {{"1.6", 10}, {"1.7", 29}, {"1.8", 31}, {"1.9", 31}});
  }

  public SvnBlameCommandIT(String serverVersion, int wcVersion) {
    this.serverVersion = serverVersion;
    this.wcVersion = wcVersion;
  }

  @Before
  public void prepare() {
    fs = mock(FileSystem.class);
    input = mock(BlameInput.class);
    when(input.fileSystem()).thenReturn(fs);
  }

  @Test
  public void testParsingOfOutput() throws Exception {
    File repoDir = unzip("repo-svn.zip");

    String scmUrl = "file:///" + unixPath(new File(repoDir, "repo-svn"));
    File baseDir = new File(checkout(scmUrl), "dummy-svn");

    when(fs.baseDir()).thenReturn(baseDir);
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", DUMMY_JAVA)
      .setLines(27)
      .setModuleBaseDir(baseDir.toPath())
      .build();

    BlameOutput blameResult = mock(BlameOutput.class);
    when(input.filesToBlame()).thenReturn(singletonList(inputFile));

    newSvnBlameCommand().blame(input, blameResult);
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(blameResult).blameResult(eq(inputFile), captor.capture());
    List<BlameLine> result = captor.getValue();
    assertThat(result).hasSize(27);
    Date commitDate = new Date(1342691097393L);
    BlameLine[] expected = IntStream.rangeClosed(1, 27).mapToObj(i -> new BlameLine().date(commitDate).revision("2").author("dgageot")).toArray(BlameLine[]::new);
    assertThat(result).containsExactly(expected);
  }

  private File unzip(String repoName) throws IOException {
    File repoDir = temp.newFolder();
    try {
      javaUnzip(Paths.get(this.getClass().getResource("test-repos").toURI()).resolve(serverVersion).resolve(repoName).toFile(), repoDir);
      return repoDir;
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }
  }

  private File checkout(String scmUrl) throws Exception {
    ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
    ISVNAuthenticationManager isvnAuthenticationManager = SVNWCUtil.createDefaultAuthenticationManager(null, null, (char[]) null, false);
    SVNClientManager svnClientManager = SVNClientManager.newInstance(options, isvnAuthenticationManager);
    File out = temp.newFolder();
    SVNUpdateClient updateClient = svnClientManager.getUpdateClient();
    SvnCheckout co = updateClient.getOperationsFactory().createCheckout();
    co.setUpdateLocksOnDemand(updateClient.isUpdateLocksOnDemand());
    co.setSource(SvnTarget.fromURL(SVNURL.parseURIEncoded(scmUrl), SVNRevision.HEAD));
    co.setSingleTarget(SvnTarget.fromFile(out));
    co.setRevision(SVNRevision.HEAD);
    co.setDepth(SVNDepth.INFINITY);
    co.setAllowUnversionedObstructions(false);
    co.setIgnoreExternals(updateClient.isIgnoreExternals());
    co.setExternalsHandler(SvnCodec.externalsHandler(updateClient.getExternalsHandler()));
    co.setTargetWorkingCopyFormat(wcVersion);
    co.run();
    return out;
  }

  @Test
  public void testParsingOfOutputWithMergeHistory() throws Exception {
    File repoDir = unzip("repo-svn-with-merge.zip");

    String scmUrl = "file:///" + unixPath(new File(repoDir, "repo-svn"));
    File baseDir = new File(checkout(scmUrl), "dummy-svn/trunk");

    when(fs.baseDir()).thenReturn(baseDir);
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", DUMMY_JAVA)
      .setLines(27)
      .setModuleBaseDir(baseDir.toPath())
      .build();

    BlameOutput blameResult = mock(BlameOutput.class);
    when(input.filesToBlame()).thenReturn(singletonList(inputFile));

    newSvnBlameCommand().blame(input, blameResult);
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(blameResult).blameResult(eq(inputFile), captor.capture());
    List<BlameLine> result = captor.getValue();
    assertThat(result).hasSize(27);
    Date commitDate = new Date(1342691097393L);
    Date revision6Date = new Date(1415262184300L);

    BlameLine[] expected = IntStream.rangeClosed(1, 27).mapToObj(i -> {
      if (i == 2 || i == 24) {
        return new BlameLine().date(revision6Date).revision("6").author("henryju");
      } else {
        return new BlameLine().date(commitDate).revision("2").author("dgageot");
      }
    }).toArray(BlameLine[]::new);

    assertThat(result).containsExactly(expected);
  }

  @Test
  public void shouldNotFailIfFileContainsLocalModification() throws Exception {
    File repoDir = unzip("repo-svn.zip");

    String scmUrl = "file:///" + unixPath(new File(repoDir, "repo-svn"));
    File baseDir = new File(checkout(scmUrl), "dummy-svn");

    when(fs.baseDir()).thenReturn(baseDir);
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", DUMMY_JAVA)
      .setLines(28)
      .setModuleBaseDir(baseDir.toPath())
      .build();

    Files.write(baseDir.toPath().resolve(DUMMY_JAVA), "\n//foo".getBytes(), StandardOpenOption.APPEND);

    BlameOutput blameResult = mock(BlameOutput.class);
    when(input.filesToBlame()).thenReturn(singletonList(inputFile));

    newSvnBlameCommand().blame(input, blameResult);
    verifyNoInteractions(blameResult);
  }

  // SONARSCSVN-7
  @Test
  public void shouldNotFailOnWrongFilename() throws Exception {
    File repoDir = unzip("repo-svn.zip");

    String scmUrl = "file:///" + unixPath(new File(repoDir, "repo-svn"));
    File baseDir = new File(checkout(scmUrl), "dummy-svn");

    when(fs.baseDir()).thenReturn(baseDir);
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", DUMMY_JAVA.toLowerCase())
      .setLines(27)
      .setModuleBaseDir(baseDir.toPath())
      .build();

    BlameOutput blameResult = mock(BlameOutput.class);
    when(input.filesToBlame()).thenReturn(singletonList(inputFile));

    newSvnBlameCommand().blame(input, blameResult);
    verifyNoInteractions(blameResult);
  }

  @Test
  public void shouldNotFailOnUncommitedFile() throws Exception {
    File repoDir = unzip("repo-svn.zip");

    String scmUrl = "file:///" + unixPath(new File(repoDir, "repo-svn"));
    File baseDir = new File(checkout(scmUrl), "dummy-svn");

    when(fs.baseDir()).thenReturn(baseDir);
    String relativePath = "src/main/java/org/dummy/Dummy2.java";
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", relativePath)
      .setLines(28)
      .setModuleBaseDir(baseDir.toPath())
      .build();

    Files.write(baseDir.toPath().resolve(relativePath), "package org.dummy;\npublic class Dummy2 {}".getBytes());

    BlameOutput blameResult = mock(BlameOutput.class);
    when(input.filesToBlame()).thenReturn(singletonList(inputFile));

    newSvnBlameCommand().blame(input, blameResult);
    verifyNoInteractions(blameResult);
  }

  @Test
  public void shouldNotFailOnUncommitedDir() throws Exception {
    File repoDir = unzip("repo-svn.zip");

    String scmUrl = "file:///" + unixPath(new File(repoDir, "repo-svn"));
    File baseDir = new File(checkout(scmUrl), "dummy-svn");

    when(fs.baseDir()).thenReturn(baseDir);
    String relativePath = "src/main/java/org/dummy2/dummy/Dummy.java";
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", relativePath)
      .setLines(28)
      .setModuleBaseDir(baseDir.toPath())
      .build();

    Path filepath = new File(baseDir, relativePath).toPath();
    Files.createDirectories(filepath.getParent());
    Files.write(filepath, "package org.dummy;\npublic class Dummy {}".getBytes());

    BlameOutput blameResult = mock(BlameOutput.class);
    when(input.filesToBlame()).thenReturn(singletonList(inputFile));

    newSvnBlameCommand().blame(input, blameResult);
    verifyNoInteractions(blameResult);
  }

  @Test
  public void blame_givenNoCredentials_logWarning() throws Exception {
    BlameOutput output = mock(BlameOutput.class);
    InputFile inputFile = mock(InputFile.class);
    SvnBlameCommand svnBlameCommand = newSvnBlameCommand();

    SVNClientManager clientManager = mock(SVNClientManager.class);
    SVNLogClient logClient = mock(SVNLogClient.class);
    SVNStatusClient statusClient = mock(SVNStatusClient.class);
    SVNStatus status = mock(SVNStatus.class);

    when(clientManager.getLogClient()).thenReturn(logClient);
    when(clientManager.getStatusClient()).thenReturn(statusClient);
    when(status.getContentsStatus()).thenReturn(SVNStatusType.STATUS_NORMAL);
    when(inputFile.file()).thenReturn(mock(File.class));
    when(statusClient.doStatus(any(File.class), anyBoolean())).thenReturn(status);

    doThrow(SVNAuthenticationException.class).when(logClient).doAnnotate(any(File.class), any(SVNRevision.class),
      any(SVNRevision.class), any(SVNRevision.class), anyBoolean(), anyBoolean(), any(AnnotationHandler.class),
      eq(null));

    assertThrows(IllegalStateException.class, () -> {
      svnBlameCommand.blame(clientManager, inputFile, output);
      assertThat(logTester.logs(Level.WARN)).contains("Authentication to SVN server is required but no " +
        "authentication data was passed to the scanner");
    });

  }

  @Test
  public void blame_givenCredentialsSupplied_doNotlogWarning() throws Exception {
    BlameOutput output = mock(BlameOutput.class);
    InputFile inputFile = mock(InputFile.class);
    SvnConfiguration properties = mock(SvnConfiguration.class);
    SvnBlameCommand svnBlameCommand = new SvnBlameCommand(properties);

    SVNClientManager clientManager = mock(SVNClientManager.class);
    SVNLogClient logClient = mock(SVNLogClient.class);
    SVNStatusClient statusClient = mock(SVNStatusClient.class);
    SVNStatus status = mock(SVNStatus.class);

    when(properties.isEmpty()).thenReturn(true);
    when(clientManager.getLogClient()).thenReturn(logClient);
    when(clientManager.getStatusClient()).thenReturn(statusClient);
    when(status.getContentsStatus()).thenReturn(SVNStatusType.STATUS_NORMAL);
    when(inputFile.file()).thenReturn(mock(File.class));
    when(statusClient.doStatus(any(File.class), anyBoolean())).thenReturn(status);

    doThrow(SVNAuthenticationException.class).when(logClient).doAnnotate(any(File.class), any(SVNRevision.class),
      any(SVNRevision.class), any(SVNRevision.class), anyBoolean(), anyBoolean(), any(AnnotationHandler.class),
      eq(null));

    assertThrows(IllegalStateException.class, () -> svnBlameCommand.blame(clientManager, inputFile, output));
    assertThat(logTester.logs(Level.WARN)).contains("Authentication to SVN server is required but no authentication data was passed to the scanner");
  }

  private static void javaUnzip(File zip, File toDir) {
    try {
      try (ZipFile zipFile = new ZipFile(zip)) {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement();
          File to = new File(toDir, entry.getName());
          if (entry.isDirectory()) {
            Files.createDirectories(to.toPath());
          } else {
            File parent = to.getParentFile();
            if (parent != null) {
              Files.createDirectories(parent.toPath());
            }

            Files.copy(zipFile.getInputStream(entry), to.toPath());
          }
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to unzip " + zip + " to " + toDir, e);
    }
  }

  private static String unixPath(File file) {
    return file.getAbsolutePath().replace('\\', '/');
  }

  private SvnBlameCommand newSvnBlameCommand() {
    return new SvnBlameCommand(mock(SvnConfiguration.class));
  }
}
