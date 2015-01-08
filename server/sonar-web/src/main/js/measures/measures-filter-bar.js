define(['navigator/filters/filter-bar'], function (FilterBarView) {

  return FilterBarView.extend({
    template: function () {
      return jQuery('#filter-bar-template').html();
    }
  });

});
