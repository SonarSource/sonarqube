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
  '../templates'
], function () {

  var $ = jQuery;

  return Marionette.ItemView.extend({
    tagName: 'tr',
    template: Templates['coding-rules-custom-rule'],

    modelEvents: {
      'change': 'render'
    },

    events: {
      'click .js-delete-custom-rule': 'deleteRule'
    },

    deleteRule: function () {
      var that = this;
      window.confirmDialog({
        title: t('delete'),
        html: t('are_you_sure'),
        yesHandler: function () {
          var url = baseUrl + '/api/rules/delete',
              options = { key: that.model.id };
          $.post(url, options).done(function () {
            that.model.collection.remove(that.model);
            that.close();
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

});
