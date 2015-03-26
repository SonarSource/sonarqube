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
  'templates/overview'
], function () {

  return Marionette.Layout.extend({
    issuesTemplate: Templates['overview-issues'],
    debtTemplate: Templates['overview-debt'],

    modelEvents: {
      'change': 'render'
    },

    events: {
      'click .js-issues': 'onIssuesClick',
      'click .js-debt': 'onDebtClick',
      'click .js-guide': 'onGuideClick'
    },

    initialize: function () {
      this.section = 'issues';
    },

    onRender: function () {
      if (this.model.has('issuesTrend')) {
        this.$('#overview-issues-trend').timeline(this.model.get('issuesTrend'), { type: 'INT' });
      }
      if (this.model.has('debtTrend')) {
        this.$('#overview-debt-trend').timeline(this.model.get('debtTrend'), { type: 'WORK_DUR' });
      }
      this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
    },

    onClose: function () {
      this.$('[data-toggle="tooltip"]').tooltip('destroy');
    },

    onIssuesClick: function (e) {
      e.preventDefault();
      this.showIssues();
    },

    onDebtClick: function (e) {
      e.preventDefault();
      this.showDebt();
    },

    onGuideClick: function (e) {
      e.preventDefault();
      this.showGuide();
    },

    getTemplate: function () {
      var option = this.section + 'Template';
      return Marionette.getOption(this, option);
    },

    showIssues: function () {
      this.section = 'issues';
      this.render();
    },

    showDebt: function () {
      this.section = 'debt';
      this.render();
    },

    showGuide: function () {

    }
  });

});
