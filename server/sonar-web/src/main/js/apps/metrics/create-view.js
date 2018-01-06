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
import Metric from './metric';
import FormView from './form-view';

export default FormView.extend({
  sendRequest() {
    const that = this;
    const metric = new Metric({
      key: this.$('#create-metric-key').val(),
      name: this.$('#create-metric-name').val(),
      description: this.$('#create-metric-description').val(),
      domain: this.$('#create-metric-domain').val(),
      type: this.$('#create-metric-type').val()
    });
    this.disableForm();
    return metric
      .save(null, {
        statusCode: {
          // do not show global error
          400: null
        }
      })
      .done(() => {
        that.collection.refresh();
        if (that.options.domains.indexOf(metric.get('domain')) === -1) {
          that.options.domains.push(metric.get('domain'));
        }
        that.destroy();
      })
      .fail(jqXHR => {
        that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
        that.enableForm();
      });
  }
});
