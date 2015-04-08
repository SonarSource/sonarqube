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

  function trans (left, top) {
    return 'translate(' + left + ', ' + top + ')';
  }

  var $ = jQuery;


  return Backbone.View.extend({

    initialize: function (options) {
      this.data = options.data;
      this.type = options.type || 'INT';
      this.width = 0;
      this.height = 0;
    },

    attachEvents: function () {
      this.detachEvents();
      var event = 'resize.trend-' + this.cid,
          update = _.throttle(_.bind(this.update, this), 50);
      $(window).on(event, update);
    },

    detachEvents: function () {
      var event = 'resize.trend-' + this.cid;
      $(window).off(event);
      return this;
    },

    render: function () {
      var that = this,
          data = this.data;
      this.container = d3.select(this.el);
      this.svg = this.container.append('svg')
          .classed('sonar-d3', true);
      this.plot = this.svg.append('g')
          .classed('plot', true);

      this.xScale = d3.time.scale()
          .domain(d3.extent(data, function (d) {
            return moment(d.val).toDate();
          }))
          .nice();
      this.yScale = d3.scale.linear()
          .domain(d3.extent(data, function (d) {
            return d.count;
          }))
          .nice();

      this.line = d3.svg.line()
          .x(function (d) {
            return that.xScale(moment(d.val).toDate());
          })
          .y(function (d) {
            return that.yScale(d.count);
          })
          .interpolate('linear');

      this.xScaleTicks = this.xScale.ticks(5);
      this.yScaleTicks = this.yScale.ticks(3);

      this.xTicks = this.xScaleTicks.map(function (tick) {
        return that.plot.append('text')
            .datum(tick)
            .text(that.xScale.tickFormat()(tick))
            .attr('dy', '0')
            .style('text-anchor', 'middle')
            .style('font-size', '10px')
            .style('font-weight', '300')
            .style('fill', '#aaa');
      });
      this.yTicks = this.yScaleTicks.map(function (tick) {
        return that.plot.append('text')
            .datum(tick)
            .text(window.formatMeasure(tick, that.type))
            .attr('dy', '5px')
            .style('text-anchor', 'end')
            .style('font-size', '10px')
            .style('font-weight', '300')
            .style('fill', '#aaa');
      });

      this.xTickLines = this.xScaleTicks.map(function (tick) {
        return that.plot.append('line')
            .datum(tick)
            .style('stroke', '#eee')
            .style('shape-rendering', 'crispedges');
      });
      this.yTickLines = this.yScaleTicks.map(function (tick) {
        return that.plot.append('line')
            .datum(tick)
            .style('stroke', '#eee')
            .style('shape-rendering', 'crispedges');
      });

      this.path = this.plot.append('path')
          .datum(data)
          .classed('line', true)
          .style('stroke', 'rgb(31, 119, 180)');

      this.attachEvents();

      return this;
    },

    update: function () {
      var that = this,
          width = this.$el.closest('.overview-trend').width(),
          height = 150,
          marginLeft = 20,
          marginRight = 50,
          marginTop = 5,
          marginBottom = 25,
          availableWidth = width - marginLeft - marginRight,
          availableHeight = height - marginTop - marginBottom;

      this.svg
          .attr('width', width)
          .attr('height', height);

      this.plot.attr('transform', trans(marginLeft, marginTop));
      this.xScale.range([0, availableWidth]);
      this.yScale.range([availableHeight, 0]);

      this.path
          .attr('d', this.line);

      this.xTicks.forEach(function (tick) {
        tick
            .attr('x', that.xScale(tick.datum()))
            .attr('y', availableHeight + 20);
      });

      this.yTicks.forEach(function (tick) {
        tick
            .attr('x', availableWidth + 50)
            .attr('y', that.yScale(tick.datum()));
      });

      this.xTickLines.forEach(function (tick) {
        tick
            .attr('x1', that.xScale(tick.datum()))
            .attr('x2', that.xScale(tick.datum()))
            .attr('y1', 0)
            .attr('y2', availableHeight);
      });

      this.yTickLines.forEach(function (tick) {
        tick
            .attr('x1', 0)
            .attr('x2', availableWidth)
            .attr('y1', that.yScale(tick.datum()))
            .attr('y2', that.yScale(tick.datum()));
      });

      return this;
    }

  });

});
