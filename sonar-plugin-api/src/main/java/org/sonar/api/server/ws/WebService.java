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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerExtension;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Defines a web service implemented in Java (no Ruby on Rails at all).
 *
 * @since 4.2
 */
public interface WebService extends ServerExtension {

  class Context {
    private final Map<String, Controller> controllers = Maps.newHashMap();

    public NewController newController(String path) {
      return new NewController(this, path);
    }

    private void register(NewController newController) {
      if (controllers.containsKey(newController.path)) {
        throw new IllegalStateException(
          String.format("The web service '%s' is defined multiple times", newController.path)
        );
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
        throw new IllegalArgumentException("Web service path can't be empty");
      }
      this.context = context;
      this.path = path;
    }

    public void done() {
      context.register(this);
    }

    public NewController setDescription(@Nullable String s) {
      this.description = s;
      return this;
    }

    public NewController setSince(@Nullable String s) {
      this.since = s;
      return this;
    }

    public NewAction newAction(String actionKey) {
      if (actions.containsKey(actionKey)) {
        throw new IllegalStateException(
          String.format("The action '%s' is defined multiple times in the web service '%s'", actionKey, path)
        );
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
          String.format("At least one action must be declared in the web service '%s'", newController.path)
        );
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
    private String description, since;
    private boolean post = false, isInternal = false;
    private RequestHandler handler;
    private Map<String, NewParam> newParams = Maps.newHashMap();

    private NewAction(String key) {
      this.key = key;
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

    public NewParam newParam(String paramKey) {
      if (newParams.containsKey(paramKey)) {
        throw new IllegalStateException(
          String.format("The parameter '%s' is defined multiple times in the action '%s'", paramKey, key)
        );
      }
      NewParam newParam = new NewParam(paramKey);
      newParams.put(paramKey, newParam);
      return newParam;
    }

    public NewAction newParam(String paramKey, @Nullable String description) {
      newParam(paramKey).setDescription(description);
      return this;
    }
  }

  @Immutable
  class Action {
    private final String key, path, description, since;
    private final boolean post, isInternal;
    private final RequestHandler handler;
    private final Map<String, Param> params;

    private Action(Controller controller, NewAction newAction) {
      this.key = newAction.key;
      this.path = String.format("%s/%s", controller.path(), key);
      this.description = newAction.description;
      this.since = StringUtils.defaultIfBlank(newAction.since, controller.since);
      this.post = newAction.post;
      this.isInternal = newAction.isInternal;

      if (newAction.handler == null) {
        throw new IllegalStateException("RequestHandler is not set on action " + path);
      }
      this.handler = newAction.handler;

      ImmutableMap.Builder<String, Param> mapBuilder = ImmutableMap.builder();
      for (NewParam newParam : newAction.newParams.values()) {
        mapBuilder.put(newParam.key, new Param(newParam));
      }
      this.params = mapBuilder.build();
    }

    public String key() {
      return key;
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
    private String key, description;

    private NewParam(String key) {
      this.key = key;
    }

    public NewParam setDescription(@Nullable String s) {
      this.description = s;
      return this;
    }

    @Override
    public String toString() {
      return key;
    }
  }

  @Immutable
  class Param {
    private final String key, description;

    public Param(NewParam newParam) {
      this.key = newParam.key;
      this.description = newParam.description;
    }

    public String key() {
      return key;
    }

    @CheckForNull
    public String description() {
      return description;
    }

    @Override
    public String toString() {
      return key;
    }
  }

  /**
   * Executed once at server startup.
   */
  void define(Context context);

}
