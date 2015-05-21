define([
  './list-item-view',
  './templates'
], function (ListItemView) {

  return Marionette.CollectionView.extend({
    tagName: 'ul',
    itemView: ListItemView
  });

});
