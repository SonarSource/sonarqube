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
    template: Templates['workspace-viewer-header'],

    modelEvents: {
      'change': 'render'
    },

    events: {
      'mousedown .js-resize': 'onResizeClick',

      'click .js-minimize': 'onMinimizeClick',
      'click .js-full-screen': 'onFullScreenClick',
      'click .js-normal-size': 'onNormalSizeClick',
      'click .js-close': 'onCloseClick'
    },

    onRender: function () {
      this.$('[data-toggle="tooltip"]').tooltip({ container: 'body' });
      this.$('.js-normal-size').addClass('hidden');
    },

    onClose: function () {
      this.$('[data-toggle="tooltip"]').tooltip('destroy');
      $('.tooltip').remove();
    },

    onResizeClick: function (e) {
      e.preventDefault();
      this.startResizing(e);
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

    startResizing: function (e) {
      this.initialResizePosition = e.clientY;
      this.initialResizeHeight = $('.workspace-viewer-container').height();
      var processResizing = _.bind(this.processResizing, this),
          stopResizing = _.bind(this.stopResizing, this);
      $('body')
          .on('mousemove.workspace', processResizing)
          .on('mouseup.workspace', stopResizing);
    },

    processResizing: function (e) {
      var currentResizePosition = e.clientY,
          resizeDelta = this.initialResizePosition - currentResizePosition,
          height = this.initialResizeHeight + resizeDelta;
      $('.workspace-viewer-container').height(height);
    },

    stopResizing: function () {
      $('body')
          .off('mousemove.workspace')
          .off('mouseup.workspace');
    },

    toFullScreen: function () {
      this.$('.js-normal-size').removeClass('hidden');
      this.$('.js-full-screen').addClass('hidden');
      this.initialResizeHeight = $('.workspace-viewer-container').height();
      $('.workspace-viewer-container').height('9999px');
    },

    toNormalSize: function () {
      this.$('.js-normal-size').addClass('hidden');
      this.$('.js-full-screen').removeClass('hidden');
      $('.workspace-viewer-container').height(this.initialResizeHeight);
    }
  });

});
