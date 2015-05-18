/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
define([
  './custom-rule-view',
  './custom-rule-creation-view',
  '../templates'
], function (CustomRuleView, CustomRuleCreationView) {

  return Marionette.CompositeView.extend({
    template: Templates['coding-rules-custom-rules'],
    itemView: CustomRuleView,
    itemViewContainer: '#coding-rules-detail-custom-rules',

    itemViewOptions: function () {
      return {
        app: this.options.app,
        templateRule: this.model
      };
    },

    modelEvents: {
      'change': 'render'
    },

    events: {
      'click .js-create-custom-rule': 'createCustomRule'
    },

    onRender: function () {
      this.$el.toggleClass('hidden', !this.model.get('isTemplate'));
    },

    createCustomRule: function () {
      new CustomRuleCreationView({
        app: this.options.app,
        templateRule: this.model
      }).render();
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        canWrite: this.options.app.canWrite
      });
    }
  });

});
