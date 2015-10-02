import Marionette from 'backbone.marionette';
import ListItemView from './list-item-view';
import Template from './templates/custom-measures-list.hbs';

export default Marionette.CompositeView.extend({
  template: Template,
  childView: ListItemView,
  childViewContainer: 'tbody'
});


