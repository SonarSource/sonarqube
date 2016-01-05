/*
 * SonarQube :: Web
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
import ModalForm from '../../components/common/modal-form';
import Template from './templates/account-change-password.hbs';

export default ModalForm.extend({
  template: Template,

  onFormSubmit: function () {
    ModalForm.prototype.onFormSubmit.apply(this, arguments);
    if (this.checkPasswords()) {
      this.sendRequest();
    } else {
      this.showErrors([{ msg: window.t('user.password_doesnt_match_confirmation') }]);
    }
  },

  checkPasswords: function () {
    var p1 = this.$('#password').val(),
        p2 = this.$('#password_confirmation').val();
    return p1 === p2;
  },

  sendRequest: function () {
    var that = this;
    var data = {
      login: window.SS.user,
      password: this.$('#password').val(),
      previousPassword: this.$('#old_password').val()
    };
    var opts = {
      type: 'POST',
      url: baseUrl + '/api/users/change_password',
      data: data,
      statusCode: {
        // do not show global error
        400: null
      }
    };
    this.disableForm();
    $.ajax(opts).done(function () {
      that.destroy();
    }).fail(function (jqXHR) {
      that.enableForm();
      that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
    });
  }
});
