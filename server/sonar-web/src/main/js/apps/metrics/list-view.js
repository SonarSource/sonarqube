define([
  './list-item-view',
  './templates'
], function (ListItemView) {

  return Marionette.CollectionView.extend({
    tagName: 'ul',
    itemView: ListItemView,

    itemViewOptions: function () {
      return {
        types: this.options.types,
        domains: this.options.domains
      };
    }
  });

});
