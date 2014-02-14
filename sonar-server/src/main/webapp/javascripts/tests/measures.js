/* jshint undef:false */

test("filter defaults", function() {
  var filter = new window.SS.Filter();
  strictEqual(true, filter.get('multiple'), "multiple doesn't equal true");
  equal(false, filter.get('placeholder'), "placeholder doesn't equal ''");
});

test("is model.view correctly set", function() {
  var filter = new window.SS.Filter(),
      filterView = new window.SS.BaseFilterView({
        model: filter
      });
  strictEqual(filterView, filter.view, "model.view doesn't model's view");
});

test("is details view set to default", function() {
  var filter = new window.SS.Filter(),
      filterView = new window.SS.BaseFilterView({
        model: filter
      });
  ok(filterView.detailsView instanceof window.SS.DetailsFilterView, "");
});
