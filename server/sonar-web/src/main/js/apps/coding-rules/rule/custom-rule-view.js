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
import Marionette from 'backbone.marionette';
import DeleteRuleView from './delete-rule-view';
import Template from '../templates/rule/coding-rules-custom-rule.hbs';

export default Marionette.ItemView.extend({
  tagName: 'tr',
  template: Template,

  modelEvents: {
    change: 'render'
  },

  events: {
    'click .js-delete-custom-rule': 'deleteRule'
  },

  deleteRule() {
    const deleteRuleView = new DeleteRuleView({
      model: this.model
    }).render();

    deleteRuleView.on('delete', () => {
      this.model.collection.remove(this.model);
      this.destroy();
    });
  },

  serializeData() {
    return {
      ...Marionette.ItemView.prototype.serializeData.apply(this, arguments),
      canDeleteCustomRule: this.options.app.customRules && this.options.app.canWrite,
      templateRule: this.options.templateRule,
      permalink: window.baseUrl + '/coding_rules/#rule_key=' + encodeURIComponent(this.model.id)
    };
  }
});
