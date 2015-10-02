import Marionette from 'backbone.marionette';
import Handlebars from 'hbsfy/runtime';
import Template from './templates/issues-workspace-home.hbs';

Handlebars.registerHelper('issueFilterHomeLink', function (id) {
  return baseUrl + '/issues/search#id=' + id;
});

export default Marionette.ItemView.extend({
  template: Template
});


