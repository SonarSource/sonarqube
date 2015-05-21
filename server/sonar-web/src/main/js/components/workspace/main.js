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
  './models/item',
  './models/items',
  './views/items-view',
  './views/viewer-view',
  './views/rule-view'
], function (Item, Items, ItemsView, ViewerView, RuleView) {

  var $ = jQuery,

      instance = null,

      Workspace = function () {
        if (instance != null) {
          throw new Error('Cannot instantiate more than one Workspace, use Workspace.getInstance()');
        }
        this.initialize();
      };

  Workspace.prototype = {
    initialize: function () {
      var that = this;

      this.items = new Items();
      this.items.load();
      this.items.on('change', function () {
        that.save();
      });

      this.itemsView = new ItemsView({ collection: this.items });
      this.itemsView.render().$el.appendTo(document.body);
      this.itemsView.on('click', function (model) {
        that.open(model);
      });
    },

    save: function () {
      this.items.save();
    },

    addComponent: function (model) {
      if (!this.items.has(model)) {
        this.items.add(model);
      }
      this.save();
    },

    open: function (options) {
      var model = typeof options.toJSON === 'function' ? options : new Item(options);
      if (!model.isValid()) {
        throw new Error(model.validationError);
      }
      this.addComponent(model);
      if (model.isComponent()) {
        this.showComponentViewer(model);
      }
      if (model.isRule()) {
        this.showRule(model);
      }
    },

    openComponent: function (options) {
      return this.open(_.extend(options, { type: 'component' }));
    },

    openRule: function (options) {
      return this.open(_.extend(options, { type: 'rule' }));
    },

    showViewer: function (Viewer, model) {
      var that = this;
      if (this.viewerView != null) {
        this.viewerView.model.trigger('hideViewer');
        this.viewerView.close();
      }
      $('.source-viewer').addClass('with-workspace');
      model.trigger('showViewer');
      this.viewerView = new Viewer({ model: model });
      this.viewerView
          .on('viewerMinimize', function () {
            model.trigger('hideViewer');
            that.closeComponentViewer();
          })
          .on('viewerClose', function (m) {
            that.closeComponentViewer();
            m.destroy();
          });
      this.viewerView.render().$el.appendTo(document.body);
    },

    showComponentViewer: function (model) {
      this.showViewer(ViewerView, model);
    },

    closeComponentViewer: function () {
      if (this.viewerView != null) {
        this.viewerView.close();
        $('.with-workspace').removeClass('with-workspace');
      }
    },

    showRule: function (model) {
      this.showViewer(RuleView, model);
      this.fetchRule(model);
    },

    fetchRule: function (model) {
      var url = baseUrl + '/api/rules/show',
          options = { key: model.get('key') };
      return $.get(url, options).done(function (r) {
        model.set(r.rule);
      });
    }
  };

  Workspace.getInstance = function () {
    if (instance == null) {
      instance = new Workspace();
    }
    return instance;
  };

  return Workspace.getInstance();

});
