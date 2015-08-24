define([
  'backbone.marionette',
  './gate-view',
  './templates'
], function (Marionette, ItemView) {

  return Marionette.CompositeView.extend({
    className: 'list-group',
    template: Templates['quality-gates-gates'],
    childView: ItemView,
    childViewContainer: '.js-list',

    childViewOptions: function (model) {
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
