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
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import DeleteConditionView from './gate-conditions-delete-view';
import Template from './templates/quality-gate-detail-condition.hbs';
import { translate } from '../../helpers/l10n';

export default Marionette.ItemView.extend({
  tagName: 'tr',
  template: Template,

  modelEvents: {
    'change': 'render'
  },

  ui: {
    periodSelect: '[name=period]',
    operatorSelect: '[name=operator]',
    warningInput: '[name=warning]',
    errorInput: '[name=error]',
    actionsBox: '.quality-gate-condition-actions',
    updateButton: '.update-condition',
    deleteButton: '.delete-condition'
  },

  events: {
    'click @ui.updateButton': 'saveCondition',
    'click @ui.deleteButton': 'deleteCondition',
    'click .add-condition': 'saveCondition',
    'click .cancel-add-condition': 'cancelAddCondition',
    'keyup :input': 'enableUpdate',
    'change :input': 'enableUpdate'
  },

  onRender: function () {
    this.ui.warningInput.val(this.model.get('warning'));
    this.ui.errorInput.val(this.model.get('error'));

    this.ui.periodSelect.select2({
      allowClear: false,
      minimumResultsForSearch: 999
    });

    this.ui.operatorSelect.select2({
      allowClear: false,
      minimumResultsForSearch: 999
    });

    if (this.model.isNew()) {
      this.ui.periodSelect.select2('open');
    }
  },

  saveCondition: function () {
    var attrs = {
      gateId: this.model.isNew() ? this.options.gate.id : void 0,
      period: this.ui.periodSelect.val(),
      op: this.ui.operatorSelect.val(),
      warning: this.ui.warningInput.val(),
      error: this.ui.errorInput.val()
    };
    this.model.save(attrs, { wait: true });
  },

  deleteCondition: function () {
    new DeleteConditionView({
      model: this.model,
      metric: this.getMetric()
    }).render();
  },

  cancelAddCondition: function () {
    this.destroy();
  },

  enableUpdate: function () {
    this.ui.updateButton.prop('disabled', false);
  },

  getMetric: function () {
    var key = this.model.get('metric');
    return _.findWhere(this.options.metrics, { key: key });
  },

  isDiffMetric: function () {
    var key = this.model.get('metric');
    return key.indexOf('new_') === 0;
  },

  serializeData: function () {
    var period = _.findWhere(this.options.periods, { key: this.model.get('period') });
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      canEdit: this.options.canEdit,
      periods: this.options.periods,
      periodText: period ? period.text : translate('value'),
      metric: this.getMetric(),
      isDiffMetric: this.isDiffMetric()
    });
  }
});
