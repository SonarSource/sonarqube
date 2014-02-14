define(['navigator/filters/base-filters', 'navigator/filters/select-filters'], function (BaseFilters, SelectFilters) {

  var DetailsMoreCriteriaFilterView = BaseFilters.DetailsFilterView.extend({
    template: '#detailsMoreCriteriaFilterTemplate',


    events: {
      'click label[data-id]': 'enableFilter'
    },


    enableFilter: function(e) {
      var id = $j(e.target).data('id');
      this.model.view.options.filterBarView.enableFilter(id);
      this.model.view.hideDetails();
    }

  });



  var MoreCriteriaFilterView = SelectFilters.SelectFilterView.extend({
    template: '#moreCriteriaFilterTemplate',
    className: 'navigator-filter navigator-filter-more-criteria',


    initialize: function() {
      BaseFilters.BaseFilterView.prototype.initialize.call(this, {
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
