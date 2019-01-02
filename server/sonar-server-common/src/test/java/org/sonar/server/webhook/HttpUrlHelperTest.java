/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.webhook;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import okhttp3.HttpUrl;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.apache.commons.lang.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.webhook.HttpUrlHelper.obfuscateCredentials;

@RunWith(DataProviderRunner.class)
public class HttpUrlHelperTest {

  @Test
  @UseDataProvider("obfuscateCredentialsUseCases")
  public void verify_obfuscateCredentials(String originalUrl, String expectedUrl) {
    assertThat(obfuscateCredentials(originalUrl, HttpUrl.parse(originalUrl)))
      .isEqualTo(obfuscateCredentials(originalUrl))
      .isEqualTo(expectedUrl);
  }

  @DataProvider
  public static Object[][] obfuscateCredentialsUseCases() {
    List<Object[]> rows = new ArrayList<>();
    for (String before : Arrays.asList("http://", "https://")) {
      for (String host : Arrays.asList("foo", "127.0.0.1", "[2001:db8:85a3:0:0:8a2e:370:7334]", "[2001:0db8:85a3:0000:0000:8a2e:0370:7334]")) {
        for (String port : Arrays.asList("", ":123")) {
          for (String after : Arrays.asList("", "/", "/bar", "/bar/", "?", "?a=b", "?a=b&c=d")) {
            for (String username : Arrays.asList("", "us", "a b", "a%20b")) {
              for (String password : Arrays.asList("", "pwd", "pwd%20k", "pwd k", "c:d")) {
                if (username.isEmpty()) {
                  String url = before + host + port + after;
                  rows.add(new Object[] {url, url});
                } else if (password.isEmpty()) {
                  String url = before + username + '@' + host + port + after;
                  String expected = before + repeat("*", username.length()) + '@' + host + port + after;
                  rows.add(new Object[] {url, expected});
                } else {
                  String url = before + username + ':' + password + '@' + host + port + after;
                  String expected = before + repeat("*", username.length()) + ':' + repeat("*", password.length()) + '@' + host + port + after;
                  rows.add(new Object[] {url, expected});
                }
              }
            }
          }
        }
      }
    }
    return rows.toArray(new Object[0][]);
  }

}
