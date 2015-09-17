import Marionette from 'backbone.marionette';
import './templates';

Handlebars.registerHelper('issueFilterHomeLink', function (id) {
  return baseUrl + '/issues/search#id=' + id;
});

export default Marionette.ItemView.extend({
  template: Templates['issues-workspace-home']
});


