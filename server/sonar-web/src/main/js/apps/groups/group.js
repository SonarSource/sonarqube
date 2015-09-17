import _ from 'underscore';
import Backbone from 'backbone';

export default Backbone.Model.extend({
  urlRoot: function () {
    return baseUrl + '/api/usergroups';
  },

  sync: function (method, model, options) {
    var opts = options || {};
    if (method === 'create') {
      _.defaults(opts, {
        url: this.urlRoot() + '/create',
        type: 'POST',
        data: _.pick(model.toJSON(), 'name', 'description')
      });
    }
    if (method === 'update') {
      var attrs = _.extend(_.pick(model.changed, 'name', 'description'), { id: model.id });
      _.defaults(opts, {
        url: this.urlRoot() + '/update',
        type: 'POST',
        data: attrs
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


