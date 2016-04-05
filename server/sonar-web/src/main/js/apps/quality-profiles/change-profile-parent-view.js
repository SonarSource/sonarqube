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
import ModalFormView from '../../components/common/modal-form';
import Template from './templates/quality-profiles-change-profile-parent.hbs';

export default ModalFormView.extend({
  template: Template,

  onRender () {
    ModalFormView.prototype.onRender.apply(this, arguments);
    this.$('select').select2({
      width: '250px',
      minimumResultsForSearch: 50
    });
  },

  onFormSubmit () {
    ModalFormView.prototype.onFormSubmit.apply(this, arguments);
    this.disableForm();
    this.sendRequest();
  },

  sendRequest () {
    const that = this;
    const url = window.baseUrl + '/api/qualityprofiles/change_parent';
    const parent = this.$('#change-profile-parent').val();
    const options = {
      profileKey: this.model.get('key'),
      parentKey: parent
    };
    return $.ajax({
      url,
      type: 'POST',
      data: options,
      statusCode: {
        // do not show global error
        400: null
      }
    }).done(function () {
      that.model.collection.fetch();
      that.model.trigger('select', that.model);
      that.destroy();
    }).fail(function (jqXHR) {
      that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
      that.enableForm();
    });
  },

  serializeData () {
    const that = this;
    const profilesData = this.model.collection.toJSON();
    const profiles = _.filter(profilesData, function (profile) {
      return profile.language === that.model.get('language') && profile.key !== that.model.id;
    });
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      profiles
    });
  }
});

