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
import $ from 'jquery';
import Marionette from 'backbone.marionette';
import Template from '../templates/workspace-viewer-header.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  modelEvents: {
    change: 'render'
  },

  events: {
    'mousedown .js-resize': 'onResizeClick',

    'click .js-minimize': 'onMinimizeClick',
    'click .js-full-screen': 'onFullScreenClick',
    'click .js-normal-size': 'onNormalSizeClick',
    'click .js-close': 'onCloseClick'
  },

  onRender() {
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body' });
    this.$('.js-normal-size').addClass('hidden');
  },

  onDestroy() {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
    $('.tooltip').remove();
  },

  onResizeClick(e) {
    e.preventDefault();
    this.startResizing(e);
  },

  onMinimizeClick(e) {
    e.preventDefault();
    this.trigger('viewerMinimize');
  },

  onFullScreenClick(e) {
    e.preventDefault();
    this.toFullScreen();
  },

  onNormalSizeClick(e) {
    e.preventDefault();
    this.toNormalSize();
  },

  onCloseClick(e) {
    e.preventDefault();
    this.trigger('viewerClose');
  },

  startResizing(e) {
    this.initialResizePosition = e.clientY;
    this.initialResizeHeight = $('.workspace-viewer-container').height();
    const processResizing = this.processResizing.bind(this);
    const stopResizing = this.stopResizing.bind(this);
    $('body')
      .on('mousemove.workspace', processResizing)
      .on('mouseup.workspace', stopResizing);
  },

  processResizing(e) {
    const currentResizePosition = e.clientY;
    const resizeDelta = this.initialResizePosition - currentResizePosition;
    const height = this.initialResizeHeight + resizeDelta;
    $('.workspace-viewer-container').height(height);
  },

  stopResizing() {
    $('body')
      .off('mousemove.workspace')
      .off('mouseup.workspace');
  },

  toFullScreen() {
    this.$('.js-normal-size').removeClass('hidden');
    this.$('.js-full-screen').addClass('hidden');
    this.initialResizeHeight = $('.workspace-viewer-container').height();
    $('.workspace-viewer-container').height('9999px');
  },

  toNormalSize() {
    this.$('.js-normal-size').addClass('hidden');
    this.$('.js-full-screen').removeClass('hidden');
    $('.workspace-viewer-container').height(this.initialResizeHeight);
  }
});
