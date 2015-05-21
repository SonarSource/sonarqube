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
  './action-view'
], function (ActionView) {

  var $ = jQuery;

  return Marionette.CollectionView.extend({
    itemView: ActionView,

    scrollToTop: function () {
      var parent = this.$el.scrollParent();
      if (parent.is(document)) {
        parent = $(window);
      }
      parent.scrollTop(0);
    },

    scrollToAction: function (action) {
      var model = this.collection.findWhere({ key: action });
      if (model != null) {
        var view = this.children.findByModel(model);
        if (view != null) {
          this.scrollToView(view);
        }
      }
    },

    scrollToView: function (view) {
      var el = view.$el,
          parent = el.scrollParent();
      var elOffset = el.offset(),
          parentOffset = parent.offset();
      if (parent.is(document)) {
        parentOffset = { top: 0 };
      }
      if (elOffset != null && parentOffset != null) {
        var scrollTop = elOffset.top - parentOffset.top - 70;
        parent.scrollTop(scrollTop);
      }
    }
  });

});
