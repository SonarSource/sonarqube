define([
  'backbone.marionette',
  './templates'
], function (Marionette) {

  var $ = jQuery;

  return Marionette.ItemView.extend({
    className: 'panel panel-vertical',
    template: Templates['api-documentation-action'],

    modelEvents: {
      'change': 'render'
    },

    events: {
      'click .js-show-response-example': 'onShowResponseExampleClick'
    },

    onRender: function () {
      this.$el.attr('data-web-service', this.model.get('path'));
      this.$el.attr('data-action', this.model.get('key'));
    },

    onShowResponseExampleClick: function (e) {
      e.preventDefault();
      this.fetchResponse();
    },

    fetchResponse: function () {
      var that = this,
          url = baseUrl + '/api/webservices/response_example',
          options = { controller: this.model.get('path'), action: this.model.get('key') };
      return $.get(url, options).done(function (r) {
        that.model.set({ responseExample: r.example });
      });
    }
  });

});
