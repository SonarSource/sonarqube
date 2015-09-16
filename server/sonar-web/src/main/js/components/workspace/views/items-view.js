define([
  './item-view',
  '../templates'
], function (ItemView) {

  return Marionette.CompositeView.extend({
    className: 'workspace-nav',
    template: Templates['workspace-items'],
    childViewContainer: '.workspace-nav-list',
    childView: ItemView,

    childViewOptions: function () {
      return { collectionView: this };
    }
  });

});
