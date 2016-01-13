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
import FormView from './form-view';

export default FormView.extend({
  method: 'create',

  prepareRequest: function () {
    var that = this;
    var url = baseUrl + '/api/qualitygates/create',
        name = this.$('#quality-gate-form-name').val(),
        options = {
          url: url,
          data: { name: name }
        };
    return this.sendRequest(options)
        .done(function (r) {
          var gate = that.addGate(r);
          gate.trigger('select', gate);
        });
  }
});


