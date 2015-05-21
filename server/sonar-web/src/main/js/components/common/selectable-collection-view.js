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

  return Marionette.CollectionView.extend({

    initialize: function () {
      this.resetSelectedIndex();
      this.listenTo(this.collection, 'reset', this.resetSelectedIndex);
    },

    itemViewOptions: function (model, index) {
      return { index: index };
    },

    resetSelectedIndex: function () {
      this.selectedIndex = 0;
    },

    onRender: function () {
      this.selectCurrent();
    },

    submitCurrent: function () {
      var view = this.children.findByIndex(this.selectedIndex);
      if (view != null) {
        view.submit();
      }
    },

    selectCurrent: function () {
      this.selectItem(this.selectedIndex);
    },

    selectNext: function () {
      if (this.selectedIndex < this.collection.length - 1) {
        this.deselectItem(this.selectedIndex);
        this.selectedIndex++;
        this.selectItem(this.selectedIndex);
      }
    },

    selectPrev: function () {
      if (this.selectedIndex > 0) {
        this.deselectItem(this.selectedIndex);
        this.selectedIndex--;
        this.selectItem(this.selectedIndex);
      }
    },

    selectItem: function (index) {
      if (index >= 0 && index < this.collection.length) {
        var view = this.children.findByIndex(index);
        if (view != null) {
          view.select();
        }
      }
    },

    deselectItem: function (index) {
      var view = this.children.findByIndex(index);
      if (view != null) {
        view.deselect();
      }
    }
  });

});
