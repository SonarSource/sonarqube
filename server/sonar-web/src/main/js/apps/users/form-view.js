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
import $ from 'jquery';
import ModalForm from '../../components/common/modal-form';
import Template from './templates/users-form.hbs';

export default ModalForm.extend({
  template: Template,

  events() {
    return {
      ...ModalForm.prototype.events.apply(this, arguments),
      'click #create-user-add-scm-account': 'onAddScmAccountClick',
      'click .js-remove-scm': 'onRemoveScmAccountClick'
    };
  },

  onRender() {
    ModalForm.prototype.onRender.apply(this, arguments);
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
  },

  onDestroy() {
    ModalForm.prototype.onDestroy.apply(this, arguments);
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onFormSubmit() {
    ModalForm.prototype.onFormSubmit.apply(this, arguments);
    this.sendRequest();
  },

  onAddScmAccountClick(e) {
    e.preventDefault();
    this.addScmAccount();
  },

  getScmAccounts() {
    const scmAccounts = this.$('[name="scmAccounts"]')
      .map(function() {
        return $(this).val();
      })
      .toArray()
      .filter(value => !!value);
    // return empty string to reset the field when updating
    return scmAccounts.length ? scmAccounts : '';
  },

  addScmAccount() {
    const fields = this.$('.js-scm-input');
    const newField = fields
      .first()
      .clone()
      .removeClass('hidden');
    newField.insertAfter(fields.last());
    newField
      .find('input')
      .val('')
      .focus();
  },

  onRemoveScmAccountClick(e) {
    $(e.currentTarget)
      .parent()
      .remove();
  }
});
