/*global d3:false, _:false */
/*jshint eqnull:true */

window.SonarWidgets = window.SonarWidgets == null ? {} : window.SonarWidgets;

(function () {

  window.SonarWidgets.Histogram = function () {
    // Set default values
    this._components = [];
    this._metrics = [];
    this._metricsPriority = [];
    this._width = window.SonarWidgets.Histogram.defaults.width;
    this._height = window.SonarWidgets.Histogram.defaults.height;
    this._margin = window.SonarWidgets.Histogram.defaults.margin;
    this._axisWidth = window.SonarWidgets.Histogram.defaults.axisWidth;
    this._options = {};

    this._lineHeight = 20;


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

    this.axisWidth = function (_) {
      return param.call(this, '_axisWidth', _);
    };

    this.options = function (_) {
      return param.call(this, '_options', _);
    };
  };

  window.SonarWidgets.Histogram.prototype.render = function (container) {
    var widget = this,
        containerS = container;

    container = d3.select(container);

    this.width(container.property('offsetWidth'));

    this.svg = container.append('svg')
        .attr('class', 'sonar-d3');
    this.gWrap = this.svg.append('g');

    this.plotWrap = this.gWrap.append('g')
        .classed('plot', true);

    this.gWrap
        .attr('transform', trans(this.margin().left, this.margin().top));


    // Configure metrics
    this.mainMetric = this.metricsPriority()[0];
    this.getMainMetric = function(d) {
      return d.measures[widget.mainMetric].val;
    };
    this.fm = function(value, name) {
      var type = this.metrics()[name].type;

      switch (type) {
        case 'FLOAT':
          return d3.format('.1f')(value);
        case 'INT':
          return d3.format('d')(value);
        default :
          return value;
      }
    };


    // Configure scales
    this.x = d3.scale.ordinal();
    this.y = d3.scale.linear();
    this.color = d3.scale.ordinal()
        .range([
          '#1f77b4', '#aec7e8', '#3182bd', '#6baed6',
          '#ff7f0e', '#ffbb78', '#e6550d', '#fd8d3c',
          '#2ca02c', '#98df8a', '#31a354', '#74c476',
          '#d62728', '#ff9896', '#ad494a', '#d6616b',
          '#9467bd', '#c5b0d5', '#756bb1', '#9e9ac8',
          '#8c564b', '#c49c94', '#ad494a', '#d6616b',
          '#e377c2', '#f7b6d2', '#ce6dbd', ' #de9ed6',
          '#7f7f7f', '#c7c7c7', '#969696', ' #bdbdbd',
          '#bcbd22', '#dbdb8d', '#8ca252', ' #b5cf6b',
          '#17becf', '#9edae5', '#6baed6', ' #9ecae1'
        ]);


    // Configure axis
    this.y1 = d3.scale.linear();

    this.axis = d3.svg.axis()
        .scale(this.y1)
        .orient("left");

    this.gAxis = this.gWrap.append("g")
        .attr("class", "y axis");


    // Configure details
    this._metricsCount = Object.keys(this.metrics()).length + 1;

    this.detailsWrap = this.gWrap.append('g');

    this.detailsColorIndicator = this.detailsWrap.append('rect')
        .classed('details-color-indicator', true)
        .attr('transform', trans(-1, 0))
        .attr('x', 0)
        .attr('y', 0)
        .attr('width', 3)
        .attr('height', 2 * this._lineHeight)
        .style('opacity', 1);


    // Update widget
    this.update(containerS);

    return this;
  };



  window.SonarWidgets.Histogram.prototype.update = function(container) {
    container = d3.select(container);

    var widget = this,
        width = container.property('offsetWidth');
    this.width(width > 100 ? width : 100);


    // Update svg canvas
    this.svg
        .attr('width', this.width())
        .attr('height', this.height());


    // Update available size
    var detailsHeight = 3 * this._lineHeight;
    this.availableWidth = this.width() - this.margin().left - this.margin().right - this.axisWidth();
    this.availableHeight = this.height() - this.margin().top - this.margin().bottom - detailsHeight;
    var minHeight = this.availableHeight / 6;


    // Update plot
    this.plotWrap
        .attr('transform', trans(this.axisWidth(), detailsHeight));


    // Update scales
    this.x
        .domain(this.components().map(function(d, i) { return i; }))
        .rangeRoundBands([0, this.availableWidth], 0);

    var yDomain = d3.extent(this.components(), function(d) {
      return widget.getMainMetric(d);
    });
    this.y
        .domain(yDomain)
        .range([minHeight, this.availableHeight])
        .nice();

    if (this.components().length < 11) {
      this.color = d3.scale.category10();
    } else if (this.components().length < 21) {
      this.color = d3.scale.category20();
    }


    // Update axis
    this.y1
        .domain(yDomain)
        .range([this.availableHeight, minHeight])
        .nice();

    this.gAxis
        .attr('transform', trans(this.axisWidth(), detailsHeight - minHeight))
        .call(this.axis);


    // Update details
    this.detailsWrap
        .transition()
        .attr('transform', trans(this.axisWidth() + widget.x(0) + 3, 0));


    // Configure bars
    this.bars = this.plotWrap.selectAll('.bar')
        .data(this.components());

    this.bars
        .enter()
        .append('rect')
        .classed('bar', true)
        .style('fill', function(d, i) { return widget.color(i); })
        .style('stroke', '#fff');

    this.bars
        .transition()
        .attr('x', function(d, i) { return widget.x(i); })
        .attr('y', function(d) { return widget.availableHeight - widget.y(widget.getMainMetric(d)); })
        .attr('width', this.x.rangeBand())
        .attr('height', function(d) { return  widget.y(widget.getMainMetric(d)); });

    this.bars
        .exit().remove();


    // Configure events
    var enterHandler = function(bar, d, i) {
          var metrics = widget.metricsPriority().map(function(m) {
            return {
              name: widget.metrics()[m].name,
              value: widget.fm(d.measures[m].val, m)
            };
          });
          metrics.unshift({ name: d.name });
          updateMetrics(metrics);

          widget.detailsColorIndicator
              .style('opacity', 1)
              .style('fill', widget.color(i));
        },

        leaveHandler = function() {
          widget.detailsColorIndicator
              .style('opacity', 0);
          widget.detailsMetrics
              .style('opacity', 0);
        },

        updateMetrics = function(metrics) {
          widget.detailsMetrics = widget.detailsWrap.selectAll('.details-metric')
              .data(metrics);

          widget.detailsMetrics.enter().append('text')
              .classed('details-metric', true)
              .classed('details-metric-main', function(d, i) { return i === 0; })
              .attr('transform', function(d, i) { return trans(10, i * widget._lineHeight); })
              .attr('dy', '1.2em');

          widget.detailsMetrics
              .text(function(d) { return d.name + (d.value ? ': ' + d.value : ''); })
              .style('opacity', 1);

          widget.detailsMetrics.exit().remove();
        };

    this.bars
        .on('mouseenter', function(d, i) {
          return enterHandler(this, d, i);
        })
        .on('mouseleave', leaveHandler)
        .on('click', function(d) {
          switch (d.qualifier) {
            case 'CLA':
            case 'FIL':
              window.location = widget.options().baseUrl + encodeURIComponent(d.key) +
                  '?metric=' + encodeURIComponent(widget.mainMetric);
              break;
            default:
              window.location = widget.options().baseUrl + encodeURIComponent(d.key);
          }
        });
  };



  window.SonarWidgets.Histogram.defaults = {
    width: 350,
    height: 300,
    margin: { top: 10, right: 10, bottom: 10, left: 10 },
    axisWidth: 40
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
