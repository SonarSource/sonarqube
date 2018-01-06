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
import key from 'keymaster';

const EVENT_SCOPE = 'modal';

export default Marionette.ItemView.extend({
  className: 'modal',
  overlayClassName: 'modal-overlay',
  htmlClassName: 'modal-open',

  events() {
    return {
      'click .js-modal-close': 'onCloseClick'
    };
  },

  onRender() {
    const that = this;
    this.$el.detach().appendTo($('body'));
    $('html').addClass(this.htmlClassName);
    this.renderOverlay();
    this.keyScope = key.getScope();
    key.setScope('modal');
    key('escape', 'modal', () => {
      that.destroy();
      return false;
    });
    this.show();
    if (this.options.large) {
      this.$el.addClass('modal-large');
    }
  },

  show() {
    const that = this;
    setTimeout(() => {
      that.$el.addClass('in');
      $('.' + that.overlayClassName).addClass('in');
    }, 0);
  },

  onDestroy() {
    $('html').removeClass(this.htmlClassName);
    this.removeOverlay();
    key.deleteScope('modal');
    key.setScope(this.keyScope);
  },

  onCloseClick(e) {
    e.preventDefault();
    this.destroy();
  },

  renderOverlay() {
    const overlay = $('.' + this.overlayClassName);
    if (overlay.length === 0) {
      $(`<div class="${this.overlayClassName}"></div>`).appendTo($('body'));
    }
  },

  removeOverlay() {
    $('.' + this.overlayClassName).remove();
  },

  attachCloseEvents() {
    const that = this;
    $('body').on('click.' + EVENT_SCOPE, () => {
      $('body').off('click.' + EVENT_SCOPE);
      that.destroy();
    });
  }
});
