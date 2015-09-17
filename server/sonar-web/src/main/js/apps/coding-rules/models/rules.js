import _ from 'underscore';
import Backbone from 'backbone';
import Rule from './rule';

export default Backbone.Collection.extend({
  model: Rule,

  parseRules: function (r) {
    var rules = r.rules,
        profiles = r.qProfiles || [];

    if (r.actives != null) {
      rules = rules.map(function (rule) {
        var activations = (r.actives[rule.key] || []).map(function (activation) {
          var profile = profiles[activation.qProfile];
          if (profile != null) {
            _.extend(activation, { profile: profile });
            if (profile.parent != null) {
              _.extend(activation, { parentProfile: profiles[profile.parent] });
            }
          }
          return activation;
        });
        return _.extend(rule, { activation: activations.length > 0 ? activations[0] : null });
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


