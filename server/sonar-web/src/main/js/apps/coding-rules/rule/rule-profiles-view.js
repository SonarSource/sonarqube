/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import ProfileView from './rule-profile-view';
import ProfileActivationView from './profile-activation-view';
import Template from '../templates/rule/coding-rules-rule-profiles.hbs';

export default Marionette.CompositeView.extend({
  template: Template,
  childView: ProfileView,
  childViewContainer: '#coding-rules-detail-quality-profiles',

  childViewOptions: function () {
    return {
      app: this.options.app,
      rule: this.model,
      refreshActives: this.refreshActives.bind(this)
    };
  },

  modelEvents: {
    'change': 'render'
  },

  events: {
    'click #coding-rules-quality-profile-activate': 'activate'
  },

  onRender: function () {
    var isManual = this.model.get('isManual'),
        qualityProfilesVisible = !isManual;

    if (qualityProfilesVisible) {
      if (this.model.get('isTemplate')) {
        qualityProfilesVisible = this.collection.length > 0;
      }
      else {
        qualityProfilesVisible = (this.options.app.canWrite || this.collection.length > 0);
      }
    }

    this.$el.toggleClass('hidden', !qualityProfilesVisible);
  },

  activate: function () {
    var that = this,
        activationView = new ProfileActivationView({
          rule: this.model,
          collection: this.collection,
          app: this.options.app
        });
    activationView.on('profileActivated', function (severity, params, profile) {
      if (that.options.app.state.get('query').qprofile === profile) {
        var activation = {
          severity: severity,
          inherit: 'NONE',
          params: params,
          qProfile: profile
        };
        that.model.set({ activation: activation });
      }
      that.refreshActives();
    });
    activationView.render();
  },

  refreshActives: function () {
    var that = this;
    this.options.app.controller.getRuleDetails(this.model).done(function (data) {
      that.collection.reset(that.model.getInactiveProfiles(data.actives, that.options.app.qualityProfiles));
    });
  },

  serializeData: function () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      canWrite: this.options.app.canWrite
    });
  }
});
