import _ from 'underscore';
import Backbone from 'backbone';

export default Backbone.Model.extend({
  idAttribute: 'id',

  urlRoot: function () {
    return baseUrl + '/api/metrics';
  },

  sync: function (method, model, options) {
    var opts = options || {};
    if (method === 'create') {
      _.defaults(opts, {
        url: this.urlRoot() + '/create',
        type: 'POST',
        data: _.pick(model.toJSON(), 'key', 'name', 'description', 'domain', 'type')
      });
    }
    if (method === 'update') {
      _.defaults(opts, {
        url: this.urlRoot() + '/update',
        type: 'POST',
        data: _.pick(model.toJSON(), 'id', 'key', 'name', 'description', 'domain', 'type')
      });
    }
    if (method === 'delete') {
      _.defaults(opts, {
        url: this.urlRoot() + '/delete',
        type: 'POST',
        data: { ids: this.id }
      });
    }
    return Backbone.ajax(opts);
  }
});


