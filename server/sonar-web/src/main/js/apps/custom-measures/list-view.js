define([
  './list-item-view',
  './templates'
], function (ListItemView) {

  return Marionette.CompositeView.extend({
    template: Templates['custom-measures-list'],
    childView: ListItemView,
    childViewContainer: 'tbody'
  });

});
