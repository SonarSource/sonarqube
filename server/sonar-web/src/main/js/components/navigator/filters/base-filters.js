import $ from 'jquery';
import _ from 'underscore';
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import Template from '../templates/base-filter.hbs';
import DetailsTemplate from '../templates/base-details-filter.hbs';

var Filter = Backbone.Model.extend({

  defaults: {
    enabled: true,
    optional: false,
    multiple: true,
    placeholder: ''
  }

});


var Filters = Backbone.Collection.extend({
  model: Filter
});


var DetailsFilterView = Marionette.ItemView.extend({
  template: DetailsTemplate,
  className: 'navigator-filter-details',


  initialize: function () {
    this.$el.on('click', function (e) {
      e.stopPropagation();
    });
    this.$el.attr('id', 'filter-' + this.model.get('property'));
  },


  onShow: function () {
  },
  onHide: function () {
  }
});


var BaseFilterView = Marionette.ItemView.extend({
  template: Template,
  className: 'navigator-filter',


  events: function () {
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


  initialize: function (options) {
    Marionette.ItemView.prototype.initialize.apply(this, arguments);

    var detailsView = (options && options.detailsView) || DetailsFilterView;
    this.detailsView = new detailsView({
      model: this.model,
      filterView: this
    });

    this.model.view = this;
  },


  attachDetailsView: function () {
    this.detailsView.$el.detach().appendTo($('body'));
  },


  render: function () {
    this.renderBase();

    this.attachDetailsView();
    this.detailsView.render();

    this.$el.toggleClass(
        'navigator-filter-disabled',
        !this.model.get('enabled'));

    this.$el.toggleClass(
        'navigator-filter-optional',
        this.model.get('optional'));
  },


  renderBase: function () {
    Marionette.ItemView.prototype.render.apply(this, arguments);
    this.renderInput();

    var title = this.model.get('name') + ': ' + this.renderValue();
    this.$el.prop('title', title);
    this.$el.attr('data-property', this.model.get('property'));
  },


  renderInput: function () {
  },


  focus: function () {
    this.render();
  },


  toggleDetails: function (e) {
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


  showDetails: function () {
    this.registerShowedDetails();

    var top = this.$el.offset().top + this.$el.outerHeight() - 1,
        left = this.$el.offset().left;

    this.detailsView.$el.css({ top: top, left: left }).addClass('active');
    this.$el.addClass('active');
    this.detailsView.onShow();
  },


  registerShowedDetails: function () {
    this.options.filterBarView.hideDetails();
    this.options.filterBarView.showedView = this;
  },


  hideDetails: function () {
    this.detailsView.$el.removeClass('active');
    this.$el.removeClass('active');
    this.detailsView.onHide();
  },


  isActive: function () {
    return this.$el.is('.active');
  },


  renderValue: function () {
    return this.model.get('value') || 'unset';
  },


  isDefaultValue: function () {
    return true;
  },


  restoreFromQuery: function (q) {
    var param = _.findWhere(q, { key: this.model.get('property') });
    if (param && param.value) {
      this.model.set('enabled', true);
      this.restore(param.value, param);
    } else {
      this.clear();
    }
  },


  restore: function (value) {
    this.model.set({ value: value }, { silent: true });
    this.renderBase();
  },


  clear: function () {
    this.model.unset('value');
  },


  disable: function (e) {
    e.stopPropagation();
    this.hideDetails();
    this.options.filterBarView.hideDetails();
    this.model.set({
      enabled: false,
      value: null
    });
  },


  formatValue: function () {
    var q = {};
    if (this.model.has('property') && this.model.has('value') && this.model.get('value')) {
      q[this.model.get('property')] = this.model.get('value');
    }
    return q;
  },


  serializeData: function () {
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
  Filter: Filter,
  Filters: Filters,
  BaseFilterView: BaseFilterView,
  DetailsFilterView: DetailsFilterView
};


