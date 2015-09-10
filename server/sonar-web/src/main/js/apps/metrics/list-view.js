define([
  './list-item-view',
  './templates'
], function (ListItemView) {

  return Marionette.CollectionView.extend({
    tagName: 'ul',
    childView: ListItemView,

    childViewOptions: function () {
      return {
        types: this.options.types,
        domains: this.options.domains
      };
    }
  });

});
