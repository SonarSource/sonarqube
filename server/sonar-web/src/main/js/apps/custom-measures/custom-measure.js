import _ from 'underscore';
import Backbone from 'backbone';

export default Backbone.Model.extend({
  idAttribute: 'id',

  urlRoot: function () {
    return baseUrl + '/api/custom_measures';
  },

  sync: function (method, model, options) {
    var opts = options || {};
    if (method === 'create') {
      _.defaults(opts, {
        url: this.urlRoot() + '/create',
        type: 'POST',
        data: _.pick(model.toJSON(), 'metricId', 'value', 'description', 'projectId')
      });
    }
    if (method === 'update') {
      _.defaults(opts, {
        url: this.urlRoot() + '/update',
        type: 'POST',
        data: _.pick(model.toJSON(), 'id', 'value', 'description')
      });
    }
    if (method === 'delete') {
      _.defaults(opts, {
        url: this.urlRoot() + '/delete',
        type: 'POST',
        data: { id: this.id }
      });
    }
    return Backbone.ajax(opts);
  }
});


