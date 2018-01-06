/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonarqube.ws.client;

import com.google.common.base.Joiner;
import com.google.protobuf.Parser;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.MapEntry;
import org.junit.rules.ExternalResource;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

/**
 * Convenient rule to test a subclass of {@link BaseService}.
 *
 * <p>
 * Declaration sample:
 * <pre>
 * {@literal @}Rule
 * public ServiceTester<PermissionsService> serviceTester = new ServiceTester&lt;&gt;(new PermissionsService(mock(WsConnector.class)));
 *
 * private PermissionsService underTest = serviceTester.getInstanceUnderTest();
 * </pre>
 * </p>
 *
 * <p>
 * Method {@link #getInstanceUnderTest()} will return an instance of the class under test which will be instrumented
 * and will allow recording internal calls to {@link BaseService#call(BaseRequest, Parser)} and
 * {@link BaseService#call(WsRequest)}.
 * </p>
 * <p>
 * Argument of calls to these method will be logged and can be accessed through {@link #getGetCalls()}, {@link #getPostCalls()}
 * and {@link #getRawCalls()} depending on whether they are made respectively with {@link GetRequest}, {@link PostRequest}
 * or other subclass of {@link BaseRequest}.
 * </p>
 * <p>
 * For convenience, when one is testing a single Ws call, on case use {@link #getGetRequest()} (and its associated
 * {@link #getGetParser()}) or {@link #getPostRequest()} (and its associated {@link #getPostParser()}). Those three
 * method will make the appropriate assertions assuming that only a single GET (or POST) request has been made.
 * </p>
 * <p>
 * Last but not least, to easily verify the content of a {@link GetRequest} (or a {@link PostRequest}), one can use
 * methods {@link #assertThat(GetRequest)} (or {@link #assertThat(PostRequest)}) to write assertions on a
 * {@link GetRequest} (or {@link PostRequest}) returned by methods of this Rule.
 * </p>
 *
 * <p>
 * Assertion usage sample:
 * <pre>
 * PostRequest postRequest = serviceTester.getPostRequest();
 * serviceTester.assertThat(postRequest)
 * .hasPath("add_group")
 * .hasParam(PARAM_PERMISSION, PERMISSION_VALUE)
 * .hasParam(PARAM_PROJECT_ID, PROJECT_ID_VALUE)
 * .hasParam(PARAM_PROJECT_KEY, PROJECT_KEY_VALUE)
 * .hasParam(PARAM_GROUP_ID, GROUP_ID_VALUE)
 * .hasParam(PARAM_GROUP_NAME, GROUP_NAME_VALUE)
 * .andNoOtherParam();
 * </pre>
 * </p>
 *
 */
public class ServiceTester<T extends BaseService> extends ExternalResource {
  private static final Joiner COMMA_JOINER = Joiner.on(",");

  private final T underTest;
  private final List<GetCall> getCalls = new ArrayList<>();
  private final List<PostCall> postCalls = new ArrayList<>();
  private final List<RawCall> rawCalls = new ArrayList<>();

  /**
   * @param underTestInstance an instance of the type to test. Use {@link #getInstanceUnderTest()} to retrieve the
   *                          instrumented instance to use in your test.
   */
  public ServiceTester(T underTestInstance) {
    this.underTest = spy(underTestInstance);
  }

  @Override
  protected void before() throws Throwable {
    Answer<Object> answer = new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Object[] arguments = invocation.getArguments();
        Object request = arguments[0];
        Parser<?> parser = arguments.length == 2 ? (Parser<?>) arguments[1] : null;
        if (request instanceof PostRequest) {
          postCalls.add(new PostCall((PostRequest) request, parser));
        } else if (request instanceof GetRequest) {
          getCalls.add(new GetCall((GetRequest) request, parser));
        } else {
          rawCalls.add(new RawCall((WsRequest) request));
        }
        return null;
      }
    };
    doAnswer(answer).when(this.underTest).call(any(GetRequest.class), any(Parser.class));
    doAnswer(answer).when(this.underTest).call(any(WsRequest.class));
  }

  @Override
  protected void after() {
    this.getCalls.clear();
  }

  public T getInstanceUnderTest() {
    return underTest;
  }

  public List<GetCall> getGetCalls() {
    return getCalls;
  }

  @CheckForNull
  public GetRequest getGetRequest() {
    assertSingleGetCall();
    return getCalls.iterator().next().getRequest();
  }

  public RequestAssert<GetRequest> assertThat(GetRequest getRequest) {
    return new RequestAssert<>(getRequest);
  }

  public RequestAssert<PostRequest> assertThat(PostRequest postRequest) {
    return new RequestAssert<>(postRequest);
  }

  @CheckForNull
  public Parser<?> getGetParser() {
    assertSingleGetCall();
    return getCalls.iterator().next().getParser();
  }

  public List<PostCall> getPostCalls() {
    return postCalls;
  }

  public PostRequest getPostRequest() {
    assertSinglePostCall();
    return postCalls.iterator().next().getRequest();
  }

  @CheckForNull
  public Parser<?> getPostParser() {
    assertSinglePostCall();
    return postCalls.iterator().next().getParser();
  }

  private void assertSingleGetCall() {
    Assertions.assertThat(getCalls).hasSize(1);
    Assertions.assertThat(postCalls).isEmpty();
    Assertions.assertThat(rawCalls).isEmpty();
  }

  private void assertSinglePostCall() {
    Assertions.assertThat(postCalls).hasSize(1);
    Assertions.assertThat(getRawCalls()).isEmpty();
    Assertions.assertThat(rawCalls).isEmpty();
  }

  public List<RawCall> getRawCalls() {
    return rawCalls;
  }

  @Immutable
  public static final class GetCall extends CallWithParser<GetRequest> {

    public GetCall(GetRequest getRequest, @Nullable Parser<?> parser) {
      super(getRequest, parser);
    }

  }

  @Immutable
  public static final class PostCall extends CallWithParser<PostRequest> {

    public PostCall(PostRequest postRequest, @Nullable Parser<?> parser) {
      super(postRequest, parser);
    }
  }

  @Immutable
  public static final class RawCall {
    private final WsRequest wsRequest;

    public RawCall(WsRequest wsRequest) {
      this.wsRequest = wsRequest;
    }

    public WsRequest getWsRequest() {
      return wsRequest;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      RawCall rawCalls = (RawCall) o;
      return Objects.equals(wsRequest, rawCalls.wsRequest);
    }

    @Override
    public int hashCode() {
      return Objects.hash(wsRequest);
    }
  }

  public static abstract class CallWithParser<T extends BaseRequest<T>> {
    private final T request;
    private final Parser<?> parser;

    public CallWithParser(T request, @Nullable Parser<?> parser) {
      this.request = request;
      this.parser = parser;
    }

    public T getRequest() {
      return request;
    }

    public Parser<?> getParser() {
      return parser;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CallWithParser getCall = (CallWithParser) o;
      return Objects.equals(request, getCall.request) &&
        Objects.equals(parser, getCall.request);
    }

    @Override
    public int hashCode() {
      return Objects.hash(request, parser);
    }
  }

  public final class RequestAssert<T extends BaseRequest<T>> extends AbstractAssert<RequestAssert<T>, BaseRequest<T>> {
    private final List<MapEntry<String, String>> assertedParams = new ArrayList<>();

    protected RequestAssert(T actual) {
      super(actual, RequestAssert.class);
    }

    public RequestAssert hasPath(String path) {
      isNotNull();

      String expectedPath = underTest.controller + "/" + path;
      if (!Objects.equals(actual.getPath(), expectedPath)) {
        failWithMessage("Expected path to be <%s> but was <%s>", expectedPath, actual.getPath());
      }

      return this;
    }

    public RequestAssert hasParam(String key, String value) {
      isNotNull();

      MapEntry<String, String> entry = MapEntry.entry(key, value);
      Assertions.assertThat(actual.getParams()).contains(entry);
      this.assertedParams.add(entry);

      return this;
    }

    public RequestAssert hasParam(String key, int value) {
      isNotNull();

      MapEntry<String, String> entry = MapEntry.entry(key, String.valueOf(value));
      Assertions.assertThat(actual.getParams()).contains(entry);
      this.assertedParams.add(entry);

      return this;
    }

    public RequestAssert hasParam(String key, boolean value) {
      isNotNull();

      MapEntry<String, String> entry = MapEntry.entry(key, String.valueOf(value));
      Assertions.assertThat(actual.getParams()).contains(entry);
      this.assertedParams.add(entry);

      return this;
    }

    public RequestAssert hasParam(String key, List<String> values) {
      isNotNull();

      MapEntry<String, String> entry = MapEntry.entry(key, values.toString());
      Assertions.assertThat(actual.getParameters().getValues(key)).containsExactly(values.toArray(new String[0]));
      this.assertedParams.add(entry);

      return this;
    }

    public RequestAssert andNoOtherParam() {
      isNotNull();

      Assertions.assertThat(actual.getParams()).hasSize(assertedParams.size());

      return this;
    }

  }
}
