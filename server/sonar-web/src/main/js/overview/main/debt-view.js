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
  'overview/trend-view',
  'templates/overview'
], function (TrendView) {

  return Marionette.Layout.extend({
    template: Templates['overview-debt'],

    modelEvents: {
      'change': 'render'
    },

    onRender: function () {
      var trend = this.model.get('debtTrend'),
          hasDebt = this.model.get('debt') != null;
      if (_.size(trend) > 1 && hasDebt) {
        this.trendView = new TrendView({ data: trend, type: 'WORK_DUR' });
        this.trendView.render()
            .$el.appendTo(this.$('#overview-debt-trend'));
        this.trendView.update();
      }
    },

    onClose: function () {
      if (this.trendView != null) {
        this.trendView.detachEvents().remove();
      }
    }
  });

});
