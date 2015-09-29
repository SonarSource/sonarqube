import Backbone from 'backbone';

export default Backbone.Collection.extend({
  url: function () {
    return baseUrl + '/api/action_plans/search';
  },

  parse: function (r) {
    return r.actionPlans;
  }
});


