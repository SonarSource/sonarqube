define([
  './templates'
], function () {

  Handlebars.registerHelper('issueFilterHomeLink', function (id) {
    return baseUrl + '/issues/search#id=' + id;
  });

  return Marionette.ItemView.extend({
    template: Templates['issues-workspace-home']
  });

});
