/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.ws;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerExtension;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
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
      // TODO check format of path
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

    public boolean isApi() {
      return path.startsWith("api/");
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
  }

  // TODO define supported parameters
  class NewAction {
    private final String key;
    private String description, since;
    private boolean post = false;
    private RequestHandler handler;

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

    public NewAction setHandler(RequestHandler h) {
      this.handler = h;
      return this;
    }
  }

  class Action {
    private final String key, path, description, since;
    private final boolean post;
    private final RequestHandler handler;

    private Action(Controller controller, NewAction newAction) {
      this.key = newAction.key;
      this.path = String.format("%s/%s", controller.path(), key);
      this.description = newAction.description;
      this.since = StringUtils.defaultIfBlank(newAction.since, controller.since);
      this.post = newAction.post;
      this.handler = newAction.handler;
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

    @CheckForNull
    public RequestHandler handler() {
      return handler;
    }

    @Override
    public String toString() {
      return path;
    }
  }

  /**
   * Executed at server startup.
   */
  void define(Context context);

}
