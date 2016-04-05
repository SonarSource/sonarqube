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
import ModalFormView from '../../components/common/modal-form';
import Template from './templates/quality-profiles-rename-profile.hbs';

export default ModalFormView.extend({
  template: Template,

  onFormSubmit () {
    ModalFormView.prototype.onFormSubmit.apply(this, arguments);
    this.sendRequest();
  },

  sendRequest () {
    const that = this;
    const url = window.baseUrl + '/api/qualityprofiles/rename';
    const name = this.$('#rename-profile-name').val();
    const options = {
      key: this.model.get('key'),
      name
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
      that.model.set({ name });
      that.destroy();
    }).fail(function (jqXHR) {
      that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
    });
  }
});

