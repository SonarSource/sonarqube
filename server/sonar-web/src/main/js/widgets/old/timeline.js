import moment from 'moment';

window.SonarWidgets = window.SonarWidgets == null ? {} : window.SonarWidgets;

(function () {

  window.SonarWidgets.Timeline = function (container) {

    // Ensure container is html id
    if (container.indexOf('#') !== 0) {
      container = '#' + container;
    }

    this.container = d3.select(container);

    // Set default values
    this._data = [];
    this._metrics = [];
    this._snapshots = [];
    this._events = [];
    this._width = window.SonarWidgets.Timeline.defaults.width;
    this._height = window.SonarWidgets.Timeline.defaults.height;
    this._margin = window.SonarWidgets.Timeline.defaults.margin;

    // Export global variables
    this.data = function (_) {
      return param.call(this, '_data', _);
    };

    this.metrics = function (_) {
      return param.call(this, '_metrics', _);
    };

    this.snapshots = function (_) {
      return param.call(this, '_snapshots', _);
    };

    this.events = function (_) {
      return param.call(this, '_events', _);
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

  };


  window.SonarWidgets.Timeline.prototype.initScalesAndAxis = function () {
    // Configure scales
    var timeDomain = this.data()
        .map(function(_) {
          return d3.extent(_, function(d) {
            return d.x;
          });
        })
        .reduce(function(p, c) {
          return p.concat(c);
        }, []);

    this.time = d3.time.scale().domain(d3.extent(timeDomain));

    this.y = this.data().map(function(_) {
      return d3.scale.linear()
          .domain(d3.extent(_, function(d) { return d.y; }));
    });

    this.color = d3.scale.category10();

    // Configure the axis
    this.timeAxis = d3.svg.axis()
        .scale(this.time)
        .orient('bottom')
        .ticks(5);
  };


  window.SonarWidgets.Timeline.prototype.initEvents = function () {
    var widget = this;
    this.events(this.events().filter(function (event) {
      return event.d >= widget.time.domain()[0];
    }));

    this.gevents = this.gWrap.append('g')
        .attr('class', 'axis events')
        .selectAll('.event-tick')
        .data(this.events());

    this.gevents.enter().append('line')
        .attr('class', 'event-tick')
        .attr('y2', -8);

    this.gevents.exit().remove();


    this.selectSnapshot = function(cl) {
      var sx = widget.time(widget.data()[0][cl].x);

      widget.markers.forEach(function(marker) {
        marker.style('opacity', 0);
        d3.select(marker[0][cl]).style('opacity', 1);
      });

      widget.scanner
          .attr('x1', sx)
          .attr('x2', sx);

      widget.infoDate
          .text(moment(widget.data()[0][cl].x).format('LL'));

      var metricsLines = widget.data().map(function(d, i) {
        return widget.metrics()[i] + ': ' + d[cl].yl;
      });

      metricsLines.forEach(function(d, i) {
        widget.infoMetrics[i].select('text').text(d);
      });

      widget.gevents.attr('y2', -8);
      widget.infoEvent.text('');
      widget.events().forEach(function(d, i) {
        if (d.d - widget.data()[0][cl].x === 0) {
          d3.select(widget.gevents[0][i]).attr('y2', -12);

          widget.infoEvent
              .text(widget.events()[i].l
                  .map(function(e) { return e.n; })
                  .join(', '));
        }
      });
    };


    // Set event listeners
    this.svg.on('mousemove', function() {
      var mx = d3.mouse(widget.plotWrap.node())[0],
          cl = closest(widget.data()[0], mx, function(d) { return widget.time(d.x); });
      widget.selectSnapshot(cl);
    });
  };


  window.SonarWidgets.Timeline.prototype.render = function () {
    var widget = this;

    this.svg = this.container.append('svg')
        .attr('class', 'sonar-d3');

    this.gWrap = this.svg.append('g');

    this.gtimeAxis = this.gWrap.append('g')
        .attr('class', 'axis x');

    this.plotWrap = this.gWrap.append('g')
        .attr('class', 'plot');

    this.scanner = this.plotWrap.append('line');

    this.infoWrap = this.gWrap.append('g')
        .attr('class', 'info');

    this.infoDate = this.infoWrap.append('text')
        .attr('class', 'info-text info-text-bold');

    this.infoEvent = this.infoWrap.append('text')
        .attr('class', 'info-text info-text-small');

    this.gWrap
        .attr('transform', trans(this.margin().left, this.margin().top));

    this.initScalesAndAxis();
    this.showLimitHistoryMessage();

    // Configure lines and points
    this.lines = [];
    this.glines = [];
    this.markers = [];
    this.data().forEach(function(_, i) {
      var line = d3.svg.line()
          .x(function(d) { return widget.time(d.x); })
          .y(function(d) { return widget.y[i](d.y); })
          .interpolate('linear');

      var gline = widget.plotWrap.append('path')
          .attr('class', 'line')
          .style('stroke', function() { return widget.color(i); });

      widget.lines.push(line);
      widget.glines.push(gline);

      var marker = widget.plotWrap.selectAll('.marker').data(_);
      marker.enter().append('circle')
          .attr('class', 'line-marker')
          .attr('r', 3)
          .style('stroke', function() { return widget.color(i); });
      marker.exit().remove();

      widget.markers.push(marker);
    });


    // Configure scanner
    this.scanner
        .attr('class', 'scanner')
        .attr('y1', 0);


    // Configure info
    this.infoWrap
        .attr('transform', trans(0, -30));

    this.infoDate
        .attr('transform', trans(0, 0));

    this.infoMetrics = [];
    this.metrics().forEach(function(d, i) {
      var infoMetric = widget.infoWrap.append('g')
          .attr('class', 'metric-legend')
          .attr('transform', function() { return trans(110 + i * 150, -1); });

      infoMetric.append('text')
          .attr('class', 'info-text-small')
          .attr('transform', trans(10, 0));

      infoMetric.append('circle')
          .attr('class', 'metric-legend-line')
          .attr('transform', trans(0, -4))
          .attr('r', 4)
          .style('fill', function() { return widget.color(i); });

      widget.infoMetrics.push(infoMetric);
    });

    this.initEvents();
    this.update();

    return this;
  };


  window.SonarWidgets.Timeline.prototype.showLimitHistoryMessage = function () {
    var minEvent = d3.min(this.events(), function (d) {
          return d.d;
        }),
        minData = this.time.domain()[0];
    if (minEvent < minData) {
      var maxResultsReachedLabel = this.container.append('div').text(this.limitedHistoricalData);
      maxResultsReachedLabel.classed('max-results-reached-message', true);
    }
  };


  window.SonarWidgets.Timeline.prototype.update = function() {
    var widget = this,
        width = this.container.property('offsetWidth');

    this.width(width > 100 ? width : 100);


    // Update svg canvas
    this.svg
        .attr('width', this.width())
        .attr('height', this.height());


    // Update available width
    this.availableWidth = this.width() - this.margin().left - this.margin().right;


    // Update metric lines
    var metricY = -1;
    this.infoMetrics.forEach(function(metric, i) {
      var x = 120 + i * 170,
          x2 = x + 170;

      if (x2 > widget.availableWidth) {
        metricY += 18;
        x = 120;
      }

      metric
          .transition()
          .attr('transform', function() { return trans(x, metricY); });
    });

    if (metricY > -1) {
      metricY += 17;
    }

    // Update available width
    this.availableHeight = this.height() - this.margin().top - this.margin().bottom - metricY;


    // Update scales
    this.time
        .range([0, this.availableWidth]);

    this.y.forEach(function(scale) {
      scale.range([widget.availableHeight, 0]);
    });


    // Update plot
    this.plotWrap
        .transition()
        .attr('transform', trans(0, metricY));


    // Update the axis
    this.gtimeAxis.attr('transform', trans(0, this.availableHeight + this.margin().bottom - 30 + metricY));

    this.gtimeAxis.transition().call(this.timeAxis);


    // Update lines and points
    this.data().forEach(function(_, i) {
      widget.glines[i]
          .transition()
          .attr('d', widget.lines[i](_));

      widget.markers[i]
          .data(_)
          .transition()
          .attr('transform', function(d) { return trans(widget.time(d.x), widget.y[i](d.y)); });
    });


    // Update scanner
    this.scanner
        .attr('y2', this.availableHeight + 10);


    // Update events
    this.infoEvent
        .attr('transform', trans(0, metricY > -1 ? metricY : 18));

    this.gevents
        .transition()
        .attr('transform', function(d) { return trans(widget.time(d.d), widget.availableHeight + 10 + metricY); });


    // Select latest values if this it the first update
    if (!this.firstUpdate) {
      this.selectSnapshot(widget.data()[0].length - 1);

      this.firstUpdate = true;
    }

  };



  window.SonarWidgets.Timeline.defaults = {
    width: 350,
    height: 150,
    margin: { top: 50, right: 10, bottom: 40, left: 10 }
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

  // Helper for find the closest number in array
  function closest(array, number, getter) {
    var cl = null;
    array.forEach(function(value, i) {
      if (cl == null ||
          Math.abs(getter(value) - number) < Math.abs(getter(array[cl]) - number)) {
        cl = i;
      }
    });
    return cl;
  }

})();
