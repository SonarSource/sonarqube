/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
define([
  './rule'
], function (Rule) {

  return Backbone.Collection.extend({
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

});
