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
(function ($) {

  function trans (left, top) {
    return 'translate(' + left + ', ' + top + ')';
  }

  var defaults = {
    height: 140,
    color: '#1f77b4',
    interpolate: 'basis'
  };

  /*
   * data = [
   *   { val: '2015-01-30', count: 30 },
   *   ...
   * ]
   */

  $.fn.timeline = function (data) {
    $(this).each(function () {
      var options = _.defaults($(this).data(), defaults);
      _.extend(options, { width: $(this).width() });

      var container = d3.select(this),
          svg = container.append('svg')
              .attr('width', options.width + 2)
              .attr('height', options.height + 2)
              .classed('sonar-d3', true),

          plot = svg.append('g')
              .classed('plot', true),

          xScale = d3.time.scale()
              .domain(d3.extent(data, function (d) {
                return new Date(d.val);
              })),

          yScale = d3.scale.linear()
              .domain([
                0, d3.max(data, function (d) {
                  return d.count;
                })
              ]),

          line = d3.svg.line()
              .x(function (d) {
                return xScale(new Date(d.val));
              })
              .y(function (d) {
                return yScale(d.count);
              })
              .interpolate(options.interpolate),

          minDate = xScale.domain()[0],
          minDateTick = svg.append('text')
              .classed('subtitle', true)
              .text(moment(minDate).format('LL')),

          maxDate = xScale.domain()[1],
          maxDateTick = svg.append('text')
              .classed('subtitle', true)
              .text(moment(maxDate).format('LL'))
              .style('text-anchor', 'end');

      _.extend(options, {
        marginLeft: 1,
        marginRight: 1,
        marginTop: 1,
        marginBottom: 1 + maxDateTick.node().getBBox().height
      });

      _.extend(options, {
        availableWidth: options.width - options.marginLeft - options.marginRight,
        availableHeight: options.height - options.marginTop - options.marginBottom
      });

      plot.attr('transform', trans(options.marginLeft, options.marginTop));
      xScale.range([0, options.availableWidth]);
      yScale.range([0, options.availableHeight]);

      minDateTick
          .attr('x', options.marginLeft)
          .attr('y', options.height);
      maxDateTick
          .attr('x', options.width - options.marginRight)
          .attr('y', options.height);

      plot.append('path')
          .datum(data)
          .attr('d', line)
          .attr('class', 'line')
          .style('stroke', options.color);

    });
  };

})(window.jQuery);
