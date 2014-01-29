/*global d3:false, baseUrl:false */
/*jshint eqnull:true */

window.SonarWidgets = window.SonarWidgets == null ? {} : window.SonarWidgets;

(function () {

  window.SonarWidgets.PieChart = function () {
    // Set default values
    this._components = [];
    this._metrics = [];
    this._metricsPriority = [];
    this._width = window.SonarWidgets.PieChart.defaults.width;
    this._height = window.SonarWidgets.PieChart.defaults.height;
    this._margin = window.SonarWidgets.PieChart.defaults.margin;
    this._legendWidth = window.SonarWidgets.PieChart.defaults.legendWidth;
    this._legendMargin = window.SonarWidgets.PieChart.defaults.legendMargin;
    this._detailsWidth = window.SonarWidgets.PieChart.defaults.detailsWidth;
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

    this.legendMargin = function (_) {
      return param.call(this, '_legendMargin', _);
    };

    this.detailsWidth = function (_) {
      return param.call(this, '_detailsWidth', _);
    };

    this.options = function (_) {
      return param.call(this, '_options', _);
    };
  };

  window.SonarWidgets.PieChart.prototype.render = function (container) {
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
    this.color = d3.scale.category10();


    // Configure arc
    this.arc = d3.svg.arc()
        .innerRadius(0);


    // Configure pie
    this.pie = d3.layout.pie()
        .sort(null)
        .value(function(d) { return widget.getMainMetric(d); });


    // Configure legend
    this.legendWrap = this.gWrap.append('g');


    // Configure details
    this._metricsCount = Object.keys(this.metrics()).length + 1;
    this._detailsHeight = this._lineHeight * this._metricsCount;

    this.detailsWrap = this.gWrap.append('g')
        .attr('width', this.legendWidth())
        .style('display', 'none');

    this.detailsColorIndicator = this.detailsWrap.append('rect')
        .classed('details-color-indicator', true)
        .attr('transform', trans(-1, 0))
        .attr('x', 0)
        .attr('y', 0)
        .attr('width', 3)
        .attr('height', this._detailsHeight)
        .style('opacity', 0);

    this.donutLabel = this.plotWrap.append('text')
        .style('text-anchor', 'middle')
        .style('opacity', 0);

    this.donutLabel2 = this.plotWrap.append('text')
        .style('text-anchor', 'middle')
        .text(this.metrics()[this.mainMetric].name);


    // Update widget
    this.update(containerS);

    return this;
  };



  window.SonarWidgets.PieChart.prototype.update = function(container) {
    container = d3.select(container);

    var widget = this,
        width = container.property('offsetWidth');
    this.width(width > 100 ? width : 100);


    // Update svg canvas
    this.svg
        .attr('width', this.width())
        .attr('height', this.height());


    // Update available size
    this.availableWidth = this.width() - this.margin().left - this.margin().right -
        this.legendWidth() - this.legendMargin();
    this.availableHeight = this.height() - this.margin().top - this.margin().bottom;
    this.radius = Math.min(this.availableWidth, this.availableHeight) / 2;
    this._legendSize = Math.floor(this.availableHeight / this._lineHeight);
    this._legendSymbols = Math.floor((this.width() - this.margin().left - this.margin().right - this.legendMargin() - 2 * this.radius) / 6.2);


    // Update plot
    this.plotWrap
        .attr('transform', trans(this.radius, this.radius));


    // Update arc
    this.arc
        .innerRadius(this.radius / 2)
        .outerRadius(this.radius);


    // Configure sectors
    this.sectors = this.plotWrap.selectAll('.arc')
        .data(this.pie(this.components()));

    this.sectors
        .enter()
        .append('path')
        .classed('arc', true)
        .style('fill', function(d, i) { return widget.color(i); });

    this.sectors
        .transition()
        .attr('d', this.arc);

    this.sectors
        .exit().remove();


    // Update legend
    this.legendWrap
        .attr('transform', trans(
            this.legendMargin() + 2 * this.radius,
            (this.availableHeight - this._legendSize * this._lineHeight) / 2
        ));

    this.legends = this.legendWrap.selectAll('.legend')
        .data(this.components().slice(0, this._legendSize));

    this.legendsEnter = this.legends.enter()
        .append('g')
        .classed('legend pie-legend', true)
        .attr('transform', function(d, i) { return trans(0, 10 + i * widget._lineHeight); });

    this.legendsEnter.append('circle')
        .attr('class', 'legend-bullet')
        .attr('r', 4)
        .style('fill', function(d, i) { return widget.color(i); });

    this.legendsEnter.append('text')
        .attr('class', 'legend-text')
        .attr('dy', '0.1em')
        .attr('transform', trans(10, 3));

    this.legends.selectAll('text')
        .text(function(d) { return d.name.length > widget._legendSymbols ? d.name.substr(0, widget._legendSymbols) + '...' : d.name; });


    // Update details
    this.detailsWrap
        .attr('width', this.legendWidth())
        .attr('transform', trans(
            this.legendMargin() + 2 * this.radius, this.radius - this._detailsHeight / 2
        ));

    this.donutLabel
        .transition()
        .style('font-size', (this.radius / 6) + 'px');

    var fz = Math.min(
        12,
        this.radius / 10,
        1.5 * this.radius / this.metrics()[this.mainMetric].name.length
    );
    this.donutLabel2
        .attr('transform', trans(0, widget.radius / 6))
        .style('font-size', fz + 'px');


    // Configure events
    var enterHandler = function(sector, d, i, showDetails) {
          if (showDetails) {
            var metrics = widget.metricsPriority().map(function(m) {
              return {
                name: widget.metrics()[m].name,
                value: (!!d.measures[m] ? d.measures[m].fval : '–')
              };
            });
            metrics.unshift({ name: (d.name.length > widget._legendSymbols ? d.name.substr(0, widget._legendSymbols) + '...' : d.name) });
            updateMetrics(metrics);

            widget.legendWrap
                .style('opacity', 0);

            widget.detailsColorIndicator
                .style('opacity', 1)
                .style('fill', widget.color(i));

            widget.detailsWrap
                .style('display', 'block');
          }
          widget.donutLabel
              .style('opacity', 1)
              .text(d.measures[widget.mainMetric].fval);
          widget.donutLabel
              .style('opacity', 1);
          widget.plotWrap
              .classed('hover', true);
          sector.
              classed('hover', true);
        },

        leaveHandler = function(sector) {
          widget.legendWrap
              .style('opacity', 1);
          widget.detailsColorIndicator
              .style('opacity', 0);
          if (widget.detailsMetrics) {
            widget.detailsMetrics
                .style('opacity', 0);
          }
          widget.donutLabel
              .style('opacity', 0)
              .text('');
          widget.plotWrap
              .classed('hover', false);
          sector.
              classed('hover', false);
          widget.detailsWrap
              .style('display', 'none');
        },

        clickHandler = function(d) {
          switch (d.qualifier) {
            case 'CLA':
            case 'FIL':
              window.location = widget.options().baseUrl + encodeURIComponent(d.key) +
                  '?metric=' + encodeURIComponent(widget.mainMetric);
              break;
            default:
              window.location = widget.options().baseUrl + encodeURIComponent(d.key);
          }
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

    this.sectors
        .on('mouseenter', function(d, i) {
          return enterHandler(d3.select(this), d.data, i, true);
        })
        .on('mouseleave', function(d, i) {
          return leaveHandler(d3.select(this), d.data, i, true);
        })
        .on('click', function(d) {
          return clickHandler(d.data);
        });

    this.legends
        .on('mouseenter', function(d, i) {
          return enterHandler(d3.select(widget.sectors[0][i]), d, i, false);
        })
        .on('mouseleave', function(d, i) {
          return leaveHandler(d3.select(widget.sectors[0][i]), d, i, false);
        })
        .on('click', function(d) {
          return clickHandler(d);
        });
  };



  window.SonarWidgets.PieChart.defaults = {
    width: 350,
    height: 300,
    margin: { top: 10, right: 10, bottom: 10, left: 10 },
    legendWidth: 160,
    legendMargin: 30
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
