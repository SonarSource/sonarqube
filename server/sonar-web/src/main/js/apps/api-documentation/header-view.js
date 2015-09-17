import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.ItemView.extend({
  template: Templates['api-documentation-header'],

  modelEvents: {
    'change': 'render'
  }
});


