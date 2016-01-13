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
import SourceViewer from '../../components/source-viewer/main';

var App = new Marionette.Application(),
    init = function () {
      let options = window.sonarqube;
      App.addRegions({ viewerRegion: options.el });
      $('.js-drilldown-link').on('click', function (e) {
        e.preventDefault();
        $(e.currentTarget).closest('table').find('.selected').removeClass('selected');
        $(e.currentTarget).closest('tr').addClass('selected');
        var uuid = $(e.currentTarget).data('uuid'),
            viewer = new SourceViewer();
        App.viewerRegion.show(viewer);
        viewer.open(uuid);
        if (window.drilldown.period != null) {
          viewer.on('loaded', function () {
            viewer.filterLinesByDate(window.drilldown.period, window.drilldown.periodName);
          });
        }
      });
    };

App.on('start', function (options) {
  init.call(App, options);
});

window.sonarqube.appStarted.then(options => App.start(options));
