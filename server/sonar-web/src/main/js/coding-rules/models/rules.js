define([
  'coding-rules/models/rule'
], function (Rule) {

  return Backbone.Collection.extend({
    model: Rule,

    parseRules: function (r) {
      var rules = r.rules;
      if (r.actives != null) {
        rules = rules.map(function (rule) {
          var profiles = r.actives[rule.key];
          return _.extend(rule, { activeProfiles: profiles });
        });
      }
      return rules;
    },

    setIndex: function () {
      this.forEach(function (rule, index) {
        rule.set({ index: index });
      });
    },

    addExtraAttributes: function (repositories) {
      this.models.forEach(function (model) {
        model.addExtraAttributes(repositories);
      });
    }
  });

});
