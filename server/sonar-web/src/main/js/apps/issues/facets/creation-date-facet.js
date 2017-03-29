/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import $ from 'jquery';
import moment from 'moment';
import { times } from 'lodash';
import BaseFacet from './base-facet';
import Template from '../templates/facets/issues-creation-date-facet.hbs';
import '../../../components/widgets/barchart';
import { formatMeasure } from '../../../helpers/measures';

export default BaseFacet.extend({
  template: Template,

  events() {
    return {
      ...BaseFacet.prototype.events.apply(this, arguments),
      'change input': 'applyFacet',
      'click .js-select-period-start': 'selectPeriodStart',
      'click .js-select-period-end': 'selectPeriodEnd',
      'click .sonar-d3 rect': 'selectBar',
      'click .js-all': 'onAllClick',
      'click .js-last-week': 'onLastWeekClick',
      'click .js-last-month': 'onLastMonthClick',
      'click .js-last-year': 'onLastYearClick',
      'click .js-leak': 'onLeakClick'
    };
  },

  onRender() {
    const that = this;
    this.$el.toggleClass('search-navigator-facet-box-collapsed', !this.model.get('enabled'));
    this.$('input').datepicker({
      dateFormat: 'yy-mm-dd',
      changeMonth: true,
      changeYear: true
    });
    const props = ['createdAfter', 'createdBefore', 'createdAt'];
    const query = this.options.app.state.get('query');
    props.forEach(prop => {
      const value = query[prop];
      if (value != null) {
        that.$(`input[name=${prop}]`).val(value);
      }
    });
    let values = this.model.getValues();
    if (!(Array.isArray(values) && values.length > 0)) {
      let date = moment();
      values = [];
      times(10, () => {
        values.push({ count: 0, val: date.toDate().toString() });
        date = date.subtract(1, 'days');
      });
      values.reverse();
    }
    values = values.map(v => {
      const format = that.options.app.state.getFacetMode() === 'count'
        ? 'SHORT_INT'
        : 'SHORT_WORK_DUR';
      const text = formatMeasure(v.count, format);
      return { ...v, text };
    });
    return this.$('.js-barchart').barchart(values);
  },

  selectPeriodStart() {
    return this.$('.js-period-start').datepicker('show');
  },

  selectPeriodEnd() {
    return this.$('.js-period-end').datepicker('show');
  },

  applyFacet() {
    const obj = { createdAt: null, createdInLast: null };
    this.$('input').each(function() {
      const property = $(this).prop('name');
      const value = $(this).val();
      obj[property] = value;
    });
    return this.options.app.state.updateFilter(obj);
  },

  disable() {
    return this.options.app.state.updateFilter({
      createdAfter: null,
      createdBefore: null,
      createdAt: null,
      sinceLeakPeriod: null,
      createdInLast: null
    });
  },

  selectBar(e) {
    const periodStart = $(e.currentTarget).data('period-start');
    const periodEnd = $(e.currentTarget).data('period-end');
    return this.options.app.state.updateFilter({
      createdAfter: periodStart,
      createdBefore: periodEnd,
      createdAt: null,
      sinceLeakPeriod: null,
      createdInLast: null
    });
  },

  selectPeriod(period) {
    return this.options.app.state.updateFilter({
      createdAfter: null,
      createdBefore: null,
      createdAt: null,
      sinceLeakPeriod: null,
      createdInLast: period
    });
  },

  onAllClick(e) {
    e.preventDefault();
    return this.disable();
  },

  onLastWeekClick(e) {
    e.preventDefault();
    return this.selectPeriod('1w');
  },

  onLastMonthClick(e) {
    e.preventDefault();
    return this.selectPeriod('1m');
  },

  onLastYearClick(e) {
    e.preventDefault();
    return this.selectPeriod('1y');
  },

  onLeakClick(e) {
    e.preventDefault();
    this.options.app.state.updateFilter({
      createdAfter: null,
      createdBefore: null,
      createdAt: null,
      createdInLast: null,
      sinceLeakPeriod: 'true'
    });
  },

  serializeData() {
    const hasLeak = this.options.app.state.get('contextComponentQualifier') === 'TRK';

    return {
      ...BaseFacet.prototype.serializeData.apply(this, arguments),
      hasLeak,
      periodStart: this.options.app.state.get('query').createdAfter,
      periodEnd: this.options.app.state.get('query').createdBefore,
      createdAt: this.options.app.state.get('query').createdAt,
      sinceLeakPeriod: this.options.app.state.get('query').sinceLeakPeriod,
      createdInLast: this.options.app.state.get('query').createdInLast
    };
  }
});
