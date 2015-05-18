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
  'components/common/modal-form',
  'components/common/file-upload',
  './profile',
  './templates'
], function (ModalFormView, uploader, Profile) {

  var $ = jQuery;

  return ModalFormView.extend({
    template: Templates['quality-profiles-restore-profile'],

    onFormSubmit: function (e) {
      var that = this;
      ModalFormView.prototype.onFormSubmit.apply(this, arguments);
      uploader({ form: $(e.currentTarget) }).done(function (r) {
        if (_.isArray(r.errors) || _.isArray(r.warnings)) {
          that.showErrors(r.errors, r.warnings);
        } else {
          that.addProfile(r.profile);
          that.close();
        }
      });
    },

    addProfile: function (profileData) {
      var profile = new Profile(profileData);
      this.collection.add([profile], { merge: true });
      var addedProfile = this.collection.get(profile.id);
      if (addedProfile != null) {
        addedProfile.trigger('select', addedProfile);
      }
    }
  });

});
