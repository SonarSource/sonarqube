define([
  'backbone.marionette',
  './list-item-view',
  './templates'
], function (Marionette, ListItemView) {

  return Marionette.CollectionView.extend({
    tagName: 'ul',
    childView: ListItemView
  });

});
