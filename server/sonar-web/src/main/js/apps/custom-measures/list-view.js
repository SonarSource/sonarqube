import Marionette from 'backbone.marionette';
import ListItemView from './list-item-view';
import './templates';

export default Marionette.CompositeView.extend({
  template: Templates['custom-measures-list'],
  childView: ListItemView,
  childViewContainer: 'tbody'
});


