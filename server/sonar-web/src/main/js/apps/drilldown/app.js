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
  'components/source-viewer/main'
], function (SourceViewer) {

  var $ = jQuery,
      App = new Marionette.Application();

  App.addRegions({
    viewerRegion: '#source-viewer'
  });

  App.addInitializer(function () {
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
  });

  var l10nXHR = window.requestMessages();
  l10nXHR.done(function () {
    App.start();
  });
});
