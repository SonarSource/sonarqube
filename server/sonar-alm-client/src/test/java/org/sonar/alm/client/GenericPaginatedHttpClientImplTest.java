/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.alm.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHRateLimit;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.auth.github.AppInstallationToken;
import org.sonar.auth.github.security.AccessToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.alm.client.ApplicationHttpClient.GetResponse;

@RunWith(MockitoJUnitRunner.class)
public class GenericPaginatedHttpClientImplTest {

  private static final String APP_URL = "https://github.com/";

  private static final String ENDPOINT = "/test-endpoint";

  private static final TypeToken<List<String>> STRING_LIST_TYPE = new TypeToken<>() {
  };

  private Gson gson = new Gson();

  @Rule
  public LogTester logTester = new LogTester();

  @Mock
  private AccessToken accessToken;

  @Mock
  private AppInstallationToken installationToken;

  @Mock
  RatioBasedRateLimitChecker rateLimitChecker;

  @Mock
  ApplicationHttpClient appHttpClient;

  @InjectMocks
  private TestPaginatedHttpClient underTest;

  private static class TestPaginatedHttpClient extends GenericPaginatedHttpClient {
    protected TestPaginatedHttpClient(ApplicationHttpClient appHttpClient, RatioBasedRateLimitChecker rateLimitChecker) {
      super(appHttpClient, rateLimitChecker);
    }
  }

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

  @Test
  public void getStrict_whenAllPagesHaveContent_returnsResponseFromAllPages() throws IOException {
    GetResponse firstPage = mockResponseWithPagination("[\"result1\"]", "/next-endpoint");
    GetResponse lastPage = mockResponseWithoutPagination("[\"result2\"]");
    when(appHttpClient.get(APP_URL, accessToken, ENDPOINT + "?per_page=100")).thenReturn(firstPage);
    when(appHttpClient.get(APP_URL, accessToken, "/next-endpoint")).thenReturn(lastPage);

    List<String> results = underTest.getStrict(APP_URL, accessToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE));

    assertThat(results).containsExactly("result1", "result2");
  }

  @Test
  public void getStrict_whenALaterPageHasNoContent_throwsInsteadOfReturningAShortList() throws IOException {
    GetResponse firstPage = mockResponseWithPagination("[\"result1\"]", "/next-endpoint");
    GetResponse bodilessPage = mock(GetResponse.class);
    when(bodilessPage.getCode()).thenReturn(200);
    when(bodilessPage.getContent()).thenReturn(Optional.empty());
    when(appHttpClient.get(APP_URL, accessToken, ENDPOINT + "?per_page=100")).thenReturn(firstPage);
    when(appHttpClient.get(APP_URL, accessToken, "/next-endpoint")).thenReturn(bodilessPage);

    assertThatIllegalStateException()
      .isThrownBy(() -> underTest.getStrict(APP_URL, accessToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE)))
      .withMessageContaining("came back without a body");
  }

  @Test
  public void get_whenAPageHasNoContent_stillSkipsIt() throws IOException {
    // The tolerant behavior getStrict() opts out of: unchanged for every existing caller.
    GetResponse firstPage = mockResponseWithPagination("[\"result1\"]", "/next-endpoint");
    GetResponse bodilessPage = mock(GetResponse.class);
    when(bodilessPage.getCode()).thenReturn(200);
    when(bodilessPage.getContent()).thenReturn(Optional.empty());
    when(appHttpClient.get(APP_URL, accessToken, ENDPOINT + "?per_page=100")).thenReturn(firstPage);
    when(appHttpClient.get(APP_URL, accessToken, "/next-endpoint")).thenReturn(bodilessPage);

    List<String> results = underTest.get(APP_URL, accessToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE));

    assertThat(results).containsExactly("result1");
  }

  private static GetResponse mockResponseWithPagination(String content, String nextEndpoint) {
    GetResponse response = mockResponseWithoutPagination(content);
    when(response.getNextEndPoint()).thenReturn(Optional.of(nextEndpoint));
    return response;
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

    ArgumentCaptor<GHRateLimit.Record> rateLimitRecordCaptor = ArgumentCaptor.forClass(GHRateLimit.Record.class);
    verify(rateLimitChecker).checkRateLimit(rateLimitRecordCaptor.capture(), anyLong());
    GHRateLimit.Record rateLimitRecord = rateLimitRecordCaptor.getValue();
    assertThat(rateLimitRecord.getLimit()).isEqualTo(10);
    assertThat(rateLimitRecord.getRemaining()).isEqualTo(1);
    assertThat(rateLimitRecord.getResetEpochSeconds()).isZero();
  }

  private static GetResponse mockResponseWithPaginationAndRateLimit(String content, String nextEndpoint) {
    GetResponse response = mockResponseWithoutPagination(content);
    when(response.getCode()).thenReturn(200);
    when(response.getNextEndPoint()).thenReturn(Optional.of(nextEndpoint));
    when(response.getRateLimit()).thenReturn(new ApplicationHttpClient.RateLimit(1, 10, 0L));
    return response;
  }

  @Test
  public void get_withInstallationToken_checksFirstRequestOfNextCallAgainstLastObservedRateLimit() throws IOException, InterruptedException {
    long futureReset = futureReset();
    when(installationToken.getInstallationId()).thenReturn("installation-1");
    GetResponse first = mockResponseWithRateLimit("[\"result1\"]", new ApplicationHttpClient.RateLimit(100, 1000, futureReset));
    GetResponse second = mockResponseWithRateLimit("[\"result2\"]", new ApplicationHttpClient.RateLimit(99, 1000, futureReset));
    when(appHttpClient.get(APP_URL, installationToken, ENDPOINT + "?per_page=100")).thenReturn(first, second);

    underTest.get(APP_URL, installationToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE));
    verifyNoInteractions(rateLimitChecker);
    underTest.get(APP_URL, installationToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE));

    ArgumentCaptor<GHRateLimit.Record> recordCaptor = ArgumentCaptor.forClass(GHRateLimit.Record.class);
    verify(rateLimitChecker).checkRateLimit(recordCaptor.capture(), anyLong());
    assertThat(recordCaptor.getValue().getRemaining()).isEqualTo(100);
    assertThat(recordCaptor.getValue().getLimit()).isEqualTo(1000);
    assertThat(recordCaptor.getValue().getResetEpochSeconds()).isEqualTo(futureReset);
  }

  @Test
  public void get_withInstallationToken_ignoresRateLimitWhoseWindowAlreadyReset() throws IOException {
    long pastReset = System.currentTimeMillis() / 1000 - 10;
    when(installationToken.getInstallationId()).thenReturn("installation-1");
    GetResponse first = mockResponseWithRateLimit("[\"result1\"]", new ApplicationHttpClient.RateLimit(0, 1000, pastReset));
    GetResponse second = mockResponseWithRateLimit("[\"result2\"]", new ApplicationHttpClient.RateLimit(999, 1000, futureReset()));
    when(appHttpClient.get(APP_URL, installationToken, ENDPOINT + "?per_page=100")).thenReturn(first, second);

    underTest.get(APP_URL, installationToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE));
    underTest.get(APP_URL, installationToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE));

    verifyNoInteractions(rateLimitChecker);
  }

  @Test
  public void get_withInstallationToken_tracksTokensSeparately() throws IOException {
    AppInstallationToken otherToken = mock(AppInstallationToken.class);
    when(installationToken.getInstallationId()).thenReturn("installation-1");
    when(otherToken.getInstallationId()).thenReturn("installation-2");
    GetResponse first = mockResponseWithRateLimit("[\"result1\"]", new ApplicationHttpClient.RateLimit(1, 1000, futureReset()));
    GetResponse second = mockResponseWithRateLimit("[\"result2\"]", new ApplicationHttpClient.RateLimit(500, 1000, futureReset()));
    when(appHttpClient.get(APP_URL, installationToken, ENDPOINT + "?per_page=100")).thenReturn(first);
    when(appHttpClient.get(APP_URL, otherToken, ENDPOINT + "?per_page=100")).thenReturn(second);

    underTest.get(APP_URL, installationToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE));
    underTest.get(APP_URL, otherToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE));

    verifyNoInteractions(rateLimitChecker);
  }

  @Test
  public void get_withInstallationToken_keepsRateLimitAcrossTokenRefresh() throws IOException, InterruptedException {
    AppInstallationToken refreshedToken = mock(AppInstallationToken.class);
    when(installationToken.getInstallationId()).thenReturn("installation-1");
    when(refreshedToken.getInstallationId()).thenReturn("installation-1");
    GetResponse first = mockResponseWithRateLimit("[\"result1\"]", new ApplicationHttpClient.RateLimit(1, 1000, futureReset()));
    GetResponse second = mockResponseWithRateLimit("[\"result2\"]", new ApplicationHttpClient.RateLimit(0, 1000, futureReset()));
    when(appHttpClient.get(APP_URL, installationToken, ENDPOINT + "?per_page=100")).thenReturn(first);
    when(appHttpClient.get(APP_URL, refreshedToken, ENDPOINT + "?per_page=100")).thenReturn(second);

    underTest.get(APP_URL, installationToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE));
    underTest.get(APP_URL, refreshedToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE));

    verify(rateLimitChecker).checkRateLimit(any(GHRateLimit.Record.class), anyLong());
  }

  @Test
  public void get_withInstallationToken_tracksSameInstallationIdOnDifferentUrlsSeparately() throws IOException {
    when(installationToken.getInstallationId()).thenReturn("installation-1");
    GetResponse first = mockResponseWithRateLimit("[\"result1\"]", new ApplicationHttpClient.RateLimit(1, 1000, futureReset()));
    GetResponse second = mockResponseWithRateLimit("[\"result2\"]", new ApplicationHttpClient.RateLimit(500, 1000, futureReset()));
    when(appHttpClient.get(APP_URL, installationToken, ENDPOINT + "?per_page=100")).thenReturn(first);
    when(appHttpClient.get("https://other-github.com/", installationToken, ENDPOINT + "?per_page=100")).thenReturn(second);

    underTest.get(APP_URL, installationToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE));
    underTest.get("https://other-github.com/", installationToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE));

    verifyNoInteractions(rateLimitChecker);
  }

  @Test
  public void get_withInstallationToken_whenResponseHasNoRateLimit_doesNotCheckNextCall() throws IOException {
    when(installationToken.getInstallationId()).thenReturn("installation-1");
    GetResponse response = mockResponseWithoutPagination("[\"result1\"]");
    when(appHttpClient.get(APP_URL, installationToken, ENDPOINT + "?per_page=100")).thenReturn(response);

    underTest.get(APP_URL, installationToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE));
    underTest.get(APP_URL, installationToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE));

    verifyNoInteractions(rateLimitChecker);
  }

  @Test
  public void get_withTokenThatIsNotAnInstallationToken_doesNotShareRateLimitBetweenCalls() throws IOException {
    GetResponse response = mockResponseWithRateLimit("[\"result1\"]", new ApplicationHttpClient.RateLimit(1, 1000, futureReset()));
    when(appHttpClient.get(APP_URL, accessToken, ENDPOINT + "?per_page=100")).thenReturn(response);

    underTest.get(APP_URL, accessToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE));
    underTest.get(APP_URL, accessToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE));

    verifyNoInteractions(rateLimitChecker);
  }

  private static long futureReset() {
    return System.currentTimeMillis() / 1000 + 3600;
  }

  private static GetResponse mockResponseWithRateLimit(String content, ApplicationHttpClient.RateLimit rateLimit) {
    GetResponse response = mockResponseWithoutPagination(content);
    when(response.getRateLimit()).thenReturn(rateLimit);
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
      .withMessage("SonarQube was not able to retrieve resources from external system. Error while executing a paginated call to https://github.com/, endpoint:/next-endpoint. "
        + "Error while executing a call to https://github.com/. Return code 400. Error message: failed.");
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
    doThrow(new InterruptedException("interrupted")).when(rateLimitChecker).checkRateLimit(any(GHRateLimit.Record.class), anyLong());

    assertThatNoException()
      .isThrownBy(() -> underTest.get(APP_URL, accessToken, ENDPOINT, result -> gson.fromJson(result, STRING_LIST_TYPE)));

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(Level.WARN))
      .containsExactly("Thread interrupted: interrupted");
  }

  @Test
  public void getRepositoryCollaborators_whenDevOpsPlatformCallThrowsIOException_shouldLogAndReThrow() throws IOException {
    when(appHttpClient.get(APP_URL, accessToken, "query?per_page=100")).thenThrow(new IOException("error"));

    assertThatIllegalStateException()
      .isThrownBy(() -> underTest.get(APP_URL, accessToken, "query", mock()))
      .isInstanceOf(IllegalStateException.class)
      .withMessage("SonarQube was not able to retrieve resources from external system. Error while executing a paginated call to https://github.com/, "
        + "endpoint:query?per_page=100. error");

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(Level.WARN))
      .containsExactly("SonarQube was not able to retrieve resources from external system. "
        + "Error while executing a paginated call to https://github.com/, endpoint:query?per_page=100.");
  }
}
