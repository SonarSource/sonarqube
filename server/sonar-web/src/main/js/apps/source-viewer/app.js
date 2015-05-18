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
requirejs.config({
  baseUrl: baseUrl + '/js'
});

requirejs([
  'components/source-viewer/main'

], function (SourceViewer) {

  var App = new Marionette.Application();

  App.addRegions({
    viewerRegion: '#source-viewer'
  });

  App.addInitializer(function () {
    var viewer = new SourceViewer();
    App.viewerRegion.show(viewer);
    viewer.open(window.file.uuid);
    if (typeof window.file.line === 'number') {
      viewer.on('loaded', function () {
        viewer
            .highlightLine(window.file.line)
            .scrollToLine(window.file.line);
      });
    }
  });

  var l10nXHR = window.requestMessages();

  l10nXHR.done(function () {
    App.start();
  });

});
