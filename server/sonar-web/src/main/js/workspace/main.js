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
  'workspace/models/item',
  'workspace/models/items',
  'workspace/views/items-view',
  'workspace/views/viewer-view'
], function (Item, Items, ItemsView, ViewerView) {

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

      this.itemsView = new ItemsView({ collection: this.items });
      this.itemsView.render().$el.appendTo(document.body);
      this.itemsView.on('click', function (uuid, model) {
        model.collection.remove(model);
        that.showComponentViewer(model.toJSON());
      });
    },

    save: function () {
      this.items.save();
    },

    load: function () {
      this.items.load();
    },

    addComponent: function (options) {
      if (options == null || typeof options.uuid !== 'string') {
        throw new Error('You must specify the component\'s uuid');
      }
      this.items.add(options);
      this.save();
    },

    openComponent: function (options) {
      if (options == null || typeof options.uuid !== 'string') {
        throw new Error('You must specify the component\'s uuid');
      }
      this.showComponentViewer(options);
    },

    showComponentViewer: function (options) {
      var that = this;
      if (this.viewerView != null) {
        this.viewerView.close();
      }
      $('.source-viewer').addClass('with-workspace');
      this.viewerView = new ViewerView({
        model: new Item(options)
      });
      this.viewerView.on('minimize', function (model) {
        that.addComponent(model.toJSON());
        that.closeComponentViewer();
      });
      this.viewerView.on('close', function () {
        that.closeComponentViewer();
      });
      this.viewerView.render().$el.appendTo(document.body);
    },

    closeComponentViewer: function () {
      if (this.viewerView != null) {
        this.viewerView.close();
        $('.with-workspace').removeClass('with-workspace');
      }
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
