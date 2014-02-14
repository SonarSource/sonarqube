/* global _:false, $j:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

(function() {

  var DetailsMoreCriteriaFilterView = window.SS.DetailsFilterView.extend({
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



  var MoreCriteriaFilterView = window.SS.SelectFilterView.extend({
    template: '#moreCriteriaFilterTemplate',
    className: 'navigator-filter navigator-filter-more-criteria',


    initialize: function() {
      window.SS.BaseFilterView.prototype.initialize.call(this, {
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

  _.extend(window.SS, {
    MoreCriteriaFilterView: MoreCriteriaFilterView
  });

})();
