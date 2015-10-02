import $ from 'jquery';
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import BaseFilters from './base-filters';
import MoreCriteriaFilters from './more-criteria-filters';

export default Marionette.CompositeView.extend({
  childViewContainer: '.navigator-filters-list',


  collectionEvents: {
    'change:enabled': 'changeEnabled'
  },


  getChildView: function (item) {
    return item.get('type') || BaseFilters.BaseFilterView;
  },


  childViewOptions: function () {
    return {
      filterBarView: this,
      app: this.options.app
    };
  },


  initialize: function () {
    Marionette.CompositeView.prototype.initialize.apply(this, arguments);

    var that = this;
    $('body').on('click', function () {
      that.hideDetails();
    });
    this.addMoreCriteriaFilter();

    key.filter = function (e) {
      var r = true,
          el = $(e.target),
          box = el.closest('.navigator-filter-details-inner'),
          tabbableSet = box.find(':tabbable'),
          isElFocusable = el.is(':input') || el.is('a'),
          isInsideDialog = el.closest('.ui-dialog').length > 0;
      if (isElFocusable) {
        if (!isInsideDialog && (e.keyCode === 9 || e.keyCode === 27)) {
          r = tabbableSet.index(el) >= tabbableSet.length - 1;
        } else {
          r = false;
        }
      }
      return r;
    };
    key('tab', 'list', function () {
      key.setScope('filters');
      that.selectFirst();
      return false;
    });
    key('shift+tab', 'filters', function () {
      that.selectPrev();
      return false;
    });
    key('tab', 'filters', function () {
      that.selectNext();
      return false;
    });
    key('escape', 'filters', function () {
      that.hideDetails();
      this.selected = -1;
      key.setScope('list');
    });
  },


  getEnabledFilters: function () {
    return this.$(this.childViewContainer).children()
        .not('.navigator-filter-disabled')
        .not('.navigator-filter-inactive')
        .not('.navigator-filter-favorite');
  },


  selectFirst: function () {
    this.selected = -1;
    this.selectNext();
  },


  selectPrev: function () {
    var filters = this.getEnabledFilters();
    if (this.selected > 0) {
      filters.eq(this.selected).blur();
      this.selected--;
      filters.eq(this.selected).click();
      this.$('.navigator-filter-submit').blur();
    }
  },


  selectNext: function () {
    var filters = this.getEnabledFilters();
    if (this.selected < filters.length - 1) {
      filters.eq(this.selected).blur();
      this.selected++;
      filters.eq(this.selected).click();
    } else {
      this.selected = filters.length;
      this.hideDetails();
      this.$('.navigator-filter-submit').focus();
    }
  },


  addMoreCriteriaFilter: function () {
    var disabledFilters = this.collection.where({ enabled: false });
    if (disabledFilters.length > 0) {
      this.moreCriteriaFilter = new BaseFilters.Filter({
        type: MoreCriteriaFilters.MoreCriteriaFilterView,
        enabled: true,
        optional: false,
        filters: disabledFilters
      });
      this.collection.add(this.moreCriteriaFilter);
    }
  },


  onAddChild: function (childView) {
    if (childView.model.get('type') === MoreCriteriaFilters.FavoriteFilterView) {
      $('.navigator-header').addClass('navigator-header-favorite');
    }
  },


  restoreFromQuery: function (q) {
    this.collection.each(function (item) {
      item.set('enabled', !item.get('optional'));
      item.view.clear();
      item.view.restoreFromQuery(q);
    });
  },


  hideDetails: function () {
    if (_.isObject(this.showedView)) {
      this.showedView.hideDetails();
    }
  },


  enableFilter: function (id) {
    var filter = this.collection.get(id),
        filterView = filter.view;

    filterView.$el.detach().insertBefore(this.$('.navigator-filter-more-criteria'));
    filter.set('enabled', true);
    filterView.showDetails();
  },


  changeEnabled: function () {
    var disabledFilters = _.reject(this.collection.where({ enabled: false }), function (filter) {
      return filter.get('type') === MoreCriteriaFilters.MoreCriteriaFilterView;
    });

    if (disabledFilters.length === 0) {
      this.moreCriteriaFilter.set({ enabled: false }, { silent: true });
    } else {
      this.moreCriteriaFilter.set({ enabled: true }, { silent: true });
    }
    this.moreCriteriaFilter.set('filters', disabledFilters);
  }

});
