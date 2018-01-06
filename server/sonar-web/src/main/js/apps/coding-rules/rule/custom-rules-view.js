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
import CustomRuleView from './custom-rule-view';
import CustomRuleCreationView from './custom-rule-creation-view';
import Template from '../templates/rule/coding-rules-custom-rules.hbs';

export default Marionette.CompositeView.extend({
  template: Template,
  childView: CustomRuleView,
  childViewContainer: '#coding-rules-detail-custom-rules',

  childViewOptions() {
    return {
      app: this.options.app,
      templateRule: this.model
    };
  },

  modelEvents: {
    change: 'render'
  },

  events: {
    'click .js-create-custom-rule': 'createCustomRule'
  },

  onRender() {
    this.$el.toggleClass('hidden', !this.model.get('isTemplate'));
  },

  createCustomRule() {
    new CustomRuleCreationView({
      app: this.options.app,
      templateRule: this.model
    }).render();
  },

  serializeData() {
    return {
      ...Marionette.ItemView.prototype.serializeData.apply(this, arguments),
      canCreateCustomRule: this.options.app.customRules && this.options.app.canWrite
    };
  }
});
