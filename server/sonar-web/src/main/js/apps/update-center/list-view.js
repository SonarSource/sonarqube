define([
  'backbone.marionette',
  './list-item-view'
], function (Marionette, ListItemView) {

  return Marionette.CollectionView.extend({
    tagName: 'ul',
    childView: ListItemView
  });

});
