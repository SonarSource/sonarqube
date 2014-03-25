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
    this._maxResultsReached = false;
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

    this.maxResultsReached = function (_) {
      return param.call(this, '_maxResultsReached', _);
    };

    this.options = function (_) {
      return param.call(this, '_options', _);
    };
  };

  window.SonarWidgets.Histogram.prototype.render = function (container) {
    var widget = this,
        containerS = container;

    container = d3.select(container);

    var validData = this.components().reduce(function(p, c) {
      return p && !!c.measures[widget.metricsPriority()[0]]
    }, true);

    if (!validData) {
      container.text(this.options().noMainMetric);
      return;
    }


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


    // Configure scales
    this.x = d3.scale.linear();
    this.y = d3.scale.ordinal();


    // Configure truncate function
    this.truncate = function(text, type) {
      var maxLength = 40;

      switch (type) {
        case 'FIL':
        case 'CLA':
          var n = text.length;
          if (n > maxLength) {
            var shortText = text.substr(n - maxLength + 2, n - 1),
                dotIndex = shortText.indexOf('.');
            return '...' + shortText.substr(dotIndex + 1);
          } else {
            return text;
          }
          break;
        default:
          return text.length > maxLength ?
              text.substr(0, maxLength - 3) + '...' :
              text;
      }
    };


    // Configure metric label
    this.metricLabel = this.gWrap.append('text')
        .text(this.metrics()[this.mainMetric].name)
        .attr('dy', '9px')
        .style('font-size', '12px');


    // Show maxResultsReached message
    if (this.maxResultsReached()) {
      this.maxResultsReachedLabel = this.gWrap.append('text')
          .classed('max-results-reached', true)
          .style('font-size', '12px')
          .style('fill', '#777')
          .text(this.options().maxItemsReachedMessage);
    }


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
    this.availableHeight = barHeight * this.components().length + this._lineHeight;
    var totalHeight = this.availableHeight + this.margin().top + this.margin().bottom;
    if (this.maxResultsReached()) {
      totalHeight += this._lineHeight;
    }
    this.height(totalHeight);


    // Update svg canvas
    this.svg
        .attr('width', this.width())
        .attr('height', this.height());


    // Update plot
    this.plotWrap
        .attr('transform', trans(0, this._lineHeight));


    // Update scales
    var xDomain = d3.extent(this.components(), function(d) {
          return widget.getMainMetric(d);
        }),
        metric = this.metrics()[this.mainMetric];

    if (!this.options().relativeScale) {
      if (this.metrics()[this.mainMetric].type === 'PERCENT') {
        xDomain = [0, 100];
      } else {
        xDomain[0] = 0;
      }
    }

    this.x
        .domain(xDomain)
        .range([0, this.availableWidth]);

    this.y
        .domain(this.components().map(function(d, i) { return i; }))
        .rangeRoundBands([0, this.availableHeight], 0);


    // Configure bars
    this.bars = this.plotWrap.selectAll('.bar')
        .data(this.components());

    this.barsEnter = this.bars.enter()
        .append('g')
        .classed('bar', true)
        .attr('transform', function(d, i) { return trans(0, i * barHeight); });

    this.barsEnter
        .append('rect')
        .style('fill', '#1f77b4');

    this.barsEnter
        .append('text')
        .classed('legend-text component', true)
        .style('text-anchor', 'end')
        .attr('dy', '-0.35em')
        .text(function(d) { return widget.truncate(d.longName, d.qualifier); })
        .attr('transform', function() { return trans(widget.legendWidth() - 10, barHeight); });

    this.barsEnter
        .append('text')
        .classed('legend-text value', true)
        .attr('dy', '-0.35em')
        .text(function(d) { return d.measures[widget.mainMetric].fval; })
        .attr('transform', function(d) { return trans(widget.legendWidth() + widget.x(widget.getMainMetric(d)) + 5, barHeight); });

    this.bars.selectAll('rect')
        .transition()
        .attr('x', this.legendWidth())
        .attr('y', 0)
        .attr('width', function(d) { return Math.max(2, widget.x(widget.getMainMetric(d))); })
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


    // Configure metric label
    this.metricLabel
        .attr('transform', trans(this.legendWidth(), 0));


    // Show maxResultsReached message
    if (this.maxResultsReached()) {
      this.maxResultsReachedLabel
          .attr('transform', trans(this.legendWidth(), this.height() - this.margin().bottom - 3));
    }
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
