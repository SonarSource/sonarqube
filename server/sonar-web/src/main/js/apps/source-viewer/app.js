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
import SourceViewer from '../../components/source-viewer/main';

const App = new Marionette.Application();
const init = function () {
  const options = window.sonarqube;

  this.addRegions({ mainRegion: options.el });

  const viewer = new SourceViewer();
  this.mainRegion.show(viewer);
  viewer.open(options.file.uuid);
  if (typeof options.file.line === 'number') {
    viewer.on('loaded', function () {
      viewer
          .highlightLine(options.file.line)
          .scrollToLine(options.file.line);
    });
  }
};

App.on('start', function () {
  init.call(App);
});

window.sonarqube.appStarted.then(options => App.start(options));


