define([
  'backbone',
  'dashboard/models/widget'
], function (
    Backbone,
    Widget) {

  return Backbone.Collection.extend({
    model: Widget,

    comparator: function(model) {
      return model.get('layout').row;
    }
  });

});
