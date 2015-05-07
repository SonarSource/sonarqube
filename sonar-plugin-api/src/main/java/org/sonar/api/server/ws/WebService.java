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
package org.sonar.api.server.ws;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.ServerSide;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines a web service. Note that contrary to the deprecated {@link org.sonar.api.web.Webservice}
 * the ws is fully implemented in Java and does not require any Ruby on Rails code.
 * <p/>
 * <p/>
 * The classes implementing this extension point must be declared in {@link org.sonar.api.SonarPlugin#getExtensions()}.
 * <p/>
 * <h3>How to use</h3>
 * <pre>
 * public class HelloWs implements WebService {
 *   {@literal @}Override
 *   public void define(Context context) {
 *     NewController controller = context.createController("api/hello");
 *     controller.setDescription("Web service example");
 *
 *     // create the URL /api/hello/show
 *     controller.createAction("show")
 *       .setDescription("Entry point")
 *       .setHandler(new RequestHandler() {
 *         {@literal @}Override
 *         public void handle(Request request, Response response) {
 *           // read request parameters and generates response output
 *           response.newJsonWriter()
 *             .prop("hello", request.mandatoryParam("key"))
 *             .close();
 *         }
 *      })
 *      .createParam("key").setDescription("Example key").setRequired(true);
 *
 *    // important to apply changes
 *    controller.done();
 *   }
 * }
 * </pre>
 * <h3>How to test</h3>
 * <pre>
 * public class HelloWsTest {
 *   WebService ws = new HelloWs();
 *
 *   {@literal @}Test
 *   public void should_define_ws() throws Exception {
 *     // WsTester is available in the Maven artifact org.codehaus.sonar:sonar-testing-harness
 *     WsTester tester = new WsTester(ws);
 *     WebService.Controller controller = tester.controller("api/hello");
 *     assertThat(controller).isNotNull();
 *     assertThat(controller.path()).isEqualTo("api/hello");
 *     assertThat(controller.description()).isNotEmpty();
 *     assertThat(controller.actions()).hasSize(1);
 *
 *     WebService.Action show = controller.action("show");
 *     assertThat(show).isNotNull();
 *     assertThat(show.key()).isEqualTo("show");
 *     assertThat(index.handler()).isNotNull();
 *   }
 * }
 * </pre>
 *
 * @since 4.2
 */
@ServerSide
@ExtensionPoint
public interface WebService extends Definable<WebService.Context> {

  class Context {
    private final Map<String, Controller> controllers = Maps.newHashMap();

    /**
     * Create a new controller.
     * <p/>
     * Structure of request URL is <code>http://&lt;server&gt;/&lt>controller path&gt;/&lt;action path&gt;?&lt;parameters&gt;</code>.
     *
     * @param path the controller path must not start or end with "/". It is recommended to start with "api/"
     *             and to use lower-case format with underscores, for example "api/coding_rules". Usual actions
     *             are "search", "list", "show", "create" and "delete"
     */
    public NewController createController(String path) {
      return new NewController(this, path);
    }

    private void register(NewController newController) {
      if (controllers.containsKey(newController.path)) {
        throw new IllegalStateException(
          String.format("The web service '%s' is defined multiple times", newController.path));
      }
      controllers.put(newController.path, new Controller(newController));
    }

    @CheckForNull
    public Controller controller(String key) {
      return controllers.get(key);
    }

    public List<Controller> controllers() {
      return ImmutableList.copyOf(controllers.values());
    }
  }

  class NewController {
    private final Context context;
    private final String path;
    private String description, since;
    private final Map<String, NewAction> actions = Maps.newHashMap();

    private NewController(Context context, String path) {
      if (StringUtils.isBlank(path)) {
        throw new IllegalArgumentException("WS controller path must not be empty");
      }
      if (StringUtils.startsWith(path, "/") || StringUtils.endsWith(path, "/")) {
        throw new IllegalArgumentException("WS controller path must not start or end with slash: " + path);
      }
      this.context = context;
      this.path = path;
    }

    /**
     * Important - this method must be called in order to apply changes and make the
     * controller available in {@link org.sonar.api.server.ws.WebService.Context#controllers()}
     */
    public void done() {
      context.register(this);
    }

    /**
     * Optional description (accept HTML)
     */
    public NewController setDescription(@Nullable String s) {
      this.description = s;
      return this;
    }

    /**
     * Optional version when the controller was created
     */
    public NewController setSince(@Nullable String s) {
      this.since = s;
      return this;
    }

    public NewAction createAction(String actionKey) {
      if (actions.containsKey(actionKey)) {
        throw new IllegalStateException(
          String.format("The action '%s' is defined multiple times in the web service '%s'", actionKey, path));
      }
      NewAction action = new NewAction(actionKey);
      actions.put(actionKey, action);
      return action;
    }
  }

  @Immutable
  class Controller {
    private final String path, description, since;
    private final Map<String, Action> actions;

    private Controller(NewController newController) {
      if (newController.actions.isEmpty()) {
        throw new IllegalStateException(
          String.format("At least one action must be declared in the web service '%s'", newController.path));
      }
      this.path = newController.path;
      this.description = newController.description;
      this.since = newController.since;
      ImmutableMap.Builder<String, Action> mapBuilder = ImmutableMap.builder();
      for (NewAction newAction : newController.actions.values()) {
        mapBuilder.put(newAction.key, new Action(this, newAction));
      }
      this.actions = mapBuilder.build();
    }

    public String path() {
      return path;
    }

    @CheckForNull
    public String description() {
      return description;
    }

    @CheckForNull
    public String since() {
      return since;
    }

    @CheckForNull
    public Action action(String actionKey) {
      return actions.get(actionKey);
    }

    public Collection<Action> actions() {
      return actions.values();
    }

    /**
     * Returns true if all the actions are for internal use
     *
     * @see org.sonar.api.server.ws.WebService.Action#isInternal()
     * @since 4.3
     */
    public boolean isInternal() {
      for (Action action : actions()) {
        if (!action.isInternal()) {
          return false;
        }
      }
      return true;
    }
  }

  class NewAction {
    private final String key;
    private String deprecatedKey, description, since;
    private boolean post = false, isInternal = false;
    private RequestHandler handler;
    private Map<String, NewParam> newParams = Maps.newHashMap();
    private URL responseExample = null;

    private NewAction(String key) {
      this.key = key;
    }

    public NewAction setDeprecatedKey(@Nullable String s) {
      this.deprecatedKey = s;
      return this;
    }

    public NewAction setDescription(@Nullable String s) {
      this.description = s;
      return this;
    }

    public NewAction setSince(@Nullable String s) {
      this.since = s;
      return this;
    }

    public NewAction setPost(boolean b) {
      this.post = b;
      return this;
    }

    public NewAction setInternal(boolean b) {
      this.isInternal = b;
      return this;
    }

    public NewAction setHandler(RequestHandler h) {
      this.handler = h;
      return this;
    }

    /**
     * Link to the document containing an example of response. Content must be UTF-8 encoded.
     * <p/>
     * Example:
     * <pre>
     *   newAction.setResponseExample(getClass().getResource("/org/sonar/my-ws-response-example.json"));
     * </pre>
     *
     * @since 4.4
     */
    public NewAction setResponseExample(@Nullable URL url) {
      this.responseExample = url;
      return this;
    }

    public NewParam createParam(String paramKey) {
      if (newParams.containsKey(paramKey)) {
        throw new IllegalStateException(
          String.format("The parameter '%s' is defined multiple times in the action '%s'", paramKey, key));
      }
      NewParam newParam = new NewParam(paramKey);
      newParams.put(paramKey, newParam);
      return newParam;
    }

    /**
     * @deprecated since 4.4. Use {@link #createParam(String paramKey)} instead.
     */
    @Deprecated
    public NewAction createParam(String paramKey, @Nullable String description) {
      createParam(paramKey).setDescription(description);
      return this;
    }

    /**
     * Add predefined parameters related to pagination of results.
     */
    public NewAction addPagingParams(int defaultPageSize) {
      createParam(Param.PAGE)
        .setDescription("1-based page number")
        .setExampleValue("42")
        .setDeprecatedKey("pageIndex")
        .setDefaultValue("1");

      createParam(Param.PAGE_SIZE)
        .setDescription("Page size. Must be greater than 0.")
        .setExampleValue("20")
        .setDeprecatedKey("pageSize")
        .setDefaultValue(String.valueOf(defaultPageSize));
      return this;
    }

    /**
     * Creates the parameter {@link org.sonar.api.server.ws.WebService.Param#FIELDS}, which is
     * used to restrict the number of fields returned in JSON response.
     */
    public NewAction addFieldsParam(Collection possibleValues) {
      createParam(Param.FIELDS)
        .setDescription("Comma-separated list of the fields to be returned in response. All the fields are returned by default.")
        .setPossibleValues(possibleValues);
      return this;
    }

    /**
     * Add predefined parameters related to sorting of results.
     */
    public <V> NewAction addSortParams(Collection<V> possibleValues, @Nullable V defaultValue, boolean defaultAscending) {
      createParam(Param.SORT)
        .setDescription("Sort field")
        .setDeprecatedKey("sort")
        .setDefaultValue(defaultValue)
        .setPossibleValues(possibleValues);

      createParam(Param.ASCENDING)
        .setDescription("Ascending sort")
        .setBooleanPossibleValues()
        .setDefaultValue(defaultAscending);
      return this;
    }
  }

  @Immutable
  class Action {
    private final String key, deprecatedKey, path, description, since;
    private final boolean post, isInternal;
    private final RequestHandler handler;
    private final Map<String, Param> params;
    private final URL responseExample;

    private Action(Controller controller, NewAction newAction) {
      this.key = newAction.key;
      this.deprecatedKey = newAction.deprecatedKey;
      this.path = String.format("%s/%s", controller.path(), key);
      this.description = newAction.description;
      this.since = StringUtils.defaultIfBlank(newAction.since, controller.since);
      this.post = newAction.post;
      this.isInternal = newAction.isInternal;
      this.responseExample = newAction.responseExample;

      if (newAction.handler == null) {
        throw new IllegalArgumentException("RequestHandler is not set on action " + path);
      }
      this.handler = newAction.handler;

      ImmutableMap.Builder<String, Param> paramsBuilder = ImmutableMap.builder();
      for (NewParam newParam : newAction.newParams.values()) {
        paramsBuilder.put(newParam.key, new Param(this, newParam));
      }
      this.params = paramsBuilder.build();
    }

    public String key() {
      return key;
    }

    public String deprecatedKey() {
      return deprecatedKey;
    }

    public String path() {
      return path;
    }

    @CheckForNull
    public String description() {
      return description;
    }

    /**
     * Set if different than controller.
     */
    @CheckForNull
    public String since() {
      return since;
    }

    public boolean isPost() {
      return post;
    }

    public boolean isInternal() {
      return isInternal;
    }

    public RequestHandler handler() {
      return handler;
    }

    /**
     * @see org.sonar.api.server.ws.WebService.NewAction#setResponseExample(java.net.URL)
     */
    @CheckForNull
    public URL responseExample() {
      return responseExample;
    }

    /**
     * @see org.sonar.api.server.ws.WebService.NewAction#setResponseExample(java.net.URL)
     */
    @CheckForNull
    public String responseExampleAsString() {
      try {
        if (responseExample != null) {
          return StringUtils.trim(IOUtils.toString(responseExample, Charsets.UTF_8));
        }
        return null;
      } catch (IOException e) {
        throw new IllegalStateException("Fail to load " + responseExample, e);
      }
    }

    /**
     * @see org.sonar.api.server.ws.WebService.NewAction#setResponseExample(java.net.URL)
     */
    @CheckForNull
    public String responseExampleFormat() {
      if (responseExample != null) {
        return StringUtils.lowerCase(FilenameUtils.getExtension(responseExample.getFile()));
      }
      return null;
    }

    @CheckForNull
    public Param param(String key) {
      return params.get(key);
    }

    public Collection<Param> params() {
      return params.values();
    }

    @Override
    public String toString() {
      return path;
    }
  }

  class NewParam {
    private String key, deprecatedKey, description, exampleValue, defaultValue;
    private boolean required = false;
    private Set<String> possibleValues = null;

    private NewParam(String key) {
      this.key = key;
    }

    /**
     * @since 5.0
     */
    public NewParam setDeprecatedKey(@Nullable String s) {
      this.deprecatedKey = s;
      return this;
    }

    public NewParam setDescription(@Nullable String s) {
      this.description = s;
      return this;
    }

    /**
     * Is the parameter required or optional ? Default value is false (optional).
     *
     * @since 4.4
     */
    public NewParam setRequired(boolean b) {
      this.required = b;
      return this;
    }

    /**
     * @since 4.4
     */
    public NewParam setExampleValue(@Nullable Object s) {
      this.exampleValue = (s != null ? s.toString() : null);
      return this;
    }

    /**
     * Exhaustive list of possible values when it makes sense, for example
     * list of severities.
     *
     * @since 4.4
     */
    public NewParam setPossibleValues(@Nullable Object... values) {
      return setPossibleValues(values == null ? (Collection) null : Arrays.asList(values));
    }

    /**
     * @since 4.4
     */
    public NewParam setBooleanPossibleValues() {
      return setPossibleValues("true", "false", "yes", "no");
    }

    /**
     * Exhaustive list of possible values when it makes sense, for example
     * list of severities.
     *
     * @since 4.4
     */
    public NewParam setPossibleValues(@Nullable Collection values) {
      if (values == null || values.isEmpty()) {
        this.possibleValues = null;
      } else {
        this.possibleValues = Sets.newLinkedHashSet();
        for (Object value : values) {
          this.possibleValues.add(value.toString());
        }
      }
      return this;
    }

    /**
     * @since 4.4
     */
    public NewParam setDefaultValue(@Nullable Object o) {
      this.defaultValue = (o != null ? o.toString() : null);
      return this;
    }

    @Override
    public String toString() {
      return key;
    }
  }

  @Immutable
  class Param {
    public static final String TEXT_QUERY = "q";
    public static final String PAGE = "p";
    public static final String PAGE_SIZE = "ps";
    public static final String FIELDS = "f";
    public static final String SORT = "s";
    public static final String ASCENDING = "asc";
    public static final String FACETS = "facets";

    private final String key, deprecatedKey, description, exampleValue, defaultValue;
    private final boolean required;
    private final Set<String> possibleValues;

    protected Param(Action action, NewParam newParam) {
      this.key = newParam.key;
      this.deprecatedKey = newParam.deprecatedKey;
      this.description = newParam.description;
      this.exampleValue = newParam.exampleValue;
      this.defaultValue = newParam.defaultValue;
      this.required = newParam.required;
      this.possibleValues = newParam.possibleValues;
      if (required && defaultValue != null) {
        throw new IllegalArgumentException(String.format("Default value must not be set on parameter '%s?%s' as it's marked as required", action, key));
      }
    }

    public String key() {
      return key;
    }

    /**
     * @since 5.0
     */
    @CheckForNull
    public String deprecatedKey() {
      return deprecatedKey;
    }

    @CheckForNull
    public String description() {
      return description;
    }

    /**
     * @since 4.4
     */
    @CheckForNull
    public String exampleValue() {
      return exampleValue;
    }

    /**
     * Is the parameter required or optional ?
     *
     * @since 4.4
     */
    public boolean isRequired() {
      return required;
    }

    /**
     * @since 4.4
     */
    @CheckForNull
    public Set<String> possibleValues() {
      return possibleValues;
    }

    /**
     * @since 4.4
     */
    @CheckForNull
    public String defaultValue() {
      return defaultValue;
    }

    @Override
    public String toString() {
      return key;
    }
  }

  /**
   * Executed once at server startup.
   */
  @Override
  void define(Context context);

}
