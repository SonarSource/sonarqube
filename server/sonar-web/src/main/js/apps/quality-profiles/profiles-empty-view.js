define([
  'backbone.marionette',
  './templates'
], function (Marionette) {

  return Marionette.ItemView.extend({
    className: 'list-group-item',
    template: Templates['quality-profiles-empty']
  });

});
