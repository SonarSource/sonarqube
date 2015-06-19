define([
  './templates'
], function () {

  return Marionette.ItemView.extend({
    template: Templates['maintenance-main'],

    initialize: function () {
      var that = this;
      this.requestOptions = { type: 'GET', url: baseUrl + '/api/system/status' };
      setInterval(function () {
        that.refresh();
      }, 5000);
    },

    refresh: function () {
      var that = this;
      return Backbone.ajax(this.requestOptions).done(function (r) {
        that.model.set(r);
        that.render();
      });
    },

    serializeData: function () {
      return _.extend(this._super(), { setup: this.options.setup });
    }
  });

});
