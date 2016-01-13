/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

export default Marionette.ItemView.extend({
  className: 'bubble-popup',

  onRender: function () {
    this.$el.detach().appendTo($('body'));
    if (this.options.bottom) {
      this.$el.addClass('bubble-popup-bottom');
      this.$el.css({
        top: this.options.triggerEl.offset().top + this.options.triggerEl.outerHeight(),
        left: this.options.triggerEl.offset().left
      });
    } else if (this.options.bottomRight) {
      this.$el.addClass('bubble-popup-bottom-right');
      this.$el.css({
        top: this.options.triggerEl.offset().top + this.options.triggerEl.outerHeight(),
        right: $(window).width() - this.options.triggerEl.offset().left - this.options.triggerEl.outerWidth()
      });
    } else {
      this.$el.css({
        top: this.options.triggerEl.offset().top,
        left: this.options.triggerEl.offset().left + this.options.triggerEl.outerWidth()
      });
    }
    this.attachCloseEvents();
  },

  attachCloseEvents: function () {
    var that = this;
    key('escape', function () {
      that.destroy();
    });
    $('body').on('click.bubble-popup', function () {
      $('body').off('click.bubble-popup');
      that.destroy();
    });
    this.options.triggerEl.on('click.bubble-popup', function (e) {
      that.options.triggerEl.off('click.bubble-popup');
      e.stopPropagation();
      that.destroy();
    });
  },

  onDestroy: function () {
    $('body').off('click.bubble-popup');
    this.options.triggerEl.off('click.bubble-popup');
  }
});


