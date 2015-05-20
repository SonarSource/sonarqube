define([
  './list-item-view',
  './templates'
], function (ListItemView) {

  return Marionette.CompositeView.extend({
    template: Templates['users-list'],
    itemViewContainer: 'tbody',
    itemView: ListItemView
  });

});
