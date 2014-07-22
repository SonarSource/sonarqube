define(['navigator/filters/filter-bar', 'common/handlebars-extensions'], function (FilterBarView) {

  return FilterBarView.extend({
    template: getTemplate('#filter-bar-template')
  });

});
