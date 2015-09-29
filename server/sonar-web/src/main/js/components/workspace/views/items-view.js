import Marionette from 'backbone.marionette';
import ItemView from './item-view';
import '../templates';

export default Marionette.CompositeView.extend({
  className: 'workspace-nav',
  template: Templates['workspace-items'],
  childViewContainer: '.workspace-nav-list',
  childView: ItemView,

  childViewOptions: function () {
    return { collectionView: this };
  }
});


