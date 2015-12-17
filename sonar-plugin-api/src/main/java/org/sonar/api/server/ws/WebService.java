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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;

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
 *             .beginObject()
 *             .prop("hello", request.mandatoryParam("key"))
 *             .endObject()
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
     *             are "search", "list", "show", "create" and "delete".
     *             the plural form is recommended - ex: api/projects
     */
    public NewController createController(String path) {
      return new NewController(this, path);
    }

    private void register(NewController newController) {
      if (controllers.containsKey(newController.path)) {
        throw new IllegalStateException(
          format("The web service '%s' is defined multiple times", newController.path));
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
    private String description;
    private String since;
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
          format("The action '%s' is defined multiple times in the web service '%s'", actionKey, path));
      }
      NewAction action = new NewAction(actionKey);
      actions.put(actionKey, action);
      return action;
    }
  }

  @Immutable
  class Controller {
    private final String path;
    private final String description;
    private final String since;
    private final Map<String, Action> actions;

    private Controller(NewController newController) {
      checkState(!newController.actions.isEmpty(), format("At least one action must be declared in the web service '%s'", newController.path));
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
    private String deprecatedKey;
    private String description;
    private String since;
    private String deprecatedSince;
    private boolean post = false;
    private boolean isInternal = false;
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

    public NewAction setDeprecatedSince(@Nullable String deprecatedSince) {
      this.deprecatedSince = deprecatedSince;
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
      checkState(!newParams.containsKey(paramKey),
        format("The parameter '%s' is defined multiple times in the action '%s'", paramKey, key));
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
     * Add predefined parameters related to pagination of results with a maximum page size.
     * Note the maximum is a documentation only feature. It does not check anything.
     */
    public NewAction addPagingParams(int defaultPageSize, int maxPageSize) {
      createParam(Param.PAGE)
        .setDescription("1-based page number")
        .setExampleValue("42")
        .setDeprecatedKey("pageIndex")
        .setDefaultValue("1");

      createParam(Param.PAGE_SIZE)
        .setDescription("Page size. Must be greater than 0 and less than " + maxPageSize)
        .setExampleValue("20")
        .setDeprecatedKey("pageSize")
        .setDefaultValue(String.valueOf(defaultPageSize));
      return this;
    }

    /**
     * Creates the parameter {@link org.sonar.api.server.ws.WebService.Param#FIELDS}, which is
     * used to restrict the number of fields returned in JSON response.
     */
    public NewAction addFieldsParam(Collection<?> possibleValues) {
      createParam(Param.FIELDS)
        .setDescription("Comma-separated list of the fields to be returned in response. All the fields are returned by default.")
        .setPossibleValues(possibleValues);
      return this;
    }

    /**$
     *
     * Creates the parameter {@link org.sonar.api.server.ws.WebService.Param#TEXT_QUERY}, which is
     * used to search for a subset of fields containing the supplied string.<br />
     * The fields must be in the <strong>plural</strong> form (ex: "names", "keys")
     */
    public NewAction addSearchQuery(String exampleValue, String... pluralFields) {
      String actionDescription = format("Limit search to %s that contain the supplied string.", Joiner.on(" or ").join(pluralFields));
      createParam(Param.TEXT_QUERY)
        .setDescription(actionDescription)
        .setExampleValue(exampleValue);
      return this;
    }

    /**
     * Add predefined parameters related to sorting of results.
     */
    public <V> NewAction addSortParams(Collection<V> possibleValues, @Nullable V defaultValue, boolean defaultAscending) {
      genericAddSortParam(possibleValues, defaultValue, defaultAscending, "Sort field");

      return this;
    }

    /**
     * Add predefined parameters related to sorting of results. Comma-separated list
     */
    public <V> NewAction addMultiSortsParams(Collection<V> possibleValues, @Nullable V defaultValue, boolean defaultAscending) {
      genericAddSortParam(possibleValues, defaultValue, defaultAscending, "Comma-separated list of sort fields");

      return this;
    }

    public <V> NewAction genericAddSortParam(Collection<V> possibleValues, @Nullable V defaultValue, boolean defaultAscending, String description) {
      createParam(Param.SORT)
        .setDescription(description)
        .setDeprecatedKey("sort")
        .setDefaultValue(defaultValue)
        .setPossibleValues(possibleValues);

      createParam(Param.ASCENDING)
        .setDescription("Ascending sort")
        .setBooleanPossibleValues()
        .setDefaultValue(defaultAscending);
      return this;
    }

    /**
     * Add 'selected=(selected|deselected|all)' for select-list oriented WS.
     */
    public NewAction addSelectionModeParam() {
      createParam(Param.SELECTED)
        .setDescription("Depending on the value, show only selected items (selected=selected), deselected items (selected=deselected), " +
          "or all items with their selection status (selected=all).")
        .setDefaultValue(SelectionMode.SELECTED.value())
        .setPossibleValues(SelectionMode.possibleValues());
      return this;
    }
  }

  @Immutable
  class Action {
    private static final Logger LOGGER = Loggers.get(Action.class);

    private final String key;
    private final String deprecatedKey;
    private final String path;
    private final String description;
    private final String since;
    private final String deprecatedSince;
    private final boolean post;
    private final boolean isInternal;
    private final RequestHandler handler;
    private final Map<String, Param> params;
    private final URL responseExample;

    private Action(Controller controller, NewAction newAction) {
      this.key = newAction.key;
      this.deprecatedKey = newAction.deprecatedKey;
      this.path = format("%s/%s", controller.path(), key);
      this.description = newAction.description;
      this.since = newAction.since;
      this.deprecatedSince = newAction.deprecatedSince;
      this.post = newAction.post;
      this.isInternal = newAction.isInternal;
      this.responseExample = newAction.responseExample;
      this.handler = newAction.handler;

      checkState(this.handler != null, "RequestHandler is not set on action " + path);
      logWarningIf(isNullOrEmpty(this.description), "Description is not set on action " + path);
      logWarningIf(isNullOrEmpty(this.since), "Since is not set on action " + path);
      logWarningIf(!this.post && this.responseExample == null, "The response example is not set on action " + path);

      ImmutableMap.Builder<String, Param> paramsBuilder = ImmutableMap.builder();
      for (NewParam newParam : newAction.newParams.values()) {
        paramsBuilder.put(newParam.key, new Param(this, newParam));
      }
      this.params = paramsBuilder.build();
    }

    private static void logWarningIf(boolean condition, String message) {
      if (condition) {
        LOGGER.warn(message);
      }
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

    @CheckForNull
    public String deprecatedSince() {
      return deprecatedSince;
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
          return StringUtils.trim(IOUtils.toString(responseExample, StandardCharsets.UTF_8));
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
    private String key;
    private String since;
    private String deprecatedSince;
    private String deprecatedKey;
    private String description;
    private String exampleValue;
    private String defaultValue;
    private boolean required = false;
    private Set<String> possibleValues = null;

    private NewParam(String key) {
      this.key = key;
    }

    public NewParam setSince(@Nullable String since) {
      this.since = since;
      return this;
    }

    public NewParam setDeprecatedSince(@Nullable String deprecatedSince) {
      this.deprecatedSince = deprecatedSince;
      return this;
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
      this.exampleValue = ((s != null) ? s.toString() : null);
      return this;
    }

    /**
     * Exhaustive list of possible values when it makes sense, for example
     * list of severities.
     *
     * @since 4.4
     */
    public NewParam setPossibleValues(@Nullable Object... values) {
      return setPossibleValues(values == null ? Collections.emptyList() : Arrays.asList(values));
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
    public NewParam setPossibleValues(@Nullable Collection<?> values) {
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
      this.defaultValue = ((o != null) ? o.toString() : null);
      return this;
    }

    @Override
    public String toString() {
      return key;
    }
  }

  enum SelectionMode {
    SELECTED("selected"), DESELECTED("deselected"), ALL("all");

    private final String paramValue;

    private static final Map<String, SelectionMode> BY_VALUE = Maps.uniqueIndex(Arrays.asList(values()), new Function<SelectionMode, String>() {
      @Override
      public String apply(@Nonnull SelectionMode input) {
        return input.paramValue;
      }
    });

    private SelectionMode(String paramValue) {
      this.paramValue = paramValue;
    }

    public String value() {
      return paramValue;
    }

    public static SelectionMode fromParam(String paramValue) {
      checkArgument(BY_VALUE.containsKey(paramValue));
      return BY_VALUE.get(paramValue);
    }

    public static Collection<String> possibleValues() {
      return BY_VALUE.keySet();
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
    public static final String SELECTED = "selected";

    private final String key;
    private final String since;
    private final String deprecatedSince;
    private final String deprecatedKey;
    private final String description;
    private final String exampleValue;
    private final String defaultValue;
    private final boolean required;
    private final Set<String> possibleValues;

    protected Param(Action action, NewParam newParam) {
      this.key = newParam.key;
      this.since = newParam.since;
      this.deprecatedSince = newParam.deprecatedSince;
      this.deprecatedKey = newParam.deprecatedKey;
      this.description = newParam.description;
      this.exampleValue = newParam.exampleValue;
      this.defaultValue = newParam.defaultValue;
      this.required = newParam.required;
      this.possibleValues = newParam.possibleValues;
      if (required && defaultValue != null) {
        throw new IllegalArgumentException(format("Default value must not be set on parameter '%s?%s' as it's marked as required", action, key));
      }
    }

    public String key() {
      return key;
    }

    /**
     * @since 5.3
     */
    @CheckForNull
    public String since() {
      return since;
    }

    /**
     * @since 5.3
     */
    @CheckForNull
    public String deprecatedSince() {
      return deprecatedSince;
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
