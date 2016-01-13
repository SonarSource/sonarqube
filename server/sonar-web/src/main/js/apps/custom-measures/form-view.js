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
import ModalForm from '../../components/common/modal-form';
import Metrics from '../metrics/metrics';
import Template from './templates/custom-measures-form.hbs';

export default ModalForm.extend({
  template: Template,

  initialize: function () {
    this.metrics = new Metrics();
    this.listenTo(this.metrics, 'reset', this.render);
    this.metrics.fetch({ reset: true });
  },

  onRender: function () {
    ModalForm.prototype.onRender.apply(this, arguments);
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
    this.$('#create-custom-measure-metric').select2({
      width: '250px',
      minimumResultsForSearch: 20
    });
  },

  onDestroy: function () {
    ModalForm.prototype.onDestroy.apply(this, arguments);
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onFormSubmit: function () {
    ModalForm.prototype.onFormSubmit.apply(this, arguments);
    this.sendRequest();
  },

  getAvailableMetrics: function () {
    var takenMetrics = this.collection.getTakenMetrics();
    return this.metrics.toJSON().filter(function (metric) {
      return takenMetrics.indexOf(metric.id) === -1;
    });
  },

  serializeData: function () {
    var metrics = this.getAvailableMetrics(),
        isNew = !this.model;
    return _.extend(ModalForm.prototype.serializeData.apply(this, arguments), {
      metrics: metrics,
      canCreateMetric: !isNew || (isNew && metrics.length > 0)
    });
  }
});


