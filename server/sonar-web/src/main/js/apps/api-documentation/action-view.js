import $ from 'jquery';
import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.ItemView.extend({
  className: 'panel panel-vertical',
  template: Templates['api-documentation-action'],

  modelEvents: {
    'change': 'render'
  },

  events: {
    'click .js-show-response-example': 'onShowResponseExampleClick',
    'click .js-hide-response-example': 'onHideResponseExampleClick'
  },

  initialize: function () {
    this.listenTo(this.options.state, 'change:query', this.toggleHidden);
  },

  onRender: function () {
    this.$el.attr('data-web-service', this.model.get('path'));
    this.$el.attr('data-action', this.model.get('key'));
    this.toggleHidden();
  },

  onShowResponseExampleClick: function (e) {
    e.preventDefault();
    this.fetchResponse();
  },

  onHideResponseExampleClick: function (e) {
    e.preventDefault();
    this.model.unset('responseExample');
  },

  fetchResponse: function () {
    var that = this,
        url = baseUrl + '/api/webservices/response_example',
        options = { controller: this.model.get('path'), action: this.model.get('key') };
    return $.get(url, options).done(function (r) {
      that.model.set({ responseExample: r.example });
    });
  },

  toggleHidden: function () {
    var test = this.model.get('path') + '/' + this.model.get('key');
    this.$el.toggleClass('hidden', !this.options.state.match(test));
  }
});
