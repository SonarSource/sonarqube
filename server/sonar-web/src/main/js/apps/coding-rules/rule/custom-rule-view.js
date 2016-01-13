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
import Marionette from 'backbone.marionette';
import Template from '../templates/rule/coding-rules-custom-rule.hbs';
import confirmDialog from '../confirm-dialog';
import { translate } from '../../../helpers/l10n';

export default Marionette.ItemView.extend({
  tagName: 'tr',
  template: Template,

  modelEvents: {
    'change': 'render'
  },

  events: {
    'click .js-delete-custom-rule': 'deleteRule'
  },

  deleteRule: function () {
    var that = this;
    confirmDialog({
      title: translate('delete'),
      html: translate('are_you_sure'),
      yesHandler: function () {
        var url = baseUrl + '/api/rules/delete',
            options = { key: that.model.id };
        $.post(url, options).done(function () {
          that.model.collection.remove(that.model);
          that.destroy();
        });
      }
    });
  },

  serializeData: function () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      canWrite: this.options.app.canWrite,
      templateRule: this.options.templateRule,
      permalink: baseUrl + '/coding_rules/#rule_key=' + encodeURIComponent(this.model.id)
    });
  }
});
