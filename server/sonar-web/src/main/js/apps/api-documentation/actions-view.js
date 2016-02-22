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
import ActionView from './action-view';

export default Marionette.CollectionView.extend({
  childView: ActionView,

  childViewOptions () {
    return {
      state: this.options.state
    };
  },

  scrollToTop () {
    let parent = this.$el.scrollParent();
    if (parent.is(document)) {
      parent = $(window);
    }
    parent.scrollTop(0);
  },

  scrollToAction (action) {
    const model = this.collection.findWhere({ key: action });
    if (model != null) {
      const view = this.children.findByModel(model);
      if (view != null) {
        this.scrollToView(view);
      }
    }
  },

  scrollToView (view) {
    const elOffset = view.el.getBoundingClientRect();
    if (elOffset != null) {
      const scrollTop = elOffset.top - 70;
      window.scrollTo(0, scrollTop);
    }
  }
});
