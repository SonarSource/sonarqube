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
  'templates/workspace'
], function () {

  var $ = jQuery;

  return Marionette.ItemView.extend({
    template: Templates['workspace-viewer-header'],

    modelEvents: {
      'change': 'render'
    },

    events: {
      'click .js-minimize': 'onMinimizeClick',
      'click .js-full-screen': 'onFullScreenClick',
      'click .js-normal-size': 'onNormalSizeClick',
      'click .js-close': 'onCloseClick'
    },

    onRender: function () {
      this.$('[data-toggle="tooltip"]').tooltip({ container: 'body' });
    },

    onClose: function () {
      this.$('[data-toggle="tooltip"]').tooltip('destroy');
      $('.tooltip').remove();
    },

    onMinimizeClick: function (e) {
      e.preventDefault();
      this.trigger('viewerMinimize');
    },

    onFullScreenClick: function (e) {
      e.preventDefault();
      this.toFullScreen();
    },

    onNormalSizeClick: function (e) {
      e.preventDefault();
      this.toNormalSize();
    },

    onCloseClick: function (e) {
      e.preventDefault();
      this.trigger('viewerClose');
    },

    toFullScreen: function () {
      this.$el.closest('.workspace-viewer').addClass('workspace-viewer-full-screen');
    },

    toNormalSize: function () {
      this.$el.closest('.workspace-viewer').removeClass('workspace-viewer-full-screen');
    }
  });

});
