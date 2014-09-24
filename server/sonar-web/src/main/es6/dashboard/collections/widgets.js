define([
  'backbone',
  'dashboard/models/widget'
], function (
    Backbone,
    Widget) {

  return Backbone.Collection.extend({
    model: Widget
  });

});
