define([
  'coding-rules/models/rule'
], function (Rule) {

  return Backbone.Collection.extend({
    model: Rule,

    parseRules: function (r) {
      var rules = r.rules,
          profileBases = r.qProfiles || [];

      if (r.actives != null) {
        rules = rules.map(function (rule) {
          var profiles = (r.actives[rule.key] || []).map(function (profile) {
            _.extend(profile, profileBases[profile.qProfile]);
            if (profile.parent != null) {
              _.extend(profile, { parentProfile: profileBases[profile.parent] });
            }
            return profile;
          });
          return _.extend(rule, { activeProfile: profiles.length > 0 ? profiles[0] : null });
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
