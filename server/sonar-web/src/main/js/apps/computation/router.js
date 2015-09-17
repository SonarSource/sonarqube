import Backbone from 'backbone';

export default Backbone.Router.extend({
  routes: {
    '': 'index',
    'index': 'index',
    'current': 'current',
    'past': 'past'
  },

  initialize: function (options) {
    this.options = options;
  },

  index: function () {
    this.navigate('current');
    this.current();
  },

  current: function () {
    this.options.reports.fetch({ q: 'queue' });
  },

  past: function () {
    this.options.reports.fetch({ q: 'history' });
  }
});


