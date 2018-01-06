/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import Backbone from 'backbone';
import Rule from './rule';

export default Backbone.Collection.extend({
  model: Rule,

  parseRules(r) {
    let rules = r.rules;
    const profiles = r.qProfiles || [];

    if (r.actives != null) {
      rules = rules.map(rule => {
        const activations = (r.actives[rule.key] || []).map(activation => {
          const profile = profiles[activation.qProfile];
          if (profile != null) {
            Object.assign(activation, { profile });
            if (profile.parent != null) {
              Object.assign(activation, { parentProfile: profiles[profile.parent] });
            }
          }
          return activation;
        });
        return { ...rule, activation: activations.length > 0 ? activations[0] : null };
      });
    }
    return rules;
  },

  setIndex() {
    this.forEach((rule, index) => {
      rule.set({ index });
    });
  },

  addExtraAttributes(repositories) {
    this.models.forEach(model => {
      model.addExtraAttributes(repositories);
    });
  }
});
