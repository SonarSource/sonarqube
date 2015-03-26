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
  'templates/overview'
], function () {

  return Marionette.Layout.extend({
    template: Templates['overview-size'],

    modelEvents: {
      'change': 'render'
    },

    onRender: function () {
      if (this.model.has('sizeTrend')) {
        this.$('#overview-size-trend').timeline(this.model.get('sizeTrend'), { type: 'INT' });
      }

      if (this.model.has('treemapMetrics') && this.model.has('treemapMetricsPriority') && this.model.has('treemapComponents')) {
        var widget = new SonarWidgets.Treemap();
        widget
            .metrics(this.model.get('treemapMetrics'))
            .metricsPriority(this.model.get('treemapMetricsPriority'))
            .components(this.model.get('treemapComponents'))
            .options({
              heightInPercents: 55,
              maxItems: 10,
              maxItemsReachedMessage: tp('widget.measure_filter_histogram.max_items_reached', 10),
              baseUrl: baseUrl + '/dashboard/index',
              noData: t('no_data'),
              resource: ''
            })
            .render('#overview-size-treemap');
        autoResize(500, function () {
          widget.update('#overview-size-treemap');
        });
      }
    }
  });

});
