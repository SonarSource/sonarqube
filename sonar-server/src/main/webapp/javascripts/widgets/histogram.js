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
    this._legendWidth = window.SonarWidgets.Histogram.defaults.legendWidth;
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

    this.legendWidth = function (_) {
      return param.call(this, '_legendWidth', _);
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
    this.x = d3.scale.linear();
    this.y = d3.scale.ordinal();
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


    // Update widget
    this.update(containerS);

    return this;
  };



  window.SonarWidgets.Histogram.prototype.update = function(container) {
    container = d3.select(container);

    var widget = this,
        barHeight = 16,
        width = container.property('offsetWidth');
    this.width(width > 100 ? width : 100);


    // Update available size
    this.availableWidth = this.width() - this.margin().left - this.margin().right - this.legendWidth();
    this.availableHeight = barHeight * this.components().length;
    this.height(this.availableHeight + this.margin().top + this.margin().bottom);


    // Update svg canvas
    this.svg
        .attr('width', this.width())
        .attr('height', this.height());


    // Update scales
    var xDomain = d3.extent(this.components(), function(d) {
      return widget.getMainMetric(d);
    });
    this.x
        .domain(xDomain)
        .range([this.availableWidth / 8, this.availableWidth]);

    this.y
        .domain(this.components().map(function(d, i) { return i; }))
        .rangeRoundBands([0, this.availableHeight], 0);

    if (this.components().length < 11) {
      this.color = d3.scale.category10();
    } else if (this.components().length < 21) {
      this.color = d3.scale.category20();
    }


    // Configure bars
    this.bars = this.plotWrap.selectAll('.bar')
        .data(this.components());

    this.barsEnter = this.bars.enter()
        .append('g')
        .classed('bar', true)
        .attr('transform', function(d, i) { return trans(0, i * barHeight); });

    this.barsEnter
        .append('rect')
        .style('fill', function(d, i) { return widget.color(i); })
        .style('stroke', '#fff');

    this.barsEnter
        .append('text')
        .classed('legend-text component', true)
        .style('text-anchor', 'end')
        .attr('dy', '-0.35em')
        .text(function(d) {
          var l = d.name.length;
          return l > 42 ? d.name.substr(0, 39) + '...' : d.name;
        })
        .attr('transform', function() { return trans(widget.legendWidth() - 10, barHeight); });

    this.barsEnter
        .append('text')
        .classed('legend-text value', true)
        .attr('dy', '-0.35em')
        .text(function(d) { return widget.fm(widget.getMainMetric(d), widget.mainMetric); })
        .attr('transform', function(d) { return trans(widget.legendWidth() + widget.x(widget.getMainMetric(d)) + 5, barHeight); });

    this.bars.selectAll('rect')
        .transition()
        .attr('x', this.legendWidth())
        .attr('y', 0)
        .attr('width', function(d) { return widget.x(widget.getMainMetric(d)); })
        .attr('height', barHeight);

    this.bars.selectAll('.component')
        .transition()
        .attr('transform', function() { return trans(widget.legendWidth() - 10, barHeight); });

    this.bars.selectAll('.value')
        .transition()
        .attr('transform', function(d) { return trans(widget.legendWidth() + widget.x(widget.getMainMetric(d)) + 5, barHeight); });

    this.bars
        .exit().remove();

    this.bars
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
    margin: { top: 4, right: 50, bottom: 4, left: 10 },
    legendWidth: 220
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
