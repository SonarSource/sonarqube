/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import BaseFacet from './base-facet';
import Template from '../templates/facets/coding-rules-custom-values-facet.hbs';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export default BaseFacet.extend({
  template: Template,

  events() {
    return {
      ...BaseFacet.prototype.events.apply(this, arguments),
      'change .js-custom-value': 'addCustomValue'
    };
  },

  getUrl() {
    return window.baseUrl;
  },

  onRender() {
    BaseFacet.prototype.onRender.apply(this, arguments);
    this.prepareSearch();
  },

  prepareSearch() {
    this.$('.js-custom-value').select2({
      placeholder: translate('search_verb'),
      minimumInputLength: 1,
      allowClear: false,
      formatNoMatches() {
        return translate('select2.noMatches');
      },
      formatSearching() {
        return translate('select2.searching');
      },
      formatInputTooShort() {
        return translateWithParameters('select2.tooShort', 1);
      },
      width: '100%',
      ajax: this.prepareAjaxSearch()
    });
  },

  prepareAjaxSearch() {
    return {
      quietMillis: 300,
      url: this.getUrl(),
      data(term, page) {
        return { s: term, p: page };
      },
      results(data) {
        return { more: data.more, results: data.results };
      }
    };
  },

  addCustomValue() {
    const property = this.model.get('property');
    const customValue = this.$('.js-custom-value').select2('val');
    let value = this.getValue();
    if (value.length > 0) {
      value += ',';
    }
    value += customValue;
    const obj = {};
    obj[property] = value;
    this.options.app.state.updateFilter(obj);
  }
});
