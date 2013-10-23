window.SonarWidgets = window.SonarWidgets == null ? {} : window.SonarWidgets;

(function () {

  SonarWidgets.BubbleChart = function () {
    // Set default values
    this._data = [];
    this._metrics = [];
    this._width = SonarWidgets.BubbleChart.defaults.width;
    this._height = SonarWidgets.BubbleChart.defaults.height;
    this._margin = SonarWidgets.BubbleChart.defaults.margin;
    this._xLog = SonarWidgets.BubbleChart.defaults.xLog;
    this._yLog = SonarWidgets.BubbleChart.defaults.yLog;
    this._bubbleColor = SonarWidgets.BubbleChart.defaults.bubbleColor;
    this._bubbleColorUndefined = SonarWidgets.BubbleChart.defaults.bubbleColorUndefined;

    // Export global variables
    this.data = function (_) {
      return param.call(this, '_data', _);
    };

    this.metrics = function (_) {
      return param.call(this, '_metrics', _);
    };

    this.width = function (_) {
      return param.call(this, '_width', _);
    };

    this.height = function (_) {
      return param.call(this, '_height', _);
    };

    this.margin = function (_) {
      return param.call(this, '_margin', _);
    };

    this.xLog = function (_) {
      return param.call(this, '_xLog', _);
    };

    this.yLog = function (_) {
      return param.call(this, '_yLog', _);
    };

    this.bubbleColor = function (_) {
      return param.call(this, '_bubbleColor', _);
    };

    this.bubbleColorUndefined = function (_) {
      return param.call(this, '_bubbleColorUndefined', _);
    };
  };

  SonarWidgets.BubbleChart.prototype.render = function (container) {
    container = d3.select(container);

    var widget = this;

    this.width(container.property('offsetWidth'));

    var svg = container.append('svg'),
        gWrap = svg.append('g'),

        gxAxis = gWrap.append('g'),
        gyAxis = gWrap.append('g'),

        gGrid = gWrap.append('g'),
        gxGrid = gGrid.append('g'),
        gyGrid = gGrid.append('g'),

        plotWrap = gWrap.append('g'),

        infoWrap = gWrap.append('g'),
        infoName = infoWrap.append('text'),
        infoMetrics = infoWrap.append('text');

    svg
        .attr('width', this.width())
        .attr('height', this.height());

    gWrap
        .attr('transform', trans(this.margin().left, this.margin().top));


    var availableWidth = this.width() - this.margin().left - this.margin().right,
        availableHeight = this.height() - this.margin().top - this.margin().bottom;


    // Configure scales
    var x = this.xLog() ? d3.scale.log() : d3.scale.linear(),
        y = this.yLog() ? d3.scale.log() : d3.scale.linear(),
        size = d3.scale.linear();

    x
        .domain(d3.extent(this.data(), function (d) {
          return d.xMetric
        }))
        .range([0, availableWidth]);

    y
        .domain(d3.extent(this.data(), function (d) {
          return d.yMetric
        }))
        .range([availableHeight, 0]);

    size
        .domain(d3.extent(this.data(), function (d) {
          return d.sizeMetric
        }))
        .range([10, 50]);


    // Avoid zero values when using log scale
    if (this.xLog) {
      var xDomain = x.domain();
      x
          .domain([xDomain[0] > 0 ? xDomain[0] : 0.1, xDomain[1]])
          .clamp(true);
    }

    if (this.yLog) {
      var yDomain = y.domain();
      y
          .domain([yDomain[0] > 0 ? yDomain[0] : 0.1, yDomain[1]])
          .clamp(true);
    }


    // Adjust the scale domain so the circles don't cross the bounds
    // X
    var minX = d3.min(this.data(), function (d) {
          return x(d.xMetric) - size(d.sizeMetric)
        }),
        maxX = d3.max(this.data(), function (d) {
          return x(d.xMetric) + size(d.sizeMetric)
        }),
        dMinX = x.range()[0] - minX,
        dMaxX = maxX - x.range()[1];
    x.range([dMinX, availableWidth - dMaxX]);

    // Y
    var minY = d3.min(this.data(), function (d) {
          return y(d.yMetric) - size(d.sizeMetric)
        }),
        maxY = d3.max(this.data(), function (d) {
          return y(d.yMetric) + size(d.sizeMetric)
        }),
        dMinY = y.range()[1] - minY,
        dMaxY = maxY - y.range()[0];
    y.range([availableHeight - dMaxY, dMinY]);

    x.nice();
    y.nice();


    // Render items
    var items = plotWrap.selectAll('.item')
        .data(this.data());

    items
        .enter().append('g')
        .attr('class', 'item')
        .attr('name', function (d) {
          return d.longName
        })
        .style('cursor', 'pointer')
        .append('circle')
        .attr('r', function (d) {
          return size(d.sizeMetric)
        })
        .attr('transform', function (d) {
          return trans(x(d.xMetric), y(d.yMetric));
        })
        .style('fill', function (d) {
          return d.sizeMetricFormatted !== '-' ?
              widget.bubbleColor() :
              widget.bubbleColorUndefined()
        })
        .style('fill-opacity', 0.2)
        .style('stroke', function (d) {
          return d.sizeMetricFormatted !== '-' ?
              widget.bubbleColor() :
              widget.bubbleColorUndefined()
        })
        .style('transition', 'all 0.2s ease');

    items.exit().remove();

    items.sort(function (a, b) {
      return b.sizeMetric - a.sizeMetric
    });


    // Set event listeners
    items
        .on('click', function (d) {
          var url = baseUrl + '/resource/index/' + d.key + '?display_title=true&metric=ncloc';
          window.open(url, '', 'height=800,width=900,scrollbars=1,resizable=1');
        })
        .on('mouseenter', function (d) {
          d3.select(this).select('circle')
              .style('fill-opacity', 0.8);

          infoName.text(d.longName);
          infoMetrics.text(
              widget.metrics().x + ': ' + d.xMetricFormatted + '; ' +
                  widget.metrics().y + ': ' + d.yMetricFormatted + '; ' +
                  widget.metrics().size + ': ' + d.sizeMetricFormatted);
        })
        .on('mouseleave', function () {
          d3.select(this).select('circle')
              .style('fill-opacity', 0.2);

          infoName.text('');
          infoMetrics.text('');
        });


    // Render axis
    // X
    var xAxis = d3.svg.axis()
        .scale(x)
        .orient('bottom');

    gxAxis.attr('transform', trans(0, availableHeight + this.margin().bottom - 40));

    gxAxis.call(xAxis);

    gxAxis.selectAll('path')
        .style('fill', 'none')
        .style('stroke', '#000');

    gxAxis.append('text')
        .text(this.metrics().x)
        .style('font-weight', 'bold')
        .style('text-anchor', 'middle')
        .attr('transform', trans(availableWidth / 2, 35));

    // Y
    var yAxis = d3.svg.axis()
        .scale(y)
        .orient('left');

    gyAxis.attr('transform', trans(60 - this.margin().left, 0));

    gyAxis.call(yAxis);

    gyAxis.selectAll('path')
        .style('fill', 'none')
        .style('stroke', '#000');

    gyAxis.append('text')
        .text(this.metrics().y)
        .style('font-weight', 'bold')
        .style('text-anchor', 'middle')
        .attr('transform', trans(-45, availableHeight / 2) + ' rotate(-90)');


    // Render grid
    gxGrid.selectAll('.gridline').data(x.ticks()).enter()
        .append('line')
        .attr({
          'class': 'gridline',
          x1: function (d) {
            return x(d)
          },
          x2: function (d) {
            return x(d)
          },
          y1: y.range()[0],
          y2: y.range()[1]
        });

    gyGrid.selectAll('.gridline').data(y.ticks()).enter()
        .append('line')
        .attr({
          'class': 'gridline',
          x1: x.range()[0],
          x2: x.range()[1],
          y1: function (d) {
            return y(d)
          },
          y2: function (d) {
            return y(d)
          }
        });

    gGrid.selectAll('.gridline')
        .style('stroke', '#000')
        .style('stroke-opacity', 0.25);


    // Render info
    infoWrap
        .attr('transform', trans(-this.margin().left, -this.margin().top + 20));

    infoName
        .style('text-anchor', 'start')
        .style('font-weight', 'bold');

    infoMetrics
        .attr('transform', trans(0, 20));

    return this;
  };

  SonarWidgets.BubbleChart.defaults = {
    width: 350,
    height: 150,
    margin: { top: 60, right: 10, bottom: 50, left: 70 },
    xLog: false,
    yLog: false,
    bubbleColor: '#4b9fd5',
    bubbleColorUndefined: '#b3b3b3'
  };


  // Some helper functions

  // Gets or sets parameter
  function param(name, value) {
    if (value == null) {
      return this[name];
    } else {
      this[name] = value;
      return this;
    }
  }

  // Helper for create the translate(x, y) string
  function trans(left, top) {
    return 'translate(' + left + ', ' + top + ')';
  }

})();
