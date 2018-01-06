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
import { throttle } from 'lodash';
import Marionette from 'backbone.marionette';
import key from 'keymaster';

const BOTTOM_OFFSET = 60;

export default Marionette.CompositeView.extend({
  ui: {
    loadMore: '.js-more',
    lastElementReached: '.js-last-element-reached'
  },

  childViewOptions() {
    return {
      app: this.options.app
    };
  },

  collectionEvents: {
    reset: 'scrollToTop'
  },

  initialize(options) {
    this.loadMoreThrottled = throttle(this.loadMore, 1000, { trailing: false });
    this.listenTo(options.app.state, 'change:maxResultsReached', this.toggleLoadMore);
    this.listenTo(options.app.state, 'change:selectedIndex', this.scrollTo);
    this.bindShortcuts();
  },

  onDestroy() {
    this.unbindScrollEvents();
    this.unbindShortcuts();
  },

  onRender() {
    this.toggleLoadMore();
  },

  toggleLoadMore() {
    const maxResultsReached = this.options.app.state.get('maxResultsReached');
    this.ui.loadMore.toggle(!maxResultsReached);
    this.ui.lastElementReached.toggle(maxResultsReached);
  },

  bindScrollEvents() {
    const that = this;
    $(window).on('scroll.workspace-list-view', () => {
      that.onScroll();
    });
  },

  unbindScrollEvents() {
    $(window).off('scroll.workspace-list-view');
  },

  bindShortcuts() {
    const that = this;
    key('up', 'list', () => {
      that.options.app.controller.selectPrev();
      return false;
    });

    key('down', 'list', () => {
      that.options.app.controller.selectNext();
      return false;
    });
  },

  unbindShortcuts() {
    key.unbind('up', 'list');
    key.unbind('down', 'list');
  },

  loadMore() {
    if (!this.options.app.state.get('maxResultsReached')) {
      const that = this;
      this.unbindScrollEvents();
      this.options.app.controller.fetchNextPage().then(() => {
        that.bindScrollEvents();
      });
    }
  },

  onScroll() {
    if ($(window).scrollTop() + $(window).height() >= this.ui.loadMore.offset().top) {
      this.loadMoreThrottled();
    }
  },

  scrollToTop() {
    this.$el.scrollParent().scrollTop(0);
  },

  scrollTo() {
    const selected = this.collection.at(this.options.app.state.get('selectedIndex'));
    if (selected == null) {
      return;
    }
    const selectedView = this.children.findByModel(selected);
    const parentTopOffset = this.$el.offset().top;
    const viewTop = selectedView.$el.offset().top - parentTopOffset;
    const viewBottom =
      selectedView.$el.offset().top + selectedView.$el.outerHeight() + BOTTOM_OFFSET;
    const windowTop = $(window).scrollTop();
    const windowBottom = windowTop + $(window).height();
    if (viewTop < windowTop) {
      $(window).scrollTop(viewTop);
    }
    if (viewBottom > windowBottom) {
      $(window).scrollTop($(window).scrollTop() - windowBottom + viewBottom);
    }
  }
});
