import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.ItemView.extend({
  className: 'list-group-item',
  template: Templates['quality-profiles-empty']
});


