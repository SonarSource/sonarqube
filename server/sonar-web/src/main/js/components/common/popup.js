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

export default Marionette.ItemView.extend({
  className: 'bubble-popup',

  onRender() {
    this.$el.detach().appendTo($('body'));
    const triggerEl = $(this.options.triggerEl);
    if (this.options.bottom) {
      this.$el.addClass('bubble-popup-bottom');
      this.$el.css({
        top: triggerEl.offset().top + triggerEl.outerHeight(),
        left: triggerEl.offset().left
      });
    } else if (this.options.bottomRight) {
      this.$el.addClass('bubble-popup-bottom-right');
      this.$el.css({
        top: triggerEl.offset().top + triggerEl.outerHeight(),
        right: $(window).width() - triggerEl.offset().left - triggerEl.outerWidth()
      });
    } else {
      this.$el.css({
        top: triggerEl.offset().top,
        left: triggerEl.offset().left + triggerEl.outerWidth()
      });
    }
    this.attachCloseEvents();
  },

  attachCloseEvents() {
    const that = this;
    const triggerEl = $(this.options.triggerEl);
    key('escape', () => {
      that.destroy();
    });
    $('body').on('click.bubble-popup', () => {
      $('body').off('click.bubble-popup');
      that.destroy();
    });
    triggerEl.on('click.bubble-popup', e => {
      triggerEl.off('click.bubble-popup');
      e.stopPropagation();
      that.destroy();
    });
  },

  onDestroy() {
    $('body').off('click.bubble-popup');
    const triggerEl = $(this.options.triggerEl);
    triggerEl.off('click.bubble-popup');
  }
});
