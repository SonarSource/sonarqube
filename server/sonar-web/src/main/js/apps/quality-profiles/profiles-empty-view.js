define([
  './templates'
], function () {

  return Marionette.ItemView.extend({
    className: 'list-group-item',
    template: Templates['quality-profiles-empty']
  });

});
