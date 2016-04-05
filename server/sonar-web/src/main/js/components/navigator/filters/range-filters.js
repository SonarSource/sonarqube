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
import $ from 'jquery';
import _ from 'underscore';
import BaseFilters from './base-filters';
import Template from '../templates/range-filter.hbs';
import { translate } from '../../../helpers/l10n';

const DetailsRangeFilterView = BaseFilters.DetailsFilterView.extend({
  template: Template,

  events: {
    'change input': 'change'
  },

  change () {
    const value = {};
    const valueFrom = this.$('input').eq(0).val();
    const valueTo = this.$('input').eq(1).val();

    if (valueFrom.length > 0) {
      value[this.model.get('propertyFrom')] = valueFrom;
    }

    if (valueTo.length > 0) {
      value[this.model.get('propertyTo')] = valueTo;
    }

    this.model.set('value', value);
  },

  populateInputs () {
    const value = this.model.get('value');
    const propertyFrom = this.model.get('propertyFrom');
    const propertyTo = this.model.get('propertyTo');
    const valueFrom = _.isObject(value) && value[propertyFrom];
    const valueTo = _.isObject(value) && value[propertyTo];

    this.$('input').eq(0).val(valueFrom || '');
    this.$('input').eq(1).val(valueTo || '');
  },

  onShow () {
    this.$(':input:first').focus();
  }

});

const RangeFilterView = BaseFilters.BaseFilterView.extend({

  initialize () {
    BaseFilters.BaseFilterView.prototype.initialize.call(this, {
      projectsView: DetailsRangeFilterView
    });
  },

  renderValue () {
    if (!this.isDefaultValue()) {
      const value = _.values(this.model.get('value'));
      return value.join(' — ');
    } else {
      return translate('any');
    }
  },

  renderInput () {
    const value = this.model.get('value');
    const propertyFrom = this.model.get('propertyFrom');
    const propertyTo = this.model.get('propertyTo');
    const valueFrom = _.isObject(value) && value[propertyFrom];
    const valueTo = _.isObject(value) && value[propertyTo];

    $('<input>')
        .prop('name', propertyFrom)
        .prop('type', 'hidden')
        .css('display', 'none')
        .val(valueFrom || '')
        .appendTo(this.$el);

    $('<input>')
        .prop('name', propertyTo)
        .prop('type', 'hidden')
        .css('display', 'none')
        .val(valueTo || '')
        .appendTo(this.$el);
  },

  isDefaultValue () {
    const value = this.model.get('value');
    const propertyFrom = this.model.get('propertyFrom');
    const propertyTo = this.model.get('propertyTo');
    const valueFrom = _.isObject(value) && value[propertyFrom];
    const valueTo = _.isObject(value) && value[propertyTo];

    return !valueFrom && !valueTo;
  },

  restoreFromQuery (q) {
    const paramFrom = _.findWhere(q, { key: this.model.get('propertyFrom') });
    const paramTo = _.findWhere(q, { key: this.model.get('propertyTo') });
    const value = {};

    if ((paramFrom && paramFrom.value) || (paramTo && paramTo.value)) {
      if (paramFrom && paramFrom.value) {
        value[this.model.get('propertyFrom')] = paramFrom.value;
      }

      if (paramTo && paramTo.value) {
        value[this.model.get('propertyTo')] = paramTo.value;
      }

      this.model.set({
        value,
        enabled: true
      });

      this.projectsView.populateInputs();
    }
  },

  restore (value) {
    if (this.choices && this.selection && value.length > 0) {
      const that = this;
      this.choices.add(this.selection.models);
      this.selection.reset([]);

      _.each(value, function (v) {
        const cModel = that.choices.findWhere({ id: v });

        if (cModel) {
          that.selection.add(cModel);
          that.choices.remove(cModel);
        }
      });

      this.projectsView.updateLists();

      this.model.set({
        value,
        enabled: true
      });
    }
  },

  formatValue () {
    return this.model.get('value');
  },

  clear () {
    this.model.unset('value');
    this.projectsView.render();
  }

});

const DateRangeFilterView = RangeFilterView.extend({

  render () {
    RangeFilterView.prototype.render.apply(this, arguments);
    this.projectsView.$('input')
        .prop('placeholder', '1970-01-31')
        .datepicker({
          dateFormat: 'yy-mm-dd',
          changeMonth: true,
          changeYear: true
        })
        .on('change', function () {
          $(this).datepicker('setDate', $(this).val());
        });
  },

  renderValue () {
    if (!this.isDefaultValue()) {
      const value = _.values(this.model.get('value'));
      return value.join(' — ');
    } else {
      return translate('anytime');
    }
  }

});

/*
 * Export public classes
 */

export default {
  RangeFilterView,
  DateRangeFilterView
};

