define([
  './gate-view',
  './templates'
], function (ItemView) {

  return Marionette.CompositeView.extend({
    className: 'list-group',
    template: Templates['quality-gates-gates'],
    itemView: ItemView,
    itemViewContainer: '.js-list',

    itemViewOptions: function (model) {
      return {
        collectionView: this,
        highlighted: model.id === this.highlighted
      };
    },

    highlight: function (id) {
      this.highlighted = id;
      this.render();
    }
  });

});
