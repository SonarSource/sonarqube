define([
  'backbone.marionette',
  './templates'
], function (Marionette) {

  return Marionette.ItemView.extend({
    template: Templates['api-documentation-header'],

    modelEvents: {
      'change': 'render'
    }
  });

});
