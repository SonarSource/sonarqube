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
    height: 30,
    color: '#1f77b4',
    interpolate: 'bundle',
    tension: 1,
    type: 'INT'
  };

  /*
   * data = [
   *   { val: '2015-01-30', count: 30 },
   *   ...
   * ]
   */

  $.fn.sparkline = function (data, opts) {
    $(this).each(function () {
          var options = _.defaults(opts || {}, $(this).data(), defaults);
          if (!options.width) {
            _.extend(options, { width: $(this).width() });
          }

          var container = d3.select(this),
              svg = container.append('svg')
                  .attr('width', options.width)
                  .attr('height', options.height)
                  .classed('sonar-d3', true),

              plot = svg.append('g')
                  .classed('plot', true),

              xScale = d3.time.scale()
                  .domain(d3.extent(data, function (d) {
                    return moment(d.val).toDate();
                  })),

              yScale = d3.scale.linear()
                  .domain(d3.extent(data, function (d) {
                    return d.count;
                  })),

              minValue = yScale.domain()[0],
              maxValue = yScale.domain()[1],

              line = d3.svg.line()
                  .x(function (d) {
                    return xScale(moment(d.val).toDate());
                  })
                  .y(function (d) {
                    return yScale(d.count);
                  })
                  .interpolate(options.interpolate)
                  .tension(options.tension),

              minLabel = plot.append('text')
                  .text(window.formatMeasure(minValue, options.type))
                  .attr('dy', '3px')
                  .style('text-anchor', 'end')
                  .style('font-size', '10px')
                  .style('font-weight', '300')
                  .style('fill', '#aaa'),

              maxLabel = plot.append('text')
                  .text(window.formatMeasure(maxValue, options.type))
                  .attr('dy', '5px')
                  .style('text-anchor', 'end')
                  .style('font-size', '10px')
                  .style('font-weight', '300')
                  .style('fill', '#aaa'),

              maxLabelWidth = Math.max(minLabel.node().getBBox().width, maxLabel.node().getBBox().width) + 3;

          _.extend(options, {
            marginLeft: 1,
            marginRight: 1 + maxLabelWidth,
            marginTop: 6,
            marginBottom: 6
          });

          _.extend(options, {
            availableWidth: options.width - options.marginLeft - options.marginRight,
            availableHeight: options.height - options.marginTop - options.marginBottom
          });

          plot.attr('transform', trans(options.marginLeft, options.marginTop));
          xScale.range([0, options.availableWidth]);
          yScale.range([options.availableHeight, 0]);

          plot.append('path')
              .datum(data)
              .attr('d', line)
              .classed('line', true)
              .style('stroke', options.color);

          minLabel
              .attr('x', options.availableWidth + maxLabelWidth)
              .attr('y', yScale(minValue));
          maxLabel
              .attr('x', options.availableWidth + maxLabelWidth)
              .attr('y', yScale(maxValue));
        }
    );
  };

})(window.jQuery);
