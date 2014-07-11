define(['navigator/filters/base-filters'], function(BaseFilters) {

  describe('BaseFilterView', function() {

    it('initializes', function() {
      var baseFilterView = new BaseFilters.BaseFilterView({
        model: new BaseFilters.Filter()
      });
      expect(baseFilterView.detailsView).toBeDefined();
      expect(baseFilterView.detailsView.options.filterView).toBe(baseFilterView);
    });

  });

});
