import Marionette from 'backbone.marionette';
import ListItemView from './list-item-view';
import './templates';

export default Marionette.CollectionView.extend({
  tagName: 'ul',
  childView: ListItemView
});


