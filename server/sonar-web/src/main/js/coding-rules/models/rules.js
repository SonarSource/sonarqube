define([
  'backbone',
  'coding-rules/models/rule'
], function (Backbone, Rule) {

  return Backbone.Collection.extend({
    model: Rule,

    parseRules: function (r) {
      return r.rules;
    },

    setIndex: function () {
      this.forEach(function (rule, index) {
        rule.set({ index: index });
      });
    }
  });

});
