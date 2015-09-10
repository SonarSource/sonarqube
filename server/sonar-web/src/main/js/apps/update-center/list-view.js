define([
  './list-item-view'
], function (ListItemView) {

  return Marionette.CollectionView.extend({
    tagName: 'ul',
    childView: ListItemView
  });

});
