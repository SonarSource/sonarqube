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
import ModalFormView from '../../components/common/modal-form';
import uploader from '../../components/common/file-upload';
import Profile from './profile';
import Template from './templates/quality-profiles-restore-profile.hbs';

export default ModalFormView.extend({
  template: Template,

  onFormSubmit: function (e) {
    var that = this;
    ModalFormView.prototype.onFormSubmit.apply(this, arguments);
    uploader({ form: $(e.currentTarget) }).done(function (r) {
      if (_.isArray(r.errors) || _.isArray(r.warnings)) {
        that.showErrors(r.errors, r.warnings);
      } else {
        that.addProfile(r.profile);
        that.destroy();
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
