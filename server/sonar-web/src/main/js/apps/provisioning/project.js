import _ from 'underscore';
import Backbone from 'backbone';

export default Backbone.Model.extend({
  idAttribute: 'uuid',

  defaults: {
    selected: false
  },

  urlRoot: function () {
    return baseUrl + '/api/projects';
  },

  sync: function (method, model, options) {
    var opts = options || {};
    if (method === 'create') {
      _.defaults(opts, {
        url: this.urlRoot() + '/create',
        type: 'POST',
        data: _.pick(model.toJSON(), 'key', 'name', 'branch')
      });
    }
    if (method === 'delete') {
      _.defaults(opts, {
        url: this.urlRoot() + '/bulk_delete',
        type: 'POST',
        data: { ids: this.id }
      });
    }
    return Backbone.ajax(opts);
  },

  toggle: function () {
    this.set({ selected: !this.get('selected') });
  }
});


