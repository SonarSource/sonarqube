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

<<<<<<< d3fd3a3175fac49d0c2874dc33e06497d4505de1
  current: function () {
    this.options.reports.fetch({ q: 'queue' });
  },
=======
    current: function () {
      this.options.reports.fetch({ q: 'queue' });
    },

    past: function () {
      this.options.reports.fetch({ q: 'activity' });
    }
  });
>>>>>>> SONAR-6834 use new API

  past: function () {
    this.options.reports.fetch({ q: 'history' });
  }
});


