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

  $.fn.barchart = function (data) {
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
        marginLeft: 1,
        marginRight: 1,
        marginTop: 18,
        marginBottom: 1
      });

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
            .attr('title', function (d, i) {
              var beginning = moment(d.val),
                  ending = i < data.length - 1 ? moment(data[i].val).subtract(1, 'days') : moment();
              return d.count + ' | ' + beginning.format('LL') + ' - ' + ending.format('LL');
            })
            .attr('data-placement', 'right')
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

        $(this).find('[data-toggle=tooltip]').tooltip({ container: 'body' });
      }
    });
  };

})(window.jQuery);
