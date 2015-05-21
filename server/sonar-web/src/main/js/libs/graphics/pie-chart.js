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
    size: 40,
    thickness: 8,
    color: '#1f77b4',
    baseColor: '#e6e6e6'
  };

  $.fn.pieChart = function () {
    $(this).each(function () {
      var data = [
            $(this).data('value'),
            $(this).data('max') - $(this).data('value')
          ],
          options = _.defaults($(this).data(), defaults),
          radius = options.size / 2;

      var container = d3.select(this),
          svg = container.append('svg')
              .attr('width', options.size)
              .attr('height', options.size),
          plot = svg.append('g')
              .attr('transform', trans(radius, radius)),
          arc = d3.svg.arc()
              .innerRadius(radius - options.thickness)
              .outerRadius(radius),
          pie = d3.layout.pie()
              .sort(null)
              .value(function (d) {
                return d;
              }),
          colors = function (i) {
            return i === 0 ? options.color : options.baseColor;
          },
          sectors = plot.selectAll('path')
              .data(pie(data));

      sectors.enter()
          .append('path')
          .style('fill', function (d, i) {
            return colors(i);
          })
          .attr('d', arc);
    });
  };

})(window.jQuery);
