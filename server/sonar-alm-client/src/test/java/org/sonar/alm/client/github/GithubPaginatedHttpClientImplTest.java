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
package org.sonar.alm.client.github;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.event.Level;
import org.sonar.alm.client.github.security.AccessToken;
import org.sonar.api.testfixtures.log.LogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.alm.client.github.ApplicationHttpClient.GetResponse;

@RunWith(MockitoJUnitRunner.class)
public class GithubPaginatedHttpClientImplTest {

  private static final String APP_URL = "https://github.com/";

  private static final String ENDPOINT = "/test-endpoint";

  private static final Type STRING_LIST_TYPE = TypeToken.getParameterized(List.class, String.class).getType();

  private Gson gson = new Gson();

  @Rule
  public LogTester logTester = new LogTester();

  @Mock
  private AccessToken accessToken;

  @Mock
  RatioBasedRateLimitChecker rateLimitChecker;

  @Mock
  ApplicationHttpClient appHttpClient;

  @InjectMocks
  private GithubPaginatedHttpClient underTest;

  @Test
  public void get_whenNoPagination_ReturnsCorrectResponse() throws IOException {

    GetResponse response = mockResponseWithoutPagination("[\"result1\", \"result2\"]");
    when(appHttpClient.get(APP_URL, accessToken, ENDPOINT + "?per_page=100")).thenReturn(response);

    List<String> results = underTest.get(APP_URL, accessToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE));

    assertThat(results)
      .containsExactly("result1", "result2");
  }

  @Test
  public void get_whenEndpointAlreadyContainsPathParameter_shouldAddANewParameter() throws IOException {
    ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

    GetResponse response = mockResponseWithoutPagination("[\"result1\", \"result2\"]");
    when(appHttpClient.get(eq(APP_URL), eq(accessToken), urlCaptor.capture())).thenReturn(response);

    underTest.get(APP_URL, accessToken, ENDPOINT + "?alreadyExistingArg=2", result -> gson.fromJson(result, STRING_LIST_TYPE));

    assertThat(urlCaptor.getValue()).isEqualTo(ENDPOINT + "?alreadyExistingArg=2&per_page=100");
  }

  private static GetResponse mockResponseWithoutPagination(String content) {
    GetResponse response = mock(GetResponse.class);
    when(response.getCode()).thenReturn(200);
    when(response.getContent()).thenReturn(Optional.of(content));
    return response;
  }

  @Test
  public void get_whenPaginationAndRateLimiting_returnsResponseFromAllPages() throws IOException, InterruptedException {
    GetResponse response1 = mockResponseWithPaginationAndRateLimit("[\"result1\", \"result2\"]", "/next-endpoint");
    GetResponse response2 = mockResponseWithoutPagination("[\"result3\"]");
    when(appHttpClient.get(APP_URL, accessToken, ENDPOINT + "?per_page=100")).thenReturn(response1);
    when(appHttpClient.get(APP_URL, accessToken, "/next-endpoint")).thenReturn(response2);

    List<String> results = underTest.get(APP_URL, accessToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE));

    assertThat(results)
      .containsExactly("result1", "result2", "result3");

    ArgumentCaptor<ApplicationHttpClient.RateLimit> rateLimitRecordCaptor = ArgumentCaptor.forClass(ApplicationHttpClient.RateLimit.class);
    verify(rateLimitChecker).checkRateLimit(rateLimitRecordCaptor.capture());
    ApplicationHttpClient.RateLimit rateLimitRecord = rateLimitRecordCaptor.getValue();
    assertThat(rateLimitRecord.limit()).isEqualTo(10);
    assertThat(rateLimitRecord.remaining()).isEqualTo(1);
    assertThat(rateLimitRecord.reset()).isZero();
  }

  private static GetResponse mockResponseWithPaginationAndRateLimit(String content, String nextEndpoint) {
    GetResponse response = mockResponseWithoutPagination(content);
    when(response.getCode()).thenReturn(200);
    when(response.getNextEndPoint()).thenReturn(Optional.of(nextEndpoint));
    when(response.getRateLimit()).thenReturn(new ApplicationHttpClient.RateLimit(1, 10, 0L));
    return response;
  }

  @Test
  public void get_whenGitHubReturnsNonSuccessCode_shouldThrow() throws IOException {
    GetResponse response1 = mockResponseWithPaginationAndRateLimit("[\"result1\", \"result2\"]", "/next-endpoint");
    GetResponse response2 = mockFailedResponse("failed");
    when(appHttpClient.get(APP_URL, accessToken, ENDPOINT + "?per_page=100")).thenReturn(response1);
    when(appHttpClient.get(APP_URL, accessToken, "/next-endpoint")).thenReturn(response2);

    assertThatIllegalStateException()
      .isThrownBy(() -> underTest.get(APP_URL, accessToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE)))
      .withMessage("Error while executing a call to GitHub. Return code 400. Error message: failed.");
  }

  private static GetResponse mockFailedResponse(String content) {
    GetResponse response = mock(GetResponse.class);
    when(response.getCode()).thenReturn(400);
    when(response.getContent()).thenReturn(Optional.of(content));
    return response;
  }

  @Test
  public void getRepositoryTeams_whenRateLimitCheckerThrowsInterruptedException_shouldSucceed() throws IOException, InterruptedException {
    GetResponse response1 = mockResponseWithPaginationAndRateLimit("[\"result1\", \"result2\"]", "/next-endpoint");
    GetResponse response2 = mockResponseWithoutPagination("[\"result3\"]");
    when(appHttpClient.get(APP_URL, accessToken, ENDPOINT + "?per_page=100")).thenReturn(response1);
    when(appHttpClient.get(APP_URL, accessToken, "/next-endpoint")).thenReturn(response2);
    doThrow(new InterruptedException("interrupted")).when(rateLimitChecker).checkRateLimit(any(ApplicationHttpClient.RateLimit.class));

    assertThatNoException()
      .isThrownBy(() -> underTest.get(APP_URL, accessToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE)));

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(Level.WARN))
      .containsExactly("Thread interrupted: interrupted");
  }
}
