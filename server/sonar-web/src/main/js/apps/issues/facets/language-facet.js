/*
 * SonarQube :: Web
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import _ from 'underscore';
import CustomValuesFacet from './custom-values-facet';

export default CustomValuesFacet.extend({
  getUrl: function () {
    return baseUrl + '/api/languages/list';
  },

  prepareSearch: function () {
    return this.$('.js-custom-value').select2({
      placeholder: 'Search...',
      minimumInputLength: 2,
      allowClear: false,
      formatNoMatches: function () {
        return window.t('select2.noMatches');
      },
      formatSearching: function () {
        return window.t('select2.searching');
      },
      formatInputTooShort: function () {
        return window.tp('select2.tooShort', 2);
      },
      width: '100%',
      ajax: {
        quietMillis: 300,
        url: this.getUrl(),
        data: function (term) {
          return { q: term, ps: 0 };
        },
        results: function (data) {
          return {
            more: false,
            results: data.languages.map(function (lang) {
              return { id: lang.key, text: lang.name };
            })
          };
        }
      }
    });
  },

  getValuesWithLabels: function () {
    var values = this.model.getValues(),
        source = this.options.app.facets.languages;
    values.forEach(function (v) {
      var key = v.val,
          label = null;
      if (key) {
        var item = _.findWhere(source, { key: key });
        if (item != null) {
          label = item.name;
        }
      }
      v.label = label;
    });
    return values;
  },

  serializeData: function () {
    return _.extend(CustomValuesFacet.prototype.serializeData.apply(this, arguments), {
      values: this.sortValues(this.getValuesWithLabels())
    });
  }
});
