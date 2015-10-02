import Marionette from 'backbone.marionette';
import Template from './templates/quality-profiles-empty.hbs';

export default Marionette.ItemView.extend({
  className: 'list-group-item',
  template: Template
});


