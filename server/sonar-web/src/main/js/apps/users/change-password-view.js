/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import ModalForm from '../../components/common/modal-form';
import Template from './templates/users-change-password.hbs';

export default ModalForm.extend({
  template: Template,

  onFormSubmit() {
    ModalForm.prototype.onFormSubmit.apply(this, arguments);
    this.sendRequest();
  },

  sendRequest() {
    const that = this;
    const oldPassword = this.$('#change-user-password-old-password').val();
    const password = this.$('#change-user-password-password').val();
    const confirmation = this.$('#change-user-password-password-confirmation').val();
    if (password !== confirmation) {
      that.showErrors([{ msg: 'New password and its confirmation do not match' }]);
      return;
    }
    this.disableForm();
    this.model
      .changePassword(oldPassword, password, {
        statusCode: {
          // do not show global error
          400: null
        }
      })
      .done(() => {
        that.destroy();
      })
      .fail(jqXHR => {
        that.enableForm();
        that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
      });
  },

  serializeData() {
    return Object.assign({}, ModalForm.prototype.serializeData.apply(this, arguments), {
      isOwnPassword: this.options.currentUser.login === this.model.id
    });
  }
});
