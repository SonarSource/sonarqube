/*
 * SonarQube
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
import CustomValuesFacet from './custom-values-facet';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export default CustomValuesFacet.extend({
  getUrl: function () {
    return '/api/issues/authors';
  },

  prepareSearch: function () {
    return this.$('.js-custom-value').select2({
      placeholder: 'Search...',
      minimumInputLength: 2,
      allowClear: false,
      formatNoMatches: function () {
        return translate('select2.noMatches');
      },
      formatSearching: function () {
        return translate('select2.searching');
      },
      formatInputTooShort: function () {
        return translateWithParameters('select2.tooShort', 2);
      },
      width: '100%',
      ajax: {
        quietMillis: 300,
        url: this.getUrl(),
        data: function (term) {
          return { q: term, ps: 25 };
        },
        results: function (data) {
          return {
            more: false,
            results: data.authors.map(function (author) {
              return { id: author, text: author };
            })
          };
        }
      }
    });
  }
});
