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
import $ from 'jquery';
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import ProfileHeaderView from './profile-header-view';
import ProfileDetailsView from './profile-details-view';

export default Marionette.Controller.extend({

  initialize () {
    this.listenTo(this.options.app.profiles, 'select', this.onProfileSelect);
    this.listenTo(this.options.app.profiles, 'setAsDefault', this.onProfileSetAsDefault);
    this.listenTo(this.options.app.profiles, 'destroy', this.onProfileDestroy);
  },

  index () {
    this.fetchProfiles();
  },

  show (key) {
    const that = this;
    this.fetchProfiles().done(function () {
      const profile = that.options.app.profiles.findWhere({ key });
      if (profile != null) {
        profile.trigger('select', profile, { trigger: false });
      }
    });
  },

  changelog (key, since, to) {
    const that = this;
    this.anchor = 'changelog';
    this.fetchProfiles().done(function () {
      const profile = that.options.app.profiles.findWhere({ key });
      if (profile != null) {
        profile.trigger('select', profile, { trigger: false });
        profile.fetchChangelog({ since, to });
      }
    });
  },

  compare (key, withKey) {
    const that = this;
    this.anchor = 'comparison';
    this.fetchProfiles().done(function () {
      const profile = that.options.app.profiles.findWhere({ key });
      if (profile != null) {
        profile.trigger('select', profile, { trigger: false });
        profile.compareWith(withKey);
      }
    });
  },

  onProfileSelect (profile, options) {
    const that = this;
    const key = profile.get('key');
    const route = 'show?key=' + encodeURIComponent(key);
    const opts = _.defaults(options || {}, { trigger: true });
    if (opts.trigger) {
      this.options.app.router.navigate(route);
    }
    this.options.app.profilesView.highlight(key);
    this.fetchProfile(profile).done(function () {
      const profileHeaderView = new ProfileHeaderView({
        model: profile,
        canWrite: that.options.app.canWrite
      });
      that.options.app.layout.headerRegion.show(profileHeaderView);

      const profileDetailsView = new ProfileDetailsView({
        model: profile,
        canWrite: that.options.app.canWrite,
        exporters: that.options.app.exporters,
        anchor: that.anchor
      });
      that.options.app.layout.detailsRegion.show(profileDetailsView);

      that.anchor = null;
    });
  },

  onProfileSetAsDefault (profile) {
    const that = this;
    const url = window.baseUrl + '/api/qualityprofiles/set_default';
    const key = profile.get('key');
    const options = { profileKey: key };
    return $.post(url, options).done(function () {
      profile.set({ isDefault: true });
      that.fetchProfiles();
    });
  },

  onProfileDestroy () {
    this.options.app.router.navigate('');
    this.options.app.layout.headerRegion.reset();
    this.options.app.layout.detailsRegion.reset();
    this.options.app.layout.renderIntro();
    this.options.app.profilesView.highlight(null);
  },

  fetchProfiles () {
    return this.options.app.profiles.fetch({ reset: true });
  },

  fetchProfile (profile) {
    return profile.fetch();
  }

});

