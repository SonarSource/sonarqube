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
  './profile',
  './templates'
], function (ModalFormView, Profile) {

  var $ = jQuery;

  return ModalFormView.extend({
    template: Templates['quality-profiles-copy-profile'],

    onFormSubmit: function () {
      ModalFormView.prototype.onFormSubmit.apply(this, arguments);
      this.disableForm();
      this.sendRequest();
    },

    sendRequest: function () {
      var that = this,
          url = baseUrl + '/api/qualityprofiles/copy',
          name = this.$('#copy-profile-name').val(),
          options = {
            fromKey: this.model.get('key'),
            toName: name
          };
      return $.ajax({
        type: 'POST',
        url: url,
        data: options,
        statusCode: {
          // do not show global error
          400: null
        }
      }).done(function (r) {
        that.addProfile(r);
        that.close();
      }).fail(function (jqXHR) {
        that.enableForm();
        that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
      });
    },

    addProfile: function (profileData) {
      var profile = new Profile(profileData);
      this.model.collection.add([profile]);
      profile.trigger('select', profile);
    }
  });

});
