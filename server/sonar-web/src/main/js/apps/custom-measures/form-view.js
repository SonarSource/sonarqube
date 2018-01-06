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
import Metrics from '../metrics/metrics';
import Template from './templates/custom-measures-form.hbs';

export default ModalForm.extend({
  template: Template,

  initialize() {
    this.metrics = new Metrics();
    this.listenTo(this.metrics, 'reset', this.render);
    this.metrics.fetch({ reset: true });
  },

  onRender() {
    ModalForm.prototype.onRender.apply(this, arguments);
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
    this.$('#create-custom-measure-metric').select2({
      width: '250px',
      minimumResultsForSearch: 20
    });
  },

  onDestroy() {
    ModalForm.prototype.onDestroy.apply(this, arguments);
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onFormSubmit() {
    ModalForm.prototype.onFormSubmit.apply(this, arguments);
    this.sendRequest();
  },

  getAvailableMetrics() {
    const takenMetrics = this.collection.getTakenMetrics();
    return this.metrics.toJSON().filter(metric => takenMetrics.indexOf(metric.id) === -1);
  },

  serializeData() {
    const metrics = this.getAvailableMetrics();
    const isNew = !this.model;
    return {
      ...ModalForm.prototype.serializeData.apply(this, arguments),
      metrics,
      canCreateMetric: !isNew || metrics.length > 0
    };
  }
});
