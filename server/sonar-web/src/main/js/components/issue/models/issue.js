define(function () {

  return Backbone.Model.extend({
    idAttribute: 'key',

    url: function () {
      return baseUrl + '/api/issues/show?key=' + this.get('key');
    },

    urlRoot: function () {
      return baseUrl + '/api/issues';
    },

    parse: function (r) {
      return r.issue ? r.issue : r;
    },

    sync: function (method, model, options) {
      var opts = options || {};
      opts.contentType = 'application/x-www-form-urlencoded';
      if (method === 'read') {
        _.extend(opts, {
          type: 'GET',
          url: this.urlRoot() + '/show',
          data: { key: model.id }
        });
      }
      if (method === 'create') {
        _.extend(opts, {
          type: 'POST',
          url: this.urlRoot() + '/create',
          data: {
            component: model.get('component'),
            line: model.get('line'),
            message: model.get('message'),
            rule: model.get('rule'),
            severity: model.get('severity')
          }
        });
      }
      var xhr = options.xhr = Backbone.ajax(opts);
      model.trigger('request', model, xhr, options);
      return xhr;
    }
  });

});
