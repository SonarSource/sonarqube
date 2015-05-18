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
define(['./item'], function (Item) {

  var STORAGE_KEY = 'sonarqube-workspace';

  return Backbone.Collection.extend({
    model: Item,

    initialize: function () {
      this.on('remove', this.save);
    },

    save: function () {
      var dump = JSON.stringify(this.toJSON());
      window.localStorage.setItem(STORAGE_KEY, dump);
    },

    load: function () {
      var dump = window.localStorage.getItem(STORAGE_KEY);
      if (dump != null) {
        try {
          var parsed = JSON.parse(dump);
          this.reset(parsed);
        } catch (err) { }
      }
    },

    has: function (model) {
      var forComponent = model.isComponent() && this.findWhere({ uuid: model.get('uuid') }) != null,
          forRule = model.isRule() && this.findWhere({ key: model.get('key') }) != null;
      return forComponent || forRule;
    }
  });

});
