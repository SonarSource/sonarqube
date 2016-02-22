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
import Template from '../templates/string-filter.hbs';

const DetailsStringFilterView = BaseFilters.DetailsFilterView.extend({
  template: Template,


  events: {
    'change input': 'change'
  },


  change (e) {
    this.model.set('value', $(e.target).val());
  },


  onShow () {
    BaseFilters.DetailsFilterView.prototype.onShow.apply(this, arguments);
    this.$(':input').focus();
  },


  serializeData () {
    return _.extend({}, this.model.toJSON(), {
      value: this.model.get('value') || ''
    });
  }

});


export default BaseFilters.BaseFilterView.extend({

  initialize () {
    BaseFilters.BaseFilterView.prototype.initialize.call(this, {
      detailsView: DetailsStringFilterView
    });
  },


  renderValue () {
    return this.isDefaultValue() ? '—' : this.model.get('value');
  },


  renderInput () {
    $('<input>')
        .prop('name', this.model.get('property'))
        .prop('type', 'hidden')
        .css('display', 'none')
        .val(this.model.get('value') || '')
        .appendTo(this.$el);
  },


  isDefaultValue () {
    return !this.model.get('value');
  },


  restore (value) {
    this.model.set({
      value,
      enabled: true
    });
  },


  clear () {
    this.model.unset('value');
    this.detailsView.render();
  }

});


