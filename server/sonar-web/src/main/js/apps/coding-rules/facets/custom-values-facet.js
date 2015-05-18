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
    './base-facet',
    '../templates'
], function (BaseFacet) {

  return BaseFacet.extend({
    template: Templates['coding-rules-custom-values-facet'],

    events: function () {
      return _.extend(BaseFacet.prototype.events.apply(this, arguments), {
        'change .js-custom-value': 'addCustomValue'
      });
    },

    getUrl: function () {
      return baseUrl;
    },

    onRender: function () {
      BaseFacet.prototype.onRender.apply(this, arguments);
      this.prepareSearch();
    },

    prepareSearch: function () {
      this.$('.js-custom-value').select2({
        placeholder: t('search_verb'),
        minimumInputLength: 1,
        allowClear: false,
        formatNoMatches: function () {
          return t('select2.noMatches');
        },
        formatSearching: function () {
          return t('select2.searching');
        },
        formatInputTooShort: function () {
          return tp('select2.tooShort', 1);
        },
        width: '100%',
        ajax: this.prepareAjaxSearch()
      });
    },

    prepareAjaxSearch: function () {
      return {
        quietMillis: 300,
        url: this.getUrl(),
        data: function (term, page) {
          return { s: term, p: page };
        },
        results: function (data) {
          return { more: data.more, results: data.results };
        }
      };
    },

    addCustomValue: function () {
      var property = this.model.get('property'),
          customValue = this.$('.js-custom-value').select2('val'),
          value = this.getValue();
      if (value.length > 0) {
        value += ',';
      }
      value += customValue;
      var obj = {};
      obj[property] = value;
      this.options.app.state.updateFilter(obj);
    }
  });

});
