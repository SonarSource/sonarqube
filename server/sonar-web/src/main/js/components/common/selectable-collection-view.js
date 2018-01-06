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

export default Marionette.CollectionView.extend({
  initialize() {
    this.resetSelectedIndex();
    this.listenTo(this.collection, 'reset', this.resetSelectedIndex);
  },

  childViewOptions(model, index) {
    return { index };
  },

  resetSelectedIndex() {
    this.selectedIndex = 0;
  },

  onRender() {
    this.selectCurrent();
  },

  submitCurrent() {
    const view = this.children.findByIndex(this.selectedIndex);
    if (view != null) {
      view.submit();
    }
  },

  selectCurrent() {
    this.selectItem(this.selectedIndex);
  },

  selectNext() {
    if (this.selectedIndex < this.collection.length - 1) {
      this.deselectItem(this.selectedIndex);
      this.selectedIndex++;
      this.selectItem(this.selectedIndex);
    }
  },

  selectPrev() {
    if (this.selectedIndex > 0) {
      this.deselectItem(this.selectedIndex);
      this.selectedIndex--;
      this.selectItem(this.selectedIndex);
    }
  },

  selectItem(index) {
    if (index >= 0 && index < this.collection.length) {
      const view = this.children.findByIndex(index);
      if (view != null) {
        view.select();
      }
    }
  },

  deselectItem(index) {
    const view = this.children.findByIndex(index);
    if (view != null) {
      view.deselect();
    }
  }
});
