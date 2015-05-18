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
define([
  './actions-view',
  './header-view'
], function (ActionsView, HeaderView) {

  return Marionette.Controller.extend({

    initialize: function (options) {
      this.list = options.app.list;
      this.listenTo(this.list, 'select', this.onItemSelect);
    },

    show: function (path) {
      var that = this;
      this.fetchList().done(function () {
        if (path) {
          var item = that.list.findWhere({ path: path });
          if (item != null) {
            that.showWebService(path);
          } else {
            that.showAction(path);
          }
        }
      });
    },

    showWebService: function (path) {
      var item = this.list.findWhere({ path: path });
      if (item != null) {
        item.trigger('select', item);
      }
    },

    showAction: function (path) {
      var webService = this.list.find(function (item) {
        return path.indexOf(item.get('path')) === 0;
      });
      if (webService != null) {
        var action = path.substr(webService.get('path').length + 1);
        webService.trigger('select', webService, { trigger: false, action: action });
      }
    },

    onItemSelect: function (item, options) {
      var path = item.get('path'),
          opts = _.defaults(options || {}, { trigger: true });
      if (opts.trigger) {
        this.options.app.router.navigate(path);
      }
      this.options.app.listView.highlight(path);

      if (item.get('internal')) {
        this.options.state.set({ internal: true });
      }

      var actions = new Backbone.Collection(item.get('actions')),
          actionsView = new ActionsView({ collection: actions });
      this.options.app.layout.detailsRegion.show(actionsView);
      this.options.app.layout.headerRegion.show(new HeaderView({ model: item }));

      if (opts.action != null) {
        actionsView.scrollToAction(opts.action);
      } else {
        actionsView.scrollToTop();
      }
    },

    fetchList: function () {
      return this.list.fetch({ data: { 'include_internals': true } });
    }

  });

});
