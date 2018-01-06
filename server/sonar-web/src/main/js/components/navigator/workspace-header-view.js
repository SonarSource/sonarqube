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
import Marionette from 'backbone.marionette';

export default Marionette.ItemView.extend({
  collectionEvents() {
    return {
      all: 'shouldRender',
      limitReached: 'flashPagination'
    };
  },

  events() {
    return {
      'click .js-bulk-change': 'onBulkChangeClick',
      'click .js-reload': 'reload',
      'click .js-next': 'selectNext',
      'click .js-prev': 'selectPrev'
    };
  },

  initialize(options) {
    this.listenTo(options.app.state, 'change', this.render);
  },

  onRender() {
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
  },

  onBeforeRender() {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onDestroy() {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onBulkChangeClick(e) {
    e.preventDefault();
    this.bulkChange();
  },

  bulkChange() {},

  shouldRender(event) {
    if (event !== 'limitReached') {
      this.render();
    }
  },

  reload() {
    this.options.app.controller.fetchList();
  },

  selectNext() {
    this.options.app.controller.selectNext();
  },

  selectPrev() {
    this.options.app.controller.selectPrev();
  },

  flashPagination() {
    const flashElement = this.$('.search-navigator-header-pagination');
    flashElement.addClass('in');
    setTimeout(() => {
      flashElement.removeClass('in');
    }, 2000);
  },

  serializeData() {
    return {
      ...Marionette.ItemView.prototype.serializeData.apply(this, arguments),
      state: this.options.app.state.toJSON()
    };
  }
});
