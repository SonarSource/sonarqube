/*global d3:false, baseUrl:false */

window.SonarWidgets = window.SonarWidgets == null ? {} : window.SonarWidgets;

(function () {

  window.SonarWidgets.BubbleChart = function () {
    // Set default values
    this._components = [];
    this._metrics = [];
    this._metricsPriority = [];
    this._width = window.SonarWidgets.BubbleChart.defaults.width;
    this._height = window.SonarWidgets.BubbleChart.defaults.height;
    this._margin = window.SonarWidgets.BubbleChart.defaults.margin;
    this._xLog = window.SonarWidgets.BubbleChart.defaults.xLog;
    this._yLog = window.SonarWidgets.BubbleChart.defaults.yLog;
    this._bubbleColor = window.SonarWidgets.BubbleChart.defaults.bubbleColor;
    this._bubbleColorUndefined = window.SonarWidgets.BubbleChart.defaults.bubbleColorUndefined;
    this._options = {};

    // Export global variables
    this.metrics = function (_) {
      return param.call(this, '_metrics', _);
    };

    this.metricsPriority = function (_) {
      return param.call(this, '_metricsPriority', _);
    };

    this.components = function (_) {
      return param.call(this, '_components', _);
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

    this.options = function (_) {
      return param.call(this, '_options', _);
    };
  };

  window.SonarWidgets.BubbleChart.prototype.render = function (container) {
    var widget = this,
        containerS = container;

    container = d3.select(container);

    var validData = this.components().reduce(function(p, c) {
      return p && !!c.measures[widget.metricsPriority()[0]] && !!c.measures[widget.metricsPriority()[1]];
    }, true);

    if (!validData) {
      container.text(this.options().noMainMetric);
      return;
    }


    this.width(container.property('offsetWidth'));

    this.svg = container.append('svg')
        .attr('class', 'sonar-d3');
    this.gWrap = this.svg.append('g');

    this.gxAxis = this.gWrap.append('g');
    this.gyAxis = this.gWrap.append('g');

    this.gGrid = this.gWrap.append('g');
    this.gxGrid = this.gGrid.append('g');
    this.gyGrid = this.gGrid.append('g');

    this.plotWrap = this.gWrap.append('g');

    this.infoWrap = this.gWrap.append('g');
    this.infoDate = this.infoWrap.append('text');

    this.gWrap
        .attr('transform', trans(this.margin().left, this.margin().top));


    // Configure metrics
    this.xMetric = this.metricsPriority()[0];
    this.getXMetric = function(d) {
      return d.measures[widget.xMetric].val;
    };

    this.yMetric = this.metricsPriority()[1];
    this.getYMetric = function(d) {
      return d.measures[widget.yMetric].val;
    };

    this.sizeMetric = this.metricsPriority()[2];
    this.getSizeMetric = function(d) {
      return !!d.measures[widget.sizeMetric] ? d.measures[widget.sizeMetric].val : 0;
    };


    // Configure scales
    this
        .xLog(this.options().xLog)
        .yLog(this.options().yLog);

    this.x = this.xLog() ? d3.scale.log() : d3.scale.linear();
    this.y = this.yLog() ? d3.scale.log() : d3.scale.linear();
    this.size = d3.scale.linear();

    this.x
        .domain(d3.extent(this.components(), function (d) {
          return widget.getXMetric(d)
        }))
        .range([0, this.availableWidth]);

    this.y
        .domain(d3.extent(this.components(), function (d) {
          return widget.getYMetric(d)
        }))
        .range([this.availableHeight, 0]);

    this.size
        .domain(d3.extent(this.components(), function (d) {
          return widget.getSizeMetric(d)
        }))
        .range([5, 45]);


    // Create bubbles
    this.items = this.plotWrap.selectAll('.item')
        .data(this.components());


    // Render bubbles
    this.items.enter().append('g')
        .attr('class', 'item')
        .attr('name', function (d) {
          return d.longName;
        })
        .style('cursor', 'pointer')
        .append('circle')
        .attr('r', function (d) {
          return widget.size(widget.getSizeMetric(d));
        })
        .style('fill', function (d) {
          // TODO widget.bubbleColorUndefined()
          return widget.bubbleColor();
        })
        .style('fill-opacity', 0.2)
        .style('stroke', function (d) {
          // TODO widget.bubbleColorUndefined()
          return widget.bubbleColor();
        })
        .style('transition', 'all 0.2s ease');

    this.items.exit().remove();

    this.items.sort(function (a, b) {
      return widget.getSizeMetric(b) - widget.getSizeMetric(a);
    });


    // Set event listeners
    this.items
        .on('click', function (d) {
          switch (d.qualifier) {
            case 'CLA':
            case 'FIL':
              window.location = widget.options().baseUrl + encodeURIComponent(d.key) +
                  '?metric=' + encodeURIComponent(widget.sizeMetric);
              break;
            default:
              window.location = widget.options().baseUrl + encodeURIComponent(d.key);
          }
        })
        .on('mouseenter', function (d) {
          d3.select(this).select('circle')
              .style('fill-opacity', 0.8);

          widget.infoDate.text(d.longName);

          var metricLines = [
            { metric: widget.metrics()[widget.xMetric].name, value: d.measures[widget.xMetric].fval },
            { metric: widget.metrics()[widget.yMetric].name, value: d.measures[widget.yMetric].fval },
            { metric: widget.metrics()[widget.sizeMetric].name, value: (!!d.measures[widget.sizeMetric] ? d.measures[widget.sizeMetric].fval : '–') }
          ];

          var lastX = 0;
          widget.infoMetrics
              .data(metricLines)
              .text(function(d) { return d.metric + ': ' + d.value; })
              .attr('transform', function(d, i) {
                var posX = lastX;
                lastX += widget.infoMetricWidth[i];
                return trans(posX, 20);
              });
        })
        .on('mouseleave', function () {
          d3.select(this).select('circle')
              .style('fill-opacity', 0.2);

          widget.infoDate.text('');
          widget.infoMetrics.text('');
        });


    // Configure axis
    // X
    this.xAxis = d3.svg.axis()
        .scale(widget.x)
        .orient('bottom');

    this.gxAxisLabel = this.gxAxis.append('text')
        .text(this.metrics()[this.xMetric].name)
        .style('font-weight', 'bold')
        .style('text-anchor', 'middle');


    // Y
    this.yAxis = d3.svg.axis()
        .scale(widget.y)
        .orient('left');

    this.gyAxis.attr('transform', trans(60 - this.margin().left, 0));

    this.gyAxisLabel = this.gyAxis.append('text')
        .text(this.metrics()[this.yMetric].name)
        .style('font-weight', 'bold')
        .style('text-anchor', 'middle');


    // Configure grid
    this.gxGridLines = this.gxGrid.selectAll('line').data(widget.x.ticks()).enter()
        .append('line');

    this.gyGridLines = this.gyGrid.selectAll('line').data(widget.y.ticks()).enter()
        .append('line');

    this.gGrid.selectAll('line')
        .style('stroke', '#000')
        .style('stroke-opacity', 0.25);


    // Configure info placeholders
    this.infoWrap
        .attr('transform', trans(-this.margin().left, -this.margin().top + 20));

    this.infoDate
        .style('text-anchor', 'start')
        .style('font-weight', 'bold');

    var metricLines = [widget.metrics().x, widget.metrics().y, widget.metrics().size];
    widget.infoMetrics = widget.infoWrap.selectAll('.metric')
        .data(metricLines);
    widget.infoMetrics.enter().append('text').attr('class', 'metric info-text-small')
        .text(function(d) { return d; });
    widget.infoMetricWidth = [];
    widget.infoMetrics.each(function() {
      widget.infoMetricWidth.push(this.getComputedTextLength() + 140);
    });
    widget.infoMetrics.text('');


    // Update widget
    this.update(containerS);

    return this;
  };



  window.SonarWidgets.BubbleChart.prototype.update = function(container) {
    container = d3.select(container);

    var widget = this,
        width = container.property('offsetWidth');

    this.width(width > 100 ? width : 100);


    // Update svg canvas
    this.svg
        .attr('width', this.width())
        .attr('height', this.height());


    // Update available size
    this.availableWidth = this.width() - this.margin().left - this.margin().right;
    this.availableHeight = this.height() - this.margin().top - this.margin().bottom;


    // Update scales
    this.x
        .domain(d3.extent(this.components(), function (d) {
          return widget.getXMetric(d);
        }))
        .range([0, this.availableWidth]);

    this.y
        .domain(d3.extent(this.components(), function (d) {
          return widget.getYMetric(d);
        }))
        .range([this.availableHeight, 0]);


    if (this.x.domain()[0] === 0 && this.x.domain()[1] === 0) {
      this.x.domain([0, 1]);
    }

    if (this.y.domain()[0] === 0 && this.y.domain()[1] === 0) {
      this.y.domain([0, 1]);
    }


    // Avoid zero values when using log scale
    if (this.xLog) {
      var xDomain = this.x.domain();
      this.x
          .domain([xDomain[0] > 0 ? xDomain[0] : 0.1, xDomain[1]])
          .clamp(true);
    }

    if (this.yLog) {
      var yDomain = this.y.domain();
      this.y
          .domain([yDomain[0] > 0 ? yDomain[0] : 0.1, yDomain[1]])
          .clamp(true);
    }


    // Adjust the scale domain so the circles don't cross the bounds
    // X
    var minX = d3.min(this.components(), function (d) {
          return widget.x(widget.getXMetric(d)) - widget.size(widget.getSizeMetric(d));
        }),
        maxX = d3.max(this.components(), function (d) {
          return widget.x(widget.getXMetric(d)) + widget.size(widget.getSizeMetric(d));
        }),
        dMinX = minX < 0 ? this.x.range()[0] - minX : this.x.range()[0],
        dMaxX = maxX > this.x.range()[1] ? maxX - this.x.range()[1] : 0;
    this.x.range([dMinX, this.availableWidth - dMaxX]);

    // Y
    var minY = d3.min(this.components(), function (d) {
          return widget.y(widget.getYMetric(d)) - widget.size(widget.getSizeMetric(d));
        }),
        maxY = d3.max(this.components(), function (d) {
          return widget.y(widget.getYMetric(d)) + widget.size(widget.getSizeMetric(d));
        }),
        dMinY = minY < 0 ? this.y.range()[1] - minY: this.y.range()[1],
        dMaxY = maxY > this.y.range()[0] ? maxY - this.y.range()[0] : 0;
    this.y.range([this.availableHeight - dMaxY, dMinY]);


    // Format improvement for log scales
    // X
    if (this.xLog()) {
      this.xAxis.tickFormat(function (d) {
        var ticksCount = widget.availableWidth / 50;
        return widget.x.tickFormat(ticksCount, d3.format(",d"))(d);
      });
    }

    // Y
    if (this.yLog()) {
      this.yAxis.tickFormat(function (d) {
        var ticksCount = widget.availableHeight / 30;
        return widget.y.tickFormat(ticksCount, d3.format(",d"))(d);
      });
    }


    // Make scale's domains nice
    this.x.nice();
    this.y.nice();


    // Update bubbles position
    this.items
        .transition()
        .attr('transform', function (d) {
          return trans(widget.x(widget.getXMetric(d)), widget.y(widget.getYMetric(d)));
        });


    // Update axis
    // X
    this.gxAxis.attr('transform', trans(0, this.availableHeight + this.margin().bottom - 40));

    this.gxAxis.transition().call(this.xAxis);

    this.gxAxis.selectAll('path')
        .style('fill', 'none')
        .style('stroke', '#444');

    this.gxAxis.selectAll('text')
        .style('fill', '#444');

    this.gxAxisLabel
        .attr('transform', trans(this.availableWidth / 2, 35));

    // Y
    this.gyAxis.transition().call(this.yAxis);

    this.gyAxis.selectAll('path')
        .style('fill', 'none')
        .style('stroke', '#444');

    this.gyAxis.selectAll('text')
        .style('fill', '#444');

    this.gyAxisLabel
        .attr('transform', trans(-45, this.availableHeight / 2) + ' rotate(-90)');


    // Update grid
    this.gxGridLines
        .transition()
        .attr({
          x1: function (d) {
            return widget.x(d);
          },
          x2: function (d) {
            return widget.x(d);
          },
          y1: widget.y.range()[0],
          y2: widget.y.range()[1]
        });

    this.gyGridLines
        .transition()
        .attr({
          x1: widget.x.range()[0],
          x2: widget.x.range()[1],
          y1: function (d) {
            return widget.y(d);
          },
          y2: function (d) {
            return widget.y(d);
          }
        });
  };



  window.SonarWidgets.BubbleChart.defaults = {
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
