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
package org.sonar.tests.integration;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class GzipCompressionTest {

  private HttpClient client;
  private HttpMethod method;

  @Before
  public void before() {
    client = new HttpClient();
    method = new GetMethod(ITUtils.getSonarURL());
  }

  @After
  public void after(){
    method.releaseConnection();
  }

  @Test
  public void responseShouldBeGzipped() throws IOException {
    client.executeMethod(method);
    int sizeWithoutGzip = method.getResponseBodyAsString().length();
    assertThat(sizeWithoutGzip, greaterThan(0));
    assertThat(method.getResponseHeader("Content-Encoding"), nullValue());
        
    method.setRequestHeader("Accept-Encoding", "gzip, deflate");
    client.executeMethod(method);
    int sizeWithGzip = method.getResponseBodyAsString().length();
    assertThat(sizeWithGzip, greaterThan(0));
    assertThat(method.getResponseHeader("Content-Encoding").getValue(), is("gzip"));

    assertThat(sizeWithGzip, lessThan(sizeWithoutGzip));
  }

}