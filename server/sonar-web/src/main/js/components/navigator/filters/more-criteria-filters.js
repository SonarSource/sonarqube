define([
  'jquery',
  './base-filters',
  './choice-filters',
  '../templates'
], function ($, BaseFilters, ChoiceFilters) {

  var DetailsMoreCriteriaFilterView = ChoiceFilters.DetailsChoiceFilterView.extend({
    template: Templates['more-criteria-details-filter'],


    events: {
      'click label[data-id]:not(.inactive)': 'enableFilter'
    },


    enableById: function(id) {
      this.model.view.options.filterBarView.enableFilter(id);
      this.model.view.hideDetails();
    },


    enableByProperty: function(property) {
      var filter = _.find(this.model.get('filters'), function(f) {
        return f.get('property') === property;
      });
      if (filter) {
        this.enableById(filter.cid);
      }
    },


    enableFilter: function(e) {
      var id = $(e.target).data('id');
      this.enableById(id);
      this.updateCurrent(0);
    },


    selectCurrent: function() {
      this.$('label').eq(this.currentChoice).click();
    },


    serializeData: function() {
      var filters = this.model.get('filters').map(function(filter) {
            return _.extend(filter.toJSON(), { id: filter.cid });
          }),
          getName = function(filter) {
            return filter.name;
          },
          uniqueFilters = _.unique(filters, getName),
          sortedFilters = _.sortBy(uniqueFilters, getName);
      return _.extend(this.model.toJSON(), { filters: sortedFilters });
    }

  });



  var MoreCriteriaFilterView = ChoiceFilters.ChoiceFilterView.extend({
    template: Templates['more-criteria-filter'],
    className: 'navigator-filter navigator-filter-more-criteria',


    initialize: function() {
      ChoiceFilters.ChoiceFilterView.prototype.initialize.call(this, {
        detailsView: DetailsMoreCriteriaFilterView
      });
    },


    renderValue: function() {
      return '';
    },


    renderInput: function() {},


    renderBase: function() {
      ChoiceFilters.ChoiceFilterView.prototype.renderBase.call(this);
      this.$el.prop('title', '');
    },


    isDefaultValue: function() {
      return false;
    }

  });



  /*
   * Export public classes
   */

  return {
    DetailsMoreCriteriaFilterView: DetailsMoreCriteriaFilterView,
    MoreCriteriaFilterView: MoreCriteriaFilterView
  };

});
