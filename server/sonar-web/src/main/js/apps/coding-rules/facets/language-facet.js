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
  './custom-values-facet'
], function (CustomValuesFacet) {

  return CustomValuesFacet.extend({

    getUrl: function () {
      return baseUrl + '/api/languages/list';
    },

    prepareAjaxSearch: function () {
      return {
        quietMillis: 300,
        url: this.getUrl(),
        data: function (term) {
          return { q: term, ps: 10000 };
        },
        results: function (data) {
          return {
            more: false,
            results: data.languages.map(function (lang) {
              return { id: lang.key, text: lang.name };
            })
          };
        }
      };
    },

    getLabelsSource: function () {
      return this.options.app.languages;
    },

    getValues: function () {
      var that = this,
          labels = that.getLabelsSource();
      return this.model.getValues().map(function (item) {
        return _.extend(item, {
          label: labels[item.val]
        });
      });
    },

    serializeData: function () {
      return _.extend(CustomValuesFacet.prototype.serializeData.apply(this, arguments), {
        values: this.getValues()
      });
    }

  });

});
