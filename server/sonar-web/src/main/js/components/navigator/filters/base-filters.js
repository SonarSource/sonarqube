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
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import Template from '../templates/base-filter.hbs';
import DetailsTemplate from '../templates/base-details-filter.hbs';

const Filter = Backbone.Model.extend({

  defaults: {
    enabled: true,
    optional: false,
    multiple: true,
    placeholder: ''
  }

});

const Filters = Backbone.Collection.extend({
  model: Filter
});

const DetailsFilterView = Marionette.ItemView.extend({
  template: DetailsTemplate,
  className: 'navigator-filter-details',

  initialize () {
    this.$el.on('click', function (e) {
      e.stopPropagation();
    });
    this.$el.attr('id', 'filter-' + this.model.get('property'));
  },

  onShow () {
  },

  onHide () {
  }
});

const BaseFilterView = Marionette.ItemView.extend({
  template: Template,
  className: 'navigator-filter',

  events () {
    return {
      'click': 'toggleDetails',
      'click .navigator-filter-disable': 'disable'
    };
  },

  modelEvents: {
    'change:enabled': 'focus',
    'change:value': 'renderBase',

    // for more criteria filter
    'change:filters': 'render'
  },

  initialize (options) {
    Marionette.ItemView.prototype.initialize.apply(this, arguments);

    const DetailsView = (options && options.projectsView) || DetailsFilterView;
    this.projectsView = new DetailsView({
      model: this.model,
      filterView: this
    });

    this.model.view = this;
  },

  attachDetailsView () {
    this.projectsView.$el.detach().appendTo($('body'));
  },

  render () {
    this.renderBase();

    this.attachDetailsView();
    this.projectsView.render();

    this.$el.toggleClass(
        'navigator-filter-disabled',
        !this.model.get('enabled'));

    this.$el.toggleClass(
        'navigator-filter-optional',
        this.model.get('optional'));
  },

  renderBase () {
    Marionette.ItemView.prototype.render.apply(this, arguments);
    this.renderInput();

    const title = this.model.get('name') + ': ' + this.renderValue();
    this.$el.prop('title', title);
    this.$el.attr('data-property', this.model.get('property'));
  },

  renderInput () {
  },

  focus () {
    this.render();
  },

  toggleDetails (e) {
    e.stopPropagation();
    this.options.filterBarView.selected = this.options.filterBarView.getEnabledFilters().index(this.$el);
    if (this.$el.hasClass('active')) {
      key.setScope('list');
      this.hideDetails();
    } else {
      key.setScope('filters');
      this.showDetails();
    }
  },

  showDetails () {
    this.registerShowedDetails();

    const top = this.$el.offset().top + this.$el.outerHeight() - 1;
    const left = this.$el.offset().left;

    this.projectsView.$el.css({ top, left }).addClass('active');
    this.$el.addClass('active');
    this.projectsView.onShow();
  },

  registerShowedDetails () {
    this.options.filterBarView.hideDetails();
    this.options.filterBarView.showedView = this;
  },

  hideDetails () {
    this.projectsView.$el.removeClass('active');
    this.$el.removeClass('active');
    this.projectsView.onHide();
  },

  isActive () {
    return this.$el.is('.active');
  },

  renderValue () {
    return this.model.get('value') || 'unset';
  },

  isDefaultValue () {
    return true;
  },

  restoreFromQuery (q) {
    const param = _.findWhere(q, { key: this.model.get('property') });
    if (param && param.value) {
      this.model.set('enabled', true);
      this.restore(param.value, param);
    } else {
      this.clear();
    }
  },

  restore (value) {
    this.model.set({ value }, { silent: true });
    this.renderBase();
  },

  clear () {
    this.model.unset('value');
  },

  disable (e) {
    e.stopPropagation();
    this.hideDetails();
    this.options.filterBarView.hideDetails();
    this.model.set({
      enabled: false,
      value: null
    });
  },

  formatValue () {
    const q = {};
    if (this.model.has('property') && this.model.has('value') && this.model.get('value')) {
      q[this.model.get('property')] = this.model.get('value');
    }
    return q;
  },

  serializeData () {
    return _.extend({}, this.model.toJSON(), {
      value: this.renderValue(),
      defaultValue: this.isDefaultValue()
    });
  }

});

/*
 * Export public classes
 */

export default {
  Filter,
  Filters,
  BaseFilterView,
  DetailsFilterView
};
