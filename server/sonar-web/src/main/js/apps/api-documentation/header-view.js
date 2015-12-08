import Marionette from 'backbone.marionette';
import Template from './templates/api-documentation-header.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  modelEvents: {
    'change': 'render'
  }
});
