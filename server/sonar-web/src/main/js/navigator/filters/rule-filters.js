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
  'navigator/filters/base-filters',
  'navigator/filters/ajax-select-filters'
], function (BaseFilters, AjaxSelectFilters) {

  var RuleSuggestions = AjaxSelectFilters.Suggestions.extend({

    url: function() {
      return baseUrl + '/api/rules/search';
    },


    fetch: function(options) {
      options.data.f = 'name,lang';
      return AjaxSelectFilters.Suggestions.prototype.fetch.call(this, options);
    },


    parse: function(r) {
      this.more = r.p * r.ps < r.total;
      return r.rules.map(function(rule) {
         return { id: rule.key, text: rule.name, category: rule.lang };
      });
    }

  });



  var RuleDetailsFilterView = AjaxSelectFilters.AjaxSelectDetailsFilterView.extend({
    searchKey: 'q'
  });


  return AjaxSelectFilters.AjaxSelectFilterView.extend({

    initialize: function() {
      AjaxSelectFilters.AjaxSelectFilterView.prototype.initialize.call(this, {
        detailsView: RuleDetailsFilterView
      });

      this.choices = new RuleSuggestions();
    },


    createRequest: function(v) {
      var that = this;
      return jQuery
          .ajax({
            url: baseUrl + '/api/rules/show',
            type: 'GET',
            data: { key: v }
          })
          .done(function (r) {
            that.choices.add(new Backbone.Model({
              id: r.rule.key,
              text: r.rule.name,
              category: r.rule.language,
              checked: true
            }));
          });
    }

  });

});
