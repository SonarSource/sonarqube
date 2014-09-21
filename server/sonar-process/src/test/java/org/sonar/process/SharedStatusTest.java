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
package org.sonar.process;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SharedStatusTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void prepare() throws Exception {
    File file = temp.newFile();
    assertThat(file).exists();

    SharedStatus sharedStatus = new SharedStatus(file);
    sharedStatus.prepare();
    assertThat(file).doesNotExist();
  }

  @Test
  public void fail_to_prepare_if_file_is_locked() throws Exception {
    File file = mock(File.class);
    when(file.exists()).thenReturn(true);
    when(file.delete()).thenReturn(false);

    SharedStatus sharedStatus = new SharedStatus(file);
    try {
      sharedStatus.prepare();
      fail();
    } catch (MessageException e) {
      // ok
    }
  }

  @Test
  public void create_file_when_ready_then_delete_when_stopped() throws Exception {
    File file = new File(temp.newFolder(), "foo.txt");
    assertThat(file).doesNotExist();

    SharedStatus sharedStatus = new SharedStatus(file);
    sharedStatus.setReady();
    assertThat(file).exists();

    sharedStatus.setStopped();
    assertThat(file).doesNotExist();
  }

  @Test
  public void was_started_after() throws Exception {
    File file = mock(File.class);
    SharedStatus sharedStatus = new SharedStatus(file);

    // does not exist
    when(file.exists()).thenReturn(false);
    when(file.lastModified()).thenReturn(123456L);
    assertThat(sharedStatus.wasStartedAfter(122000L)).isFalse();

    // file created before
    when(file.exists()).thenReturn(true);
    when(file.lastModified()).thenReturn(123456L);
    assertThat(sharedStatus.wasStartedAfter(124000L)).isFalse();

    // file created after
    when(file.exists()).thenReturn(true);
    when(file.lastModified()).thenReturn(123456L);
    assertThat(sharedStatus.wasStartedAfter(123123L)).isTrue();

    // file created after, but can be truncated to second on some OS
    when(file.exists()).thenReturn(true);
    when(file.lastModified()).thenReturn(123000L);
    assertThat(sharedStatus.wasStartedAfter(123456L)).isTrue();
  }
}
