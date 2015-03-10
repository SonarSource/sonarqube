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

  var defaults = function () {
    return {
      height: 140,
      color: '#1f77b4',
      interpolate: 'basis',
      endDate: moment().format('YYYY-MM-DD'),

      marginLeft: 1,
      marginRight: 1,
      marginTop: 18,
      marginBottom: 1
    };
  };

  /*
   * data = [
   *   { val: '2015-01-30', count: 30 },
   *   ...
   * ]
   */

  $.fn.barchart = function (data) {
    $(this).each(function () {
      var options = _.defaults($(this).data(), defaults());
      _.extend(options, {
        width: options.width || $(this).width(),
        endDate: moment(options.endDate)
      });

      var container = d3.select(this),
          svg = container.append('svg')
              .attr('width', options.width + 2)
              .attr('height', options.height + 2)
              .classed('sonar-d3', true),

          plot = svg.append('g')
              .classed('plot', true),

          xScale = d3.scale.ordinal()
              .domain(data.map(function (d, i) {
                return i;
              })),

          yScaleMax = d3.max(data, function (d) {
            return d.count;
          }),
          yScale = d3.scale.linear()
              .domain([0, yScaleMax]);

      _.extend(options, {
        availableWidth: options.width - options.marginLeft - options.marginRight,
        availableHeight: options.height - options.marginTop - options.marginBottom
      });

      plot.attr('transform', trans(options.marginLeft, options.marginTop));
      xScale.rangeRoundBands([0, options.availableWidth], 0.05, 0);
      yScale.range([3, options.availableHeight]);

      var barWidth = xScale.rangeBand(),
          bars = plot.selectAll('g').data(data);

      if (barWidth > 0) {
        var barsEnter = bars.enter()
            .append('g')
            .attr('transform', function (d, i) {
              return trans(xScale(i), Math.ceil(options.availableHeight - yScale(d.count)));
            });

        barsEnter.append('rect')
            .style('fill', options.color)
            .attr('width', barWidth)
            .attr('height', function (d) {
              return Math.floor(yScale(d.count));
            })
            .style('cursor', 'pointer')
            .attr('data-period-start', function (d) {
              return moment(d.val).format('YYYY-MM-DD');
            })
            .attr('data-period-end', function (d, i) {
              var beginning = moment(d.val),
                  ending = i < data.length - 1 ? moment(data[i + 1].val).subtract(1, 'days') : options.endDate,
                  isSameDay = ending.diff(beginning, 'days') <= 1;
              if (isSameDay) {
                ending.add(1, 'days');
              }
              return ending.format('YYYY-MM-DD');
            })
            .attr('title', function (d, i) {
              var beginning = moment(d.val),
                  ending = i < data.length - 1 ? moment(data[i + 1].val).subtract(1, 'days') : options.endDate,
                  isSameDay = ending.diff(beginning, 'days') <= 1;
              return d.count + '<br>' + beginning.format('LL') + (isSameDay ? '' : (' – ' + ending.format('LL')));
            })
            .attr('data-placement', 'bottom')
            .attr('data-toggle', 'tooltip');

        var maxValue = d3.max(data, function (d) {
              return d.count;
            }),
            isValueShown = false;

        barsEnter.append('text')
            .classed('subtitle', true)
            .attr('transform', trans(barWidth / 2, -4))
            .style('text-anchor', 'middle')
            .text(function (d) {
              var text = !isValueShown && d.count === maxValue ? d.count : '';
              isValueShown = d.count === maxValue;
              return text;
            });

        $(this).find('[data-toggle=tooltip]').tooltip({ container: 'body', html: true });
      }
    });
  };

})(window.jQuery);
