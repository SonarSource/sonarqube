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
package org.sonar.api.charts;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.*;

public class ChartParametersTest {
  @Test
  public void shouldForbidHighSizeForSecurityReasons() {
    String url = ChartParameters.PARAM_WIDTH + "=100000&" + ChartParameters.PARAM_HEIGHT + "=9999999";
    ChartParameters params = new ChartParameters(url);
    assertEquals(ChartParameters.MAX_WIDTH, params.getWidth());
    assertEquals(ChartParameters.MAX_HEIGHT, params.getHeight());
  }

  @Test
  public void shouldReadImageSizeFromParameters() {
    String url = ChartParameters.PARAM_WIDTH + "=200&" + ChartParameters.PARAM_HEIGHT + "=300";
    ChartParameters params = new ChartParameters(url);
    assertEquals(200, params.getWidth());
    assertEquals(300, params.getHeight());
  }

  @Test
  public void shouldGetDefaultSizesIfNoParameters() {
    ChartParameters params = new ChartParameters("foo=bar");
    assertEquals(ChartParameters.DEFAULT_WIDTH, params.getWidth());
    assertEquals(ChartParameters.DEFAULT_HEIGHT, params.getHeight());
  }

  @Test
  public void shouldDecodeValue() {
    ChartParameters params = new ChartParameters("foo=0%3D10,3%3D8");
    assertEquals("0=10,3=8", params.getValue("foo", "", true));
    assertEquals("0%3D10,3%3D8", params.getValue("foo"));
    assertNull(params.getValue("bar", null, true));
  }

  @Test
  public void shouldDecodeValues() {
    ChartParameters params = new ChartParameters("foo=0%3D10,3%3D8|5%3D5,7%3D17");
    assertArrayEquals(new String[]{"0%3D10,3%3D8", "5%3D5,7%3D17"}, params.getValues("foo", "|"));
    assertArrayEquals(new String[]{"0=10,3=8", "5=5,7=17"}, params.getValues("foo", "|", true));
    assertArrayEquals(new String[0], params.getValues("bar", "|", true));
  }

  @Test
  public void getLocale() {
    ChartParameters params = new ChartParameters("foo=0&locale=fr");
    assertEquals(Locale.FRENCH, params.getLocale());

    params = new ChartParameters("foo=0&locale=fr-CH");
    assertEquals("fr-ch", params.getLocale().getLanguage());

    params = new ChartParameters("foo=0");
    assertEquals(Locale.ENGLISH, params.getLocale());
  }
}
