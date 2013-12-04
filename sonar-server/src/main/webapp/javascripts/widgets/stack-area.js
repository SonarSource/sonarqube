/*global d3:false*/

window.SonarWidgets = window.SonarWidgets == null ? {} : window.SonarWidgets;

(function () {

  window.SonarWidgets.StackArea = function (container) {

    // Ensure container is html id
    if (container.indexOf('#') !== 0) {
      container = '#' + container;
    }

    this.container = d3.select(container);


    // Set default values
    this._data = [];
    this._metrics = [];
    this._snapshots = [];
    this._colors = [];
    this._width = window.SonarWidgets.StackArea.defaults.width;
    this._height = window.SonarWidgets.StackArea.defaults.height;
    this._margin = window.SonarWidgets.StackArea.defaults.margin;

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

    this.colors = function (_) {
      return param.call(this, '_colors', _);
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

  window.SonarWidgets.StackArea.prototype.render = function () {
    var widget = this,
        cl = widget.colors().length;

    this.svg = this.container.append('svg')
        .attr('class', 'sonar-d3');

    this.gWrap = this.svg.append('g');

    this.gtimeAxis = this.gWrap.append('g')
        .attr('class', 'axis x');

    this.plotWrap = this.gWrap.append('g')
        .attr('class', 'plot');

    this.scanner = this.plotWrap.append('line');

    this.infoWrap = this.gWrap.append('g');
    this.infoDate = this.infoWrap.append('text');
    this.infoSnapshot = this.infoWrap.append('text');
    this.infoTotal = this.infoWrap.append('text');

    this.gWrap
        .attr('transform', trans(this.margin().left, this.margin().top));


    // Configure stack
    this.stack = d3.layout.stack();
    this.stackData = this.stack(this.data());
    this.stackDataTop = this.stackData[this.stackData.length - 1];


    // Configure scales
    var timeDomain = this.data()
        .map(function(_) {
          return d3.extent(_, function(d) { return d.x; });
        })
        .reduce(function(p, c) {
          return p.concat(c);
        }, []);

    this.time = d3.time.scale().domain(d3.extent(timeDomain));

    this.y = d3.scale.linear()
        .domain([0, d3.max(this.stackDataTop, function(d) { return d.y0 + d.y; })])
        .nice();

    this.color = function(i) { return widget.colors()[i % cl][0]; };


    // Configure the axis
    this.timeAxis = d3.svg.axis()
        .scale(this.time)
        .orient('bottom')
        .ticks(5);


    // Configure the area
    this.area = d3.svg.area()
        .x(function(d) { return widget.time(d.x); })
        .y0(function(d) { return widget.y(d.y0); })
        .y1(function(d) { return widget.y(d.y0 + d.y); });

    this.areaLine = d3.svg.line()
        .x(function(d) { return widget.time(d.x); })
        .y(function(d) { return widget.y(d.y0 + d.y); });


    // Configure scanner
    this.scanner
        .attr('class', 'scanner')
        .attr('y1', 0);


    // Configure info
    this.infoWrap
        .attr('class', 'info')
        .attr('transform', trans(0, -60));

    this.infoDate
        .attr('class', 'info-text info-text-bold')
        .attr('transform', trans(0, 0));

    this.infoTotal
        .attr('class', 'info-text info-text-small')
        .attr('transform', trans(0, 18));

    this.infoSnapshot
        .attr('class', 'info-text info-text-small')
        .attr('transform', trans(0, 54));

    this.infoMetrics = [];
    var prevX = 110;
    this.metrics().forEach(function(d, i) {
      var infoMetric = widget.infoWrap.append('g');

      var infoMetricText = infoMetric.append('text')
          .attr('class', 'info-text-small')
          .attr('transform', trans(10, 0))
          .text(widget.metrics()[i]);

      infoMetric.append('circle')
          .attr('transform', trans(0, -4))
          .attr('r', 4)
          .style('fill', function() { return widget.color(i); });

      // Align metric labels
      infoMetric
          .attr('transform', function() {
            return trans(prevX, -1 + (i % 3) * 18);
          });

      widget.infoMetrics.push(infoMetric);

      if (i % 3 === 2) {
        prevX += (infoMetricText.node().getComputedTextLength() + 60);
      }
    });


    // Configure events
    this.events = widget.snapshots()
        .filter(function(d) { return d.e.length > 0; });

    this.gevents = this.gWrap.append('g')
        .attr('class', 'axis events')
        .selectAll('.event-tick')
        .data(this.events);

    this.gevents.enter().append('line')
        .attr('class', 'event-tick')
        .attr('y2', -8);


    this.selectSnapshot = function(cl) {
      var dataX = widget.data()[0][cl].x,
          sx = widget.time(dataX),
          snapshotIndex = null,
          eventIndex = null;

      // Update scanner position
      widget.scanner
          .attr('x1', sx)
          .attr('x2', sx);


      // Update info
      widget.infoDate
          .text(d3.time.format('%b %d, %Y')(widget.data()[0][cl].x));

      widget.infoTotal
          .text('Total: ' + d3.format('.1f')(widget.stackDataTop[cl].y0 + widget.stackDataTop[cl].y));


      // Update metric labels
      var metricsLines = widget.data().map(function(d, i) {
        return widget.metrics()[i] + ': ' + d[cl].y;
      });

      metricsLines.forEach(function(d, i) {
        widget.infoMetrics[i].select('text').text(d);
      });


      // Update snapshot info
      this.snapshots().forEach(function(d, i) {
        if (d.d - dataX === 0) {
          snapshotIndex = i;
        }
      });

      if (snapshotIndex != null) {
        widget.infoSnapshot
            .text(this.snapshots()[snapshotIndex].e.join(', '));
      }


      // Update event
      this.events.forEach(function(d, i) {
        if (d.d - dataX === 0) {
          eventIndex = i;
        }
      });

      widget.gevents.attr('y2', -8);
      d3.select(widget.gevents[0][eventIndex]).attr('y2', -12);
    };


    // Set event listeners
    this.svg.on('mousemove', function() {
      var mx = d3.mouse(widget.plotWrap.node())[0],
          cl = closest(widget.data()[0], mx, function(d) { return widget.time(d.x); });
      widget.selectSnapshot(cl);
    });


    this.update();

    return this;
  };



  window.SonarWidgets.StackArea.prototype.update = function() {
    var widget = this,
        width = this.container.property('offsetWidth');

    this.width(width > 100 ? width : 100);


    // Update svg canvas
    this.svg
        .attr('width', this.width())
        .attr('height', this.height());


    // Update available size
    this.availableWidth = this.width() - this.margin().left - this.margin().right;
    this.availableHeight = this.height() - this.margin().top - this.margin().bottom;


    // Update scales
    this.time.range([0, this.availableWidth]);
    this.y.range([widget.availableHeight, 0]);


    // Update the axis
    this.gtimeAxis.attr('transform', trans(0, this.availableHeight + this.margin().bottom - 30));
    this.gtimeAxis.transition().call(this.timeAxis);


    // Update area
    this.garea = this.plotWrap.selectAll('.area')
        .data(this.stackData)
        .enter()
        .insert('path', ':first-child')
        .attr('class', 'area')
        .attr('d', function(d) { return widget.area(d); })
        .style("fill", function(d, i) { return widget.color(i); });

    this.gareaLine = this.plotWrap.selectAll('.area-line')
        .data(this.stackData)
        .enter()
        .insert('path')
        .attr('class', 'area-line')
        .attr('d', function(d) { return widget.areaLine(d); })
        .style('fill', 'none')
        .style('stroke', '#808080');


    // Update scanner
    this.scanner.attr('y2', this.availableHeight + 10);


    // Update events
    this.gevents
        .transition()
        .attr('transform', function(d) {
          return trans(widget.time(d.d), widget.availableHeight + 10);
        });


    // Select latest values if this it the first update
    if (!this.firstUpdate) {
      this.selectSnapshot(widget.data()[0].length - 1);

      this.firstUpdate = true;
    }

  };



  window.SonarWidgets.StackArea.defaults = {
    width: 350,
    height: 150,
    margin: { top: 80, right: 10, bottom: 40, left: 40 }
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
