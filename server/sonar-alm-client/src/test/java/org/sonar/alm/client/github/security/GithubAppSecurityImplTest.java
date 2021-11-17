/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.alm.client.github.security;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.io.IOException;
import java.security.spec.InvalidKeySpecException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Random;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.alm.client.github.config.GithubAppConfiguration;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(DataProviderRunner.class)
public class GithubAppSecurityImplTest {
  private Clock clock = Clock.fixed(Instant.ofEpochSecond(132_600_888L), ZoneId.systemDefault());
  private GithubAppSecurityImpl underTest = new GithubAppSecurityImpl(clock);

  @Test
  public void createAppToken_fails_with_IAE_if_privateKey_content_is_garbage() {
    String garbage = randomAlphanumeric(555);
    GithubAppConfiguration githubAppConfiguration = createAppConfigurationForPrivateKey(garbage);

    assertThatThrownBy(() -> underTest.createAppToken(githubAppConfiguration.getId(), githubAppConfiguration.getPrivateKey()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasRootCauseMessage("Failed to decode Github Application private key");

  }

  @Test
  public void createAppToken_fails_with_IAE_if_privateKey_PKCS8_content_is_missing_end_comment() {
    String incompletePrivateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
      "MIIEowIBAAKCAQEA6C29ZdvrwHOu7Eewv+xvUd4inCnACTzAHukHKTSY4R16+lRI\n" +
      "YC5qZ8Xo304J7lLhN4/d4Xnof3lDXZOHthVbJKik4fOuEGbTXTIcuFs3hdJtrJsb\n" +
      "antv8SOl5iR4fYRAf2AILMdtZI4iMSicBLIIttR+wVXo6NJYMjpj1OuAU3uN8eET\n" +
      "Gge09oJT3QOUBem7N8uaYi/p5uAfsf2/SVNsoMPV624X4kgNcyj/TMa6BosFJ8Y3\n" +
      "oeg0Aguk2yuHhAnixDVGoz6N7Go0QjEipVNix2JOOJwpFH4k2iZfM6n+8sJTLilq\n" +
      "yzT53JW/XI+M5AXVj4OjBJ/2yMPi3RFMNTdgRwIDAQABAoIBACcYBIsRI7oNAIgi\n" +
      "bh1y1y+mwpce5Inpo8PQovcKNy+4gguCg4lGZ34/sb1f64YoiGmNnOOpXj+QkIpC\n" +
      "HBjJscYTa2fsWwPB/Jb1qCZWnZu32eW1XEFqtWeaBAYjX/JqgV2xMs8vaTkEQbeb\n" +
      "SeH0hEkcsJcnOwdw247hjAu+96WWlyt10ZGgQaWPfXsdtelbaoaturNAVAJHdl9e\n" +
      "TIknCIbtLlbz/FtzjtCtdeiWr8gbKdVkshGtA8SKVhXGQwDwENjUkAUtSJ0aXR1t\n" +
      "+UjQcTISk7LiiYs0MrJ/CKoJ7mShwx7+YF3hgyqQ0qaqHwt9Yyd7wzWdCgdM5Eha\n" +
      "ccioIskCgYEA+EDJmcM5NGu5AYpZ1ogmG6jzsefAlr2NG1PQ/U03twal/B+ygAQb\n" +
      "5dholrq+aF+45Hrzfxije3Zrvpb08vxzKAs20lOlJsKftx2zkLR+mNvWTAORuO16\n" +
      "lG0c0cgYAKA1ld4R8KB8NmbuNb1w4LYZuyuFIEVmm2B3ca141WNHBwMCgYEA72yK\n" +
      "B4+xxomZn6dtbCGQZxziaI9WH/KEfDemKO5cfPlynQjmmMkiDpcyHa7mvdU+PGh3\n" +
      "g+OmQxORXMmBkHEnYS1fl3ac3U5sLiHAQBmTKKcLuVQlIU4oDu/K6WEGL9DdPtaK\n" +
      "gyOOWtSnfHTbT0bZ4IMm+gzdc4bCuEjvYyUhzG0CgYAEN011MAyTqFSvAwN9kjhb\n" +
      "deYVmmL57GQuF6FP+/S7RgChpIQqimdS4vb7wFYlfaKtNq1V9jwoh51S0kt8qO7n\n" +
      "ujEHJ2aBnwKJYJbBGV+hBvK/vbvG0TmotaWspmJJ+G6QigHx/Te+0Maw4PO+zTjo\n" +
      "pdeP8b3JW70LkC+iKBp3swKBgFL/nm32m1tHEjFtehpVHFkSg05Z+jJDATiKlhh0\n" +
      "YS2Vz+yuTDpE54CFW4M8wZKnXNbWJDBdd6KjIu42kKrA/zTJ5Ox92u1BJXFsk9fk\n" +
      "xcX++qp5iBGepXZgHEiBMQLcdgY1m3jQl6XXOGSFog0+c4NIE/f1A8PrwI7gAdSt\n" +
      "56SVAoGBAJp214Fo0oheMTTYKVtXuGiH/v3JNG1jKFgsmHqndf4wy7U6bbNctEzc\n" +
      "ZXNIacuhWmko6YejMrWNhE57sX812MhXGZq6y0sYZGKtp7oDv8G3rWD6bpZywpcV\n" +
      "kTtMJxm8J64u6bAkpWG3BocJP9qbXeAbILo1wuXgYqABBrpA9nnc";
    GithubAppConfiguration githubAppConfiguration = createAppConfigurationForPrivateKey(incompletePrivateKey);

    assertThatThrownBy(() -> underTest.createAppToken(githubAppConfiguration.getId(), githubAppConfiguration.getPrivateKey()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasRootCauseInstanceOf(IOException.class)
      .hasRootCauseMessage("-----END RSA PRIVATE KEY not found");
  }

  @Test
  public void createAppToken_fails_with_IAE_if_privateKey_PKCS8_content_is_corrupted() {
    String corruptedPrivateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
      "MIIEowIBAAKCAQEA6C29ZdvrwHOu7Eewv+xvUd4inCnACTzAHukHKTSY4R16+lRI\n" +
      "YC5qZ8Xo304J7lLhN4/d4Xnof3lDXZOHthVbJKik4fOuEGbTXTIcuFs3hdJtrJsb\n" +
      "antv8SOl5iR4fYRAf2AILMdtZI4iMSicBLIIttR+wVXo6NJYMjpj1OuAU3uN8eET\n" +
      "Gge09oJT3QOUBem7N8uaYi/p5uAfsf2/SVNsoMPV624X4kgNcyj/TMa6BosFJ8Y3\n" +
      "oeg0Aguk2yuHhAnixDVGoz6N7Go0QjEipVNix2JOOJwpFH4k2iZfM6n+8sJTLilq\n" +
      "yzT53JW/XI+M5AXVj4OjBJ/2yMPi3RFMNTdgRwIDAQABAoIBACcYBIsRI7oNAIgi\n" +
      "bh1y1y+mwpce5Inpo8PQovcKNy+4gguCg4lGZ34/sb1f64YoiGmNnOOpXj+QkIpC\n" +
      "HBjJscYTa2fsWwPB/Jb1qCZWnZu32eW1XEFqtWeaBAYjX/JqgV2xMs8vaTkEQbeb\n" +
      // "SeH0hEkcsJcnOwdw247hjAu+96WWlyt10ZGgQaWPfXsdtelbaoaturNAVAJHdl9e\n" +
      // "TIknCIbtLlbz/FtzjtCtdeiWr8gbKdVkshGtA8SKVhXGQwDwENjUkAUtSJ0aXR1t\n" +
      // "+UjQcTISk7LiiYs0MrJ/CKoJ7mShwx7+YF3hgyqQ0qaqHwt9Yyd7wzWdCgdM5Eha\n" +
      // "ccioIskCgYEA+EDJmcM5NGu5AYpZ1ogmG6jzsefAlr2NG1PQ/U03twal/B+ygAQb\n" +
      // "5dholrq+aF+45Hrzfxije3Zrvpb08vxzKAs20lOlJsKftx2zkLR+mNvWTAORuO16\n" +
      // "lG0c0cgYAKA1ld4R8KB8NmbuNb1w4LYZuyuFIEVmm2B3ca141WNHBwMCgYEA72yK\n" +
      // "B4+xxomZn6dtbCGQZxziaI9WH/KEfDemKO5cfPlynQjmmMkiDpcyHa7mvdU+PGh3\n" +
      "g+OmQxORXMmBkHEnYS1fl3ac3U5sLiHAQBmTKKcLuVQlIU4oDu/K6WEGL9DdPtaK\n" +
      "gyOOWtSnfHTbT0bZ4IMm+gzdc4bCuEjvYyUhzG0CgYAEN011MAyTqFSvAwN9kjhb\n" +
      "deYVmmL57GQuF6FP+/S7RgChpIQqimdS4vb7wFYlfaKtNq1V9jwoh51S0kt8qO7n\n" +
      "ujEHJ2aBnwKJYJbBGV+hBvK/vbvG0TmotaWspmJJ+G6QigHx/Te+0Maw4PO+zTjo\n" +
      "pdeP8b3JW70LkC+iKBp3swKBgFL/nm32m1tHEjFtehpVHFkSg05Z+jJDATiKlhh0\n" +
      "YS2Vz+yuTDpE54CFW4M8wZKnXNbWJDBdd6KjIu42kKrA/zTJ5Ox92u1BJXFsk9fk\n" +
      "xcX++qp5iBGepXZgHEiBMQLcdgY1m3jQl6XXOGSFog0+c4NIE/f1A8PrwI7gAdSt\n" +
      "56SVAoGBAJp214Fo0oheMTTYKVtXuGiH/v3JNG1jKFgsmHqndf4wy7U6bbNctEzc\n" +
      "ZXNIacuhWmko6YejMrWNhE57sX812MhXGZq6y0sYZGKtp7oDv8G3rWD6bpZywpcV\n" +
      "kTtMJxm8J64u6bAkpWG3BocJP9qbXeAbILo1wuXgYqABBrpA9nnc\n" +
      "-----END RSA PRIVATE KEY-----";
    GithubAppConfiguration githubAppConfiguration = createAppConfigurationForPrivateKey(corruptedPrivateKey);

    assertThatThrownBy(() -> underTest.createAppToken(githubAppConfiguration.getId(), githubAppConfiguration.getPrivateKey()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasCauseInstanceOf(InvalidKeySpecException.class);
  }

  @Test
  public void getApplicationJWTToken_throws_ISE_if_conf_is_not_complete() {
    GithubAppConfiguration githubAppConfiguration = createAppConfiguration(false);
    assertThatThrownBy(() -> underTest.createAppToken(githubAppConfiguration.getId(), githubAppConfiguration.getPrivateKey()))
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void getApplicationJWTToken_returns_token_if_app_config_and_private_key_are_valid() {
    GithubAppConfiguration githubAppConfiguration = createAppConfiguration(true);

    assertThat(underTest.createAppToken(githubAppConfiguration.getId(), githubAppConfiguration.getPrivateKey())).isNotNull();
  }

  private GithubAppConfiguration createAppConfiguration(boolean validConfiguration) {
    if (validConfiguration) {
      return createAppConfiguration();
    } else {
      return new GithubAppConfiguration(null, null, null);
    }
  }

  private GithubAppConfiguration createAppConfiguration() {
    return new GithubAppConfiguration(new Random().nextLong(), REAL_PRIVATE_KEY, randomAlphanumeric(5));
  }

  private GithubAppConfiguration createAppConfigurationForPrivateKey(String privateKey) {
    long applicationId = new Random().nextInt(654);
    return new GithubAppConfiguration(applicationId, privateKey, randomAlphabetic(8));
  }

  private static final String REAL_PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY-----\n" +
    "MIIEowIBAAKCAQEA6C29ZdvrwHOu7Eewv+xvUd4inCnACTzAHukHKTSY4R16+lRI\n" +
    "YC5qZ8Xo304J7lLhN4/d4Xnof3lDXZOHthVbJKik4fOuEGbTXTIcuFs3hdJtrJsb\n" +
    "antv8SOl5iR4fYRAf2AILMdtZI4iMSicBLIIttR+wVXo6NJYMjpj1OuAU3uN8eET\n" +
    "Gge09oJT3QOUBem7N8uaYi/p5uAfsf2/SVNsoMPV624X4kgNcyj/TMa6BosFJ8Y3\n" +
    "oeg0Aguk2yuHhAnixDVGoz6N7Go0QjEipVNix2JOOJwpFH4k2iZfM6n+8sJTLilq\n" +
    "yzT53JW/XI+M5AXVj4OjBJ/2yMPi3RFMNTdgRwIDAQABAoIBACcYBIsRI7oNAIgi\n" +
    "bh1y1y+mwpce5Inpo8PQovcKNy+4gguCg4lGZ34/sb1f64YoiGmNnOOpXj+QkIpC\n" +
    "HBjJscYTa2fsWwPB/Jb1qCZWnZu32eW1XEFqtWeaBAYjX/JqgV2xMs8vaTkEQbeb\n" +
    "SeH0hEkcsJcnOwdw247hjAu+96WWlyt10ZGgQaWPfXsdtelbaoaturNAVAJHdl9e\n" +
    "TIknCIbtLlbz/FtzjtCtdeiWr8gbKdVkshGtA8SKVhXGQwDwENjUkAUtSJ0aXR1t\n" +
    "+UjQcTISk7LiiYs0MrJ/CKoJ7mShwx7+YF3hgyqQ0qaqHwt9Yyd7wzWdCgdM5Eha\n" +
    "ccioIskCgYEA+EDJmcM5NGu5AYpZ1ogmG6jzsefAlr2NG1PQ/U03twal/B+ygAQb\n" +
    "5dholrq+aF+45Hrzfxije3Zrvpb08vxzKAs20lOlJsKftx2zkLR+mNvWTAORuO16\n" +
    "lG0c0cgYAKA1ld4R8KB8NmbuNb1w4LYZuyuFIEVmm2B3ca141WNHBwMCgYEA72yK\n" +
    "B4+xxomZn6dtbCGQZxziaI9WH/KEfDemKO5cfPlynQjmmMkiDpcyHa7mvdU+PGh3\n" +
    "g+OmQxORXMmBkHEnYS1fl3ac3U5sLiHAQBmTKKcLuVQlIU4oDu/K6WEGL9DdPtaK\n" +
    "gyOOWtSnfHTbT0bZ4IMm+gzdc4bCuEjvYyUhzG0CgYAEN011MAyTqFSvAwN9kjhb\n" +
    "deYVmmL57GQuF6FP+/S7RgChpIQqimdS4vb7wFYlfaKtNq1V9jwoh51S0kt8qO7n\n" +
    "ujEHJ2aBnwKJYJbBGV+hBvK/vbvG0TmotaWspmJJ+G6QigHx/Te+0Maw4PO+zTjo\n" +
    "pdeP8b3JW70LkC+iKBp3swKBgFL/nm32m1tHEjFtehpVHFkSg05Z+jJDATiKlhh0\n" +
    "YS2Vz+yuTDpE54CFW4M8wZKnXNbWJDBdd6KjIu42kKrA/zTJ5Ox92u1BJXFsk9fk\n" +
    "xcX++qp5iBGepXZgHEiBMQLcdgY1m3jQl6XXOGSFog0+c4NIE/f1A8PrwI7gAdSt\n" +
    "56SVAoGBAJp214Fo0oheMTTYKVtXuGiH/v3JNG1jKFgsmHqndf4wy7U6bbNctEzc\n" +
    "ZXNIacuhWmko6YejMrWNhE57sX812MhXGZq6y0sYZGKtp7oDv8G3rWD6bpZywpcV\n" +
    "kTtMJxm8J64u6bAkpWG3BocJP9qbXeAbILo1wuXgYqABBrpA9nnc\n" +
    "-----END RSA PRIVATE KEY-----";
}
