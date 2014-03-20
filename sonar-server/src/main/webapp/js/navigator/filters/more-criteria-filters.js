define(['navigator/filters/base-filters', 'navigator/filters/choice-filters', 'common/handlebars-extensions'], function (BaseFilters, ChoiceFilters) {

  var DetailsMoreCriteriaFilterView = BaseFilters.DetailsFilterView.extend({
    template: getTemplate('#more-criteria-details-filter-template'),


    events: {
      'click label[data-id]:not(.inactive)': 'enableFilter'
    },


    enableFilter: function(e) {
      var id = $j(e.target).data('id');
      this.model.view.options.filterBarView.enableFilter(id);
      this.model.view.hideDetails();
    },


    serializeData: function() {
      var filters = this.model.get('filters').map(function(filter) {
            return _.extend(filter.toJSON(), { id: filter.cid });
          }),
          uniqueFilters = _.unique(filters, function(filter) {
            return filter.name;
          });
      return _.extend(this.model.toJSON(), { filters: uniqueFilters });
    }

  });



  var MoreCriteriaFilterView = ChoiceFilters.ChoiceFilterView.extend({
    template: getTemplate('#more-criteria-filter-template'),
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
