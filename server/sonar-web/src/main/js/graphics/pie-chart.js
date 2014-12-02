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
