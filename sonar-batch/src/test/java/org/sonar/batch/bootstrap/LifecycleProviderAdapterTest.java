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
package org.sonar.batch.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.picocontainer.Startable;
import org.junit.Test;

public class LifecycleProviderAdapterTest {
  private DummyProvider provider;

  @Before
  public void setUp() {
    provider = new DummyProvider();
    provider.provide();
  }

  @Test
  public void testStart() {
    // ComponentLifecycle's start gets called on the provider
    provider.start(null);
    assertThat(provider.inst.started).isEqualTo(true);
    assertThat(provider.isStarted()).isEqualTo(true);
    assertThat(provider.inst.stopped).isEqualTo(false);
  }

  @Test
  public void testSop() {
    // ComponentLifecycle's stop gets called on the provider
    provider.stop(null);
    assertThat(provider.inst.stopped).isEqualTo(true);
    assertThat(provider.isStarted()).isEqualTo(false);
    assertThat(provider.inst.started).isEqualTo(false);
  }

  public class DummyProvided implements Startable {
    boolean started = false;
    boolean stopped = false;

    @Override
    public void start() {
      started = true;
    }

    @Override
    public void stop() {
      stopped = true;
    }
  }

  public class DummyProvider extends LifecycleProviderAdapter {
    DummyProvided inst;

    public DummyProvided provide() {
      inst = new DummyProvided();
      super.instance = inst;
      return inst;
    }
  }
}
