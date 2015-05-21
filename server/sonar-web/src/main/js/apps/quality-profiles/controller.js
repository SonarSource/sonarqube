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
  './profile-header-view',
  './profile-details-view'
], function (ProfileHeaderView, ProfileDetailsView) {

  var $ = jQuery;

  return Marionette.Controller.extend({

    initialize: function () {
      this.listenTo(this.options.app.profiles, 'select', this.onProfileSelect);
      this.listenTo(this.options.app.profiles, 'setAsDefault', this.onProfileSetAsDefault);
      this.listenTo(this.options.app.profiles, 'destroy', this.onProfileDestroy);
    },

    index: function () {
      this.fetchProfiles();
    },

    show: function (key) {
      var that = this;
      this.fetchProfiles().done(function () {
        var profile = that.options.app.profiles.findWhere({ key: key });
        if (profile != null) {
          profile.trigger('select', profile, { trigger: false });
        }
      });
    },

    changelog: function (key, since, to) {
      var that = this;
      this.anchor = 'changelog';
      this.fetchProfiles().done(function () {
        var profile = that.options.app.profiles.findWhere({ key: key });
        if (profile != null) {
          profile.trigger('select', profile, { trigger: false });
          profile.fetchChangelog({ since: since, to: to });
        }
      });
    },

    compare: function (key, withKey) {
      var that = this;
      this.anchor = 'comparison';
      this.fetchProfiles().done(function () {
        var profile = that.options.app.profiles.findWhere({ key: key });
        if (profile != null) {
          profile.trigger('select', profile, { trigger: false });
          profile.compareWith(withKey);
        }
      });
    },

    onProfileSelect: function (profile, options) {
      var that = this,
          key = profile.get('key'),
          route = 'show?key=' + encodeURIComponent(key),
          opts = _.defaults(options || {}, { trigger: true });
      if (opts.trigger) {
        this.options.app.router.navigate(route);
      }
      this.options.app.profilesView.highlight(key);
      this.fetchProfile(profile).done(function () {
        var profileHeaderView = new ProfileHeaderView({
          model: profile,
          canWrite: that.options.app.canWrite
        });
        that.options.app.layout.headerRegion.show(profileHeaderView);

        var profileDetailsView = new ProfileDetailsView({
          model: profile,
          canWrite: that.options.app.canWrite,
          exporters: that.options.app.exporters,
          anchor: that.anchor
        });
        that.options.app.layout.detailsRegion.show(profileDetailsView);
      });
    },

    onProfileSetAsDefault: function (profile) {
      var that = this,
          url = baseUrl + '/api/qualityprofiles/set_default',
          key = profile.get('key'),
          options = { profileKey: key };
      return $.post(url, options).done(function () {
        profile.set({ isDefault: true });
        that.fetchProfiles();
      });
    },

    onProfileDestroy: function () {
      this.options.app.router.navigate('');
      this.options.app.layout.headerRegion.reset();
      this.options.app.layout.detailsRegion.reset();
      this.options.app.layout.renderIntro();
      this.options.app.profilesView.highlight(null);
    },

    fetchProfiles: function () {
      return this.options.app.profiles.fetch({ reset: true });
    },

    fetchProfile: function (profile) {
      return profile.fetch();
    }

  });

});
