define([
  './templates'
], function () {

  return Marionette.ItemView.extend({
    template: Templates['api-documentation-header'],

    modelEvents: {
      'change': 'render'
    }
  });

});
