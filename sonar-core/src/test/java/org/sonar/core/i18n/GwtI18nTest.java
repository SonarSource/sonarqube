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
package org.sonar.core.i18n;

import com.google.common.collect.Lists;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.mock;

public class GwtI18nTest {

  private GwtI18n i18n;
  private ResourceBundle bundle;

  @Before
  public void init() {
    bundle = ResourceBundle.getBundle("org.sonar.core.i18n.GwtI18nTest.gwt", Locale.ENGLISH);
    i18n = new GwtI18n(mock(DefaultI18n.class));
    i18n.doStart(bundle);
  }

  @Test
  public void shouldListAllPropertyKeysAtStartup() {
    assertThat(i18n.getPropertyKeys().length, Is.is(2));
    assertThat(Lists.newArrayList(i18n.getPropertyKeys()), hasItems("one", "two"));
  }

  @Test
  public void shouldEncodeJavascriptValues() {
    String js = i18n.getJsDictionnary(bundle);
    assertThat(js, containsString("var l10n = {"));
    assertThat(js, containsString("one\": \"One"));
    assertThat(js, containsString("two\": \"Two"));
    assertThat(js, containsString("};"));
    assertThat(js, not(containsString(",};"))); // IE does not support empty key-values
  }
}
