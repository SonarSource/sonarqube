/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import $ from 'jquery';
import { max } from 'd3-array';
import { select } from 'd3-selection';
import { scaleLinear, scaleBand } from 'd3-scale';
import { isSameDay, toNotSoISOString } from '../../helpers/dates';

function trans(left, top) {
  return `translate(${left}, ${top})`;
}

const defaults = function() {
  return {
    height: 140,
    color: '#1f77b4',

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

$.fn.barchart = function(data) {
  $(this).each(function() {
    const options = { ...defaults(), ...$(this).data() };
    Object.assign(options, {
      width: options.width || $(this).width(),
      endDate: options.endDate ? new Date(options.endDate) : null
    });

    const container = select(this);
    const svg = container
      .append('svg')
      .attr('width', options.width + 2)
      .attr('height', options.height + 2)
      .classed('sonar-d3', true);
    const plot = svg.append('g').classed('plot', true);
    const xScale = scaleBand().domain(data.map((d, i) => i));
    const yScaleMax = max(data, d => d.count);
    const yScale = scaleLinear().domain([0, yScaleMax]);

    Object.assign(options, {
      availableWidth: options.width - options.marginLeft - options.marginRight,
      availableHeight: options.height - options.marginTop - options.marginBottom
    });

    plot.attr('transform', trans(options.marginLeft, options.marginTop));
    xScale.rangeRound([0, options.availableWidth]).paddingInner(0.05);
    yScale.range([3, options.availableHeight]);

    const barWidth = xScale.bandwidth();
    const bars = plot.selectAll('g').data(data);

    if (barWidth > 0) {
      const barsEnter = bars
        .enter()
        .append('g')
        .attr('transform', (d, i) =>
          trans(xScale(i), Math.ceil(options.availableHeight - yScale(d.count)))
        );

      barsEnter
        .append('rect')
        .style('fill', options.color)
        .attr('width', barWidth)
        .attr('height', d => Math.floor(yScale(d.count)))
        .style('cursor', 'pointer')
        .attr('data-period-start', d => toNotSoISOString(new Date(d.val)))
        .attr('data-period-end', (d, i) => {
          const ending = i < data.length - 1 ? new Date(data[i + 1].val) : options.endDate;
          if (ending) {
            return toNotSoISOString(ending);
          } else {
            return '';
          }
        })
        .attr('title', (d, i) => {
          const beginning = new Date(d.val);
          let ending = options.endDate;
          if (i < data.length - 1) {
            ending = new Date(data[i + 1].val);
            ending.setDate(ending.getDate() - 1);
          }
          if (ending) {
            return (
              d.text +
              '<br>' +
              beginning.format('LL') +
              (isSameDay(ending, beginning) ? '' : ' – ' + ending.format('LL'))
            );
          } else {
            return d.text + '<br>' + beginning.format('LL');
          }
        })
        .attr('data-placement', 'bottom')
        .attr('data-toggle', 'tooltip');
      const maxValue = max(data, d => d.count);
      let isValueShown = false;
      barsEnter
        .append('text')
        .classed('subtitle', true)
        .attr('transform', trans(barWidth / 2, -4))
        .style('text-anchor', 'middle')
        .text(d => {
          const text = !isValueShown && d.count === maxValue ? d.text : '';
          isValueShown = d.count === maxValue;
          return text;
        });
      $(this).find('[data-toggle=tooltip]').tooltip({ container: 'body', html: true });
    }
  });
};
