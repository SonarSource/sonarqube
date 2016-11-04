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
import Marionette from 'backbone.marionette';
import Layout from './layout';
import CustomMeasures from './custom-measures';
import HeaderView from './header-view';
import ListView from './list-view';
import ListFooterView from './list-footer-view';

const App = new Marionette.Application();
const init = function (el, component) {
  // Layout
  this.layout = new Layout({ el });
  this.layout.render();

  // Collection
  this.customMeasures = new CustomMeasures({
    projectId: component.id
  });

  // Header View
  this.headerView = new HeaderView({
    collection: this.customMeasures,
    projectId: component.id
  });
  this.layout.headerRegion.show(this.headerView);

  // List View
  this.listView = new ListView({
    collection: this.customMeasures
  });
  this.layout.listRegion.show(this.listView);

  // List Footer View
  this.listFooterView = new ListFooterView({
    collection: this.customMeasures
  });
  this.layout.listFooterRegion.show(this.listFooterView);

  // Go!
  this.customMeasures.fetch();
};

App.on('start', function (options) {
  init.call(App, options.el, options.component);
});

export default function (el, component) {
  App.start({ el, component });
}

