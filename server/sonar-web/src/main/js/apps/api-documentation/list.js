import _ from 'underscore';
import Backbone from 'backbone';

export default Backbone.Collection.extend({
  url: baseUrl + '/api/webservices/list',
  comparator: 'path',

  parse: function (r) {
    return r.webServices.map(function (webService) {
      var internal = _.every(webService.actions, function (action) {
            return action.internal;
          }),
          actions = webService.actions.map(function (action) {
            return _.extend(action, { path: webService.path });
          });
      return _.extend(webService, {
        internal: internal,
        actions: actions
      });
    });
  }
});


