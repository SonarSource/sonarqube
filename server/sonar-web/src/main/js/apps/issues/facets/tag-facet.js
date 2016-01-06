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
import { translate } from '../../../helpers/l10n';

export default CustomValuesFacet.extend({
  prepareSearch: function () {
    var url = baseUrl + '/api/issues/tags?ps=10',
        tags = this.options.app.state.get('query').tags;
    if (tags != null) {
      url += '&tags=' + tags;
    }
    return this.$('.js-custom-value').select2({
      placeholder: 'Search...',
      minimumInputLength: 0,
      allowClear: false,
      formatNoMatches: function () {
        return translate('select2.noMatches');
      },
      formatSearching: function () {
        return translate('select2.searching');
      },
      width: '100%',
      ajax: {
        quietMillis: 300,
        url: url,
        data: function (term) {
          return { q: term, ps: 10 };
        },
        results: function (data) {
          var results = data.tags.map(function (tag) {
            return { id: tag, text: tag };
          });
          return { more: false, results: results };
        }
      }
    });
  },

  getValuesWithLabels: function () {
    var values = this.model.getValues();
    values.forEach(function (v) {
      v.label = v.val;
      v.extra = '';
    });
    return values;
  },

  serializeData: function () {
    return _.extend(CustomValuesFacet.prototype.serializeData.apply(this, arguments), {
      values: this.sortValues(this.getValuesWithLabels())
    });
  }
});
