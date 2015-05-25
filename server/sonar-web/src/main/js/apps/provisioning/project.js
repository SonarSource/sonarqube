define(function () {

  return Backbone.Model.extend({
    idAttribute: 'uuid',

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
          url: this.urlRoot() + '/delete',
          type: 'POST',
          data: { uuids: this.id }
        });
      }
      return Backbone.ajax(opts);
    }
  });

});
