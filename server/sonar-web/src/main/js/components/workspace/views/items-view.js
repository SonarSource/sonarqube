import Marionette from 'backbone.marionette';
import ItemView from './item-view';
import Template from '../templates/workspace-items.hbs';

export default Marionette.CompositeView.extend({
  className: 'workspace-nav',
  template: Template,
  childViewContainer: '.workspace-nav-list',
  childView: ItemView,

  childViewOptions: function () {
    return { collectionView: this };
  }
});


