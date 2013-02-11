/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.batch;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;
import org.sonar.batch.bootstrap.Container;

import java.util.Properties;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class BatchTest {

  @Test
  public void shouldExecute() {
    FakeModule module = new FakeModule();
    module.init();
    new Batch(module).execute();

    assertThat(module.started, is(true));
    assertThat(module.stopped, is(true));
  }

  public static class FakeModule extends Container {
    private boolean started=false;
    private boolean stopped=false;

    @Override
    protected void doStart() {
      started = true;
    }

    @Override
    protected void doStop() {
      if (!started) {
        throw new IllegalStateException("Not started");
      }
      stopped = true;
    }

    @Override
    protected void configure() {
    }
  }

  @Test
  public void shouldConvertCommonsConfigurationToProperties() {
    PropertiesConfiguration commonsConf = new PropertiesConfiguration();
    commonsConf.setProperty("foo", "Foo");
    commonsConf.setProperty("list", "One,Two");
    assertThat(commonsConf.getString("list"), is("One"));
    assertThat(commonsConf.getStringArray("list")[0], is("One"));
    assertThat(commonsConf.getStringArray("list")[1], is("Two"));

    Properties props = Batch.convertToProperties(commonsConf);
    assertThat(props.getProperty("foo"), is("Foo"));
    assertThat(props.getProperty("list"), is("One,Two"));
  }
}
