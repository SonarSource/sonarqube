/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import Marionette from 'backbone.marionette';
import ProfileView from './rule-profile-view';
import ProfileActivationView from './profile-activation-view';
import Template from '../templates/rule/coding-rules-rule-profiles.hbs';

export default Marionette.CompositeView.extend({
  template: Template,
  childView: ProfileView,
  childViewContainer: '#coding-rules-detail-quality-profiles',

  childViewOptions() {
    return {
      app: this.options.app,
      rule: this.model,
      refreshActives: this.refreshActives.bind(this)
    };
  },

  modelEvents: {
    change: 'render'
  },

  events: {
    'click #coding-rules-quality-profile-activate': 'activate'
  },

  onRender() {
    let qualityProfilesVisible = true;

    if (this.model.get('isTemplate')) {
      qualityProfilesVisible = this.collection.length > 0;
    }

    this.$el.toggleClass('hidden', !qualityProfilesVisible);
  },

  activate() {
    const that = this;
    const activationView = new ProfileActivationView({
      rule: this.model,
      collection: this.collection,
      app: this.options.app
    });
    activationView.on('profileActivated', (severity, params, profile) => {
      if (that.options.app.state.get('query').qprofile === profile) {
        const activation = {
          severity,
          params,
          inherit: 'NONE',
          qProfile: profile
        };
        that.model.set({ activation });
      }
      that.refreshActives();
    });
    activationView.render();
  },

  refreshActives() {
    const that = this;
    this.options.app.controller.getRuleDetails(this.model).done(data => {
      that.collection.reset(
        that.model.getInactiveProfiles(data.actives, that.options.app.qualityProfiles)
      );
    });
  },

  serializeData() {
    // show "Activate" button only if user has at least one QP which he administates
    const canActivate = this.collection
      .toJSON()
      .some(profile => profile.actions && profile.actions.edit);

    return {
      ...Marionette.ItemView.prototype.serializeData.apply(this, arguments),
      canActivate
    };
  }
});
