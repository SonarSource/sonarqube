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


define(['design/view'], function (DesignView) {

  var $ = jQuery,
      RESOURCES_URL = baseUrl + '/api/resources',
      App = new Marionette.Application();

  App.noDataAvailable = function () {
    var message = t('design.noData');
    $('#project-design').html('<p class="message-alert"><i class="icon-alert-warn"></i> ' + message + '</p>');
  };

  App.addInitializer(function () {
    var packageTangles = {},
        packageTanglesXHR = $.get(RESOURCES_URL, {
          resource: window.resourceKey,
          depth: 1,
          metrics: 'package_tangles'
        }).done(function (data) {
          return data.forEach(function (component) {
            packageTangles[component.id] = component.msr[0].frmt_val;
          });
        }),
        dsmXHR = $.get(RESOURCES_URL, {
          resource: window.resourceKey,
          metrics: 'dsm'
        }).fail(function () {
          App.noDataAvailable();
        });

    $.when(packageTanglesXHR, dsmXHR).done(function () {
      var rawData = dsmXHR.responseJSON;
      if (!(_.isArray(rawData) && rawData.length === 1 && _.isArray(rawData[0].msr))) {
        App.noDataAvailable();
        return;
      }

      var data = JSON.parse(rawData[0].msr[0].data);
      data.forEach(function (row, rowIndex) {
        return row.v.forEach(function (cell, columnIndex) {
          if ((cell.w != null) && cell.w > 0) {
            cell.status = rowIndex < columnIndex ? 'cycle' : 'dependency';
          }
        });
      });
      data = data.map(function (row) {
        return _.extend(row, {
          empty: row.q === 'DIR' && row.v.every(function (item) {
            return item.w == null;
          })
        });
      });

      var collection = new Backbone.Collection(data);
      collection.forEach(function (model) {
        return model.set('pt', packageTangles[model.get('i')]);
      });
      this.view = new DesignView({
        app: this,
        collection: collection
      });
      $('#project-design').empty().append(this.view.render().el);
    });
  });

  window.requestMessages().done(function () {
    App.start();
  });

});
