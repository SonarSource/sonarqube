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
import _ from 'underscore';
import Backbone from 'backbone';
import ModalForm from '../../components/common/modal-form';
import Gate from './gate';
import Template from './templates/quality-gate-form.hbs';

export default ModalForm.extend({
  template: Template,

  onFormSubmit: function () {
    ModalForm.prototype.onFormSubmit.apply(this, arguments);
    this.disableForm();
    this.prepareRequest();
  },

  sendRequest: function (options) {
    var that = this,
        opts = _.defaults(options || {}, {
          type: 'POST',
          statusCode: {
            // do not show global error
            400: null
          }
        });
    return Backbone.ajax(opts)
        .done(function () {
          that.destroy();
        }).fail(function (jqXHR) {
          that.enableForm();
          that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
        });
  },

  addGate: function (attrs) {
    var gate = new Gate(attrs);
    this.collection.add(gate, { merge: true });
    return gate;
  },

  serializeData: function () {
    return _.extend(ModalForm.prototype.serializeData.apply(this, arguments), {
      method: this.method
    });
  }
});


