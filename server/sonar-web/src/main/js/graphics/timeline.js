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
    interpolate: 'basis',
    type: 'UNKNOWN'
  };

  /*
   * data = [
   *   { val: '2015-01-30', count: 30 },
   *   ...
   * ]
   */

  $.fn.timeline = function (data, opts) {
    $(this).each(function () {
          var options = _.defaults(opts || {}, $(this).data(), defaults);
          _.extend(options, { width: $(this).width() });

          var container = d3.select(this),
              svg = container.append('svg')
                  .attr('width', options.width + 12)
                  .attr('height', options.height + 12)
                  .classed('sonar-d3', true),

              extra = svg.append('g'),

              plot = svg.append('g')
                  .classed('plot', true),

              xScale = d3.time.scale()
                  .domain(d3.extent(data, function (d) {
                    return new Date(d.val);
                  })),

              yScale = d3.scale.linear()
                  .domain(d3.extent(data, function (d) {
                    return d.count;
                  })),

              line = d3.svg.line()
                  .x(function (d) {
                    return xScale(new Date(d.val));
                  })
                  .y(function (d) {
                    return yScale(d.count);
                  })
                  .interpolate(options.interpolate);

          // Medians
          var medianValue = getNiceMedian(0.5, data, function (d) {
                return d.count;
              }),
              medianLabel = extra.append('text')
                  .text(window.formatMeasure(medianValue, options.type))
                  .style('text-anchor', 'end')
                  .style('font-size', '10px')
                  .style('fill', '#ccc')
                  .attr('dy', '0.32em'),
              medianLabelWidth = medianLabel.node().getBBox().width;

          _.extend(options, {
            marginLeft: 1,
            marginRight: 1 + medianLabelWidth + 4,
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

          medianLabel
              .attr('x', options.width - 1)
              .attr('y', options.marginTop + yScale(medianValue));
          extra.append('line')
              .attr('x1', options.marginLeft)
              .attr('y1', options.marginTop + yScale(medianValue))
              .attr('x2', options.availableWidth + options.marginLeft)
              .attr('y2', options.marginTop + yScale(medianValue))
              .style('stroke', '#eee')
              .style('shape-rendering', 'crispedges');
        }
    )
    ;
  };

  function getNiceMedian (p, array, accessor) {
    var min = d3.min(array, accessor),
        max = d3.max(array, accessor),
        median = d3.median(array, accessor),
        threshold = (max - min) / 2,
        threshold10 = Math.pow(10, Math.floor(Math.log(threshold) / Math.LN10) - 1);
    return (p - 0.5) > 0.0001 ?
        Math.floor(median / threshold10) * threshold10 :
        Math.ceil(median / threshold10) * threshold10;
  }

})
(window.jQuery);
