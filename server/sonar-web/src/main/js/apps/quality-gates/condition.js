import Backbone from 'backbone';

export default Backbone.Model.extend({

  defaults: {
    period: 0
  },

  url: function () {
    return baseUrl + '/api/qualitygates';
  },

  createUrl: function () {
    return this.url() + '/create_condition';
  },

  updateUrl: function () {
    return this.url() + '/update_condition';
  },

  deleteUrl: function () {
    return this.url() + '/delete_condition';
  },

  sync: function (method, model, options) {
    var opts = options || {};
    opts.type = 'POST';
    if (method === 'create') {
      opts.url = this.createUrl();
      opts.data = model.toJSON();
    }
    if (method === 'update') {
      opts.url = this.updateUrl();
      opts.data = model.toJSON();
    }
    if (method === 'delete') {
      opts.url = this.deleteUrl();
      opts.data = { id: model.id };
    }
    if (opts.data.period === '0') {
      delete opts.data.period;
    }
    return Backbone.ajax(opts);
  }
});


