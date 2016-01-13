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
import ModalForm from '../../components/common/modal-form';
import Template from './templates/users-form.hbs';

export default ModalForm.extend({
  template: Template,

  events: function () {
    return _.extend(ModalForm.prototype.events.apply(this, arguments), {
      'click #create-user-add-scm-account': 'onAddScmAccountClick'
    });
  },

  onRender: function () {
    ModalForm.prototype.onRender.apply(this, arguments);
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
  },

  onDestroy: function () {
    ModalForm.prototype.onDestroy.apply(this, arguments);
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onFormSubmit: function () {
    ModalForm.prototype.onFormSubmit.apply(this, arguments);
    this.sendRequest();
  },

  onAddScmAccountClick: function (e) {
    e.preventDefault();
    this.addScmAccount();
  },

  getScmAccounts: function () {
    var scmAccounts = this.$('[name="scmAccounts"]').map(function () {
      return $(this).val();
    }).toArray();
    return scmAccounts.filter(function (value) {
      return !!value;
    });
  },

  addScmAccount: function () {
    var fields = this.$('[name="scmAccounts"]');
    fields.first().clone().val('').insertAfter(fields.last());
  }
});


