import Backbone from 'backbone';

export default Backbone.Model.extend({

  isDefault: function () {
    return this.get('isDefault');
  },

  url: function () {
    return baseUrl + '/api/qualitygates';
  },

  showUrl: function () {
    return this.url() + '/show';
  },

  deleteUrl: function () {
    return this.url() + '/destroy';
  },

  toggleDefaultUrl: function () {
    var method = this.isDefault() ? 'unset_default' : 'set_as_default';
    return this.url() + '/' + method;
  },

  sync: function (method, model, options) {
    var opts = options || {};
    opts.data = opts.data || {};
    opts.data.id = model.id;
    if (method === 'read') {
      opts.url = this.showUrl();
    }
    if (method === 'delete') {
      opts.url = this.deleteUrl();
      opts.type = 'POST';
    }
    return Backbone.ajax(opts);
  },

  toggleDefault: function () {
    var that = this;
    var opts = {
      type: 'POST',
      url: this.toggleDefaultUrl(),
      data: { id: this.id }
    };
    return Backbone.ajax(opts).done(function () {
      that.collection.toggleDefault(that);
    });
  }

});


