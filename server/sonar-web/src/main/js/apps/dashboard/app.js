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
import _ from 'underscore';

window.Portal = function (options) {
  this.initialize(options);
};

window.Portal.prototype = {

  initialize (options) {
    this.options = options;
    if (!this.options.editorEnabled) {
      return;
    }
    this.createAllSortables();
    this.lastSaveString = '';
    this.saveDashboardsState();
  },

  createAllSortables () {
    const that = this;
    const blocks = $('.' + this.options.block);
    const columnHandle = $('.' + this.options.columnHandle);
    let draggable;
    const onDragLeave = function (e) {
      $(e.currentTarget).removeClass(that.options.hoverClass);
    };
    const onDrop = function (e) {
      e.preventDefault();
      draggable.detach().insertBefore($(e.currentTarget));
      onDragLeave(e);
      that.saveDashboardsState();
    };

    blocks
        .prop('draggable', true)
        .on('selectstart', function () {
          this.dragDrop();
          return false;
        })
        .on('dragstart', function (e) {
          e.originalEvent.dataTransfer.setData('Text', 'drag');
          draggable = $(this);
          columnHandle.show();
        })
        .on('dragover', function (e) {
          if (draggable.prop('id') !== $(this).prop('id')) {
            e.preventDefault();
            $(e.currentTarget).addClass(that.options.hoverClass);
          }
        })
        .on('drop', onDrop)
        .on('dragleave', onDragLeave);

    columnHandle
        .on('dragover', function (e) {
          e.preventDefault();
          $(e.currentTarget).addClass(that.options.hoverClass);
        })
        .on('drop', onDrop)
        .on('dragleave', onDragLeave);
  },

  highlightWidget (widgetId) {
    const block = $('#block_' + widgetId);
    const options = this.options;
    block.css('background-color', options.highlightStartColor);
    setTimeout(function () {
      block.css('background-color', options.highlightEndColor);
    }, this.options.highlightDuration);
  },

  saveDashboardsState () {
    const options = this.options;
    const result = $('.' + this.options.column).map(function () {
      const blocks = $(this).find('.' + options.block);
      $(this).find('.' + options.columnHandle).toggle(blocks.length === 0);

      return blocks.map(function () {
        return $(this).prop('id').substring(options.block.length + 1);
      }).get().join(',');
    }).get().join(';');

    if (result === this.lastSaveString) {
      return;
    }

    const firstTime = this.lastSaveString === '';
    this.lastSaveString = result;

    if (firstTime) {
      return;
    }

    if (this.options.saveUrl) {
      const postBody = this.options.dashboardState + '=' + encodeURIComponent(result);

      $.ajax({
        url: this.options.saveUrl,
        type: 'POST',
        data: postBody
      });
    }
  },

  editWidget (widgetId) {
    $('#widget_title_' + widgetId).hide();
    $('#widget_' + widgetId).hide();
    $('#widget_props_' + widgetId).show();
    $($(`#block_${widgetId} a.link-action`)[0]).hide();
  },

  cancelEditWidget (widgetId) {
    $('widget_title_' + widgetId).show();
    $('#widget_' + widgetId).show();
    $('#widget_props_' + widgetId).hide();
    $($(`#block_${widgetId} a.link-action`)[0]).show();
  },

  deleteWidget (element) {
    $(element).closest('.' + this.options.block).remove();
    this.saveDashboardsState();
  }
};

window.autoResize = function (everyMs, callback) {
  const debounce = _.debounce(callback, everyMs);
  $(window).on('resize', debounce);
};
