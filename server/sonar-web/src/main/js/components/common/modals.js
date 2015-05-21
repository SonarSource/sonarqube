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
define(function () {

  var $ = jQuery,
      EVENT_SCOPE = 'modal';

  return Marionette.ItemView.extend({
    className: 'modal',
    overlayClassName: 'modal-overlay',
    htmlClassName: 'modal-open',

    events: function () {
      return {
        'click .js-modal-close': 'onCloseClick'
      };
    },

    onRender: function () {
      var that = this;
      this.$el.detach().appendTo($('body'));
      $('html').addClass(this.htmlClassName);
      this.renderOverlay();
      this.keyScope = key.getScope();
      key.setScope('modal');
      key('escape', 'modal', function () {
        that.close();
        return false;
      });
      this.show();
      if (!!this.options.large) {
        this.$el.addClass('modal-large');
      }
    },

    show: function () {
      var that = this;
      setTimeout(function () {
        that.$el.addClass('in');
        $('.' + that.overlayClassName).addClass('in');
      }, 0);
    },

    onClose: function () {
      $('html').removeClass(this.htmlClassName);
      this.removeOverlay();
      key.deleteScope('modal');
      key.setScope(this.keyScope);
    },

    onCloseClick: function (e) {
      e.preventDefault();
      this.close();
    },

    renderOverlay: function () {
      var overlay = $('.' + this.overlayClassName);
      if (overlay.length === 0) {
        $('<div class="' + this.overlayClassName + '"></div>').appendTo($('body'));
      }
    },

    removeOverlay: function () {
      $('.' + this.overlayClassName).remove();
    },

    attachCloseEvents: function () {
      var that = this;
      $('body').on('click.' + EVENT_SCOPE, function () {
        $('body').off('click.' + EVENT_SCOPE);
        that.close();
      });
    }
  });

});
