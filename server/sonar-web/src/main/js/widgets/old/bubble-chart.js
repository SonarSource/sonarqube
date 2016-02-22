/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import $ from 'jquery';
import { collapsedDirFromPath, fileFromPath } from '../../helpers/path';

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


  window.SonarWidgets.BubbleChart.prototype.hasValidData = function () {
    const widget = this;
    let noInvalidEntry = true;
    let atLeastOneValueOnX = false;
    let atLeastOneValueOnY = false;
    this.components().forEach(function (component) {
      noInvalidEntry = noInvalidEntry &&
          !!component.measures[widget.metricsPriority()[0]] &&
          !!component.measures[widget.metricsPriority()[1]];
      atLeastOneValueOnX = atLeastOneValueOnX ||
          (component.measures[widget.metricsPriority()[0]] || {}).fval !== '-';
      atLeastOneValueOnY = atLeastOneValueOnY ||
          (component.measures[widget.metricsPriority()[1]] || {}).fval !== '-';
    });
    return !!noInvalidEntry && !!atLeastOneValueOnX && !!atLeastOneValueOnY;
  };


  window.SonarWidgets.BubbleChart.prototype.init = function (container) {
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
  };


  window.SonarWidgets.BubbleChart.prototype.initMetrics = function () {
    const widget = this;

    this.xMetric = this.metricsPriority()[0];
    this.getXMetric = function (d) {
      return d.measures[widget.xMetric].val;
    };

    this.yMetric = this.metricsPriority()[1];
    this.getYMetric = function (d) {
      return d.measures[widget.yMetric].val;
    };

    this.sizeMetric = this.metricsPriority()[2];
    this.getSizeMetric = function (d) {
      return d.measures[widget.sizeMetric] ? d.measures[widget.sizeMetric].val : 0;
    };
  };


  window.SonarWidgets.BubbleChart.prototype.initScales = function () {
    const widget = this;
    this
        .xLog(this.options().xLog)
        .yLog(this.options().yLog);

    this.x = this.xLog() ? d3.scale.log() : d3.scale.linear();
    this.y = this.yLog() ? d3.scale.log() : d3.scale.linear();
    this.size = d3.scale.linear();

    this.x.range([0, this.availableWidth]);
    this.y.range([this.availableHeight, 0]);
    this.size.range([5, 45]);

    if (this.components().length > 1) {
      this.x.domain(d3.extent(this.components(), function (d) {
        return widget.getXMetric(d);
      }));
      this.y.domain(d3.extent(this.components(), function (d) {
        return widget.getYMetric(d);
      }));
      this.size.domain(d3.extent(this.components(), function (d) {
        return widget.getSizeMetric(d);
      }));
    } else {
      const singleComponent = this.components()[0];
      const xm = this.getXMetric(singleComponent);
      const ym = this.getYMetric(singleComponent);
      const sm = this.getSizeMetric(singleComponent);
      this.x.domain([xm * 0.8, xm * 1.2]);
      this.y.domain([ym * 0.8, ym * 1.2]);
      this.size.domain([sm * 0.8, sm * 1.2]);
    }
  };


  window.SonarWidgets.BubbleChart.prototype.initBubbles = function () {
    const widget = this;

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
        .style('fill', function () {
          return widget.bubbleColor();
        })
        .style('fill-opacity', 0.2)
        .style('stroke', function () {
          return widget.bubbleColor();
        })
        .style('transition', 'all 0.2s ease')

        .attr('title', function (d) {
          const xMetricName = widget.metrics()[widget.xMetric].name;
          const yMetricName = widget.metrics()[widget.yMetric].name;
          const sizeMetricName = widget.metrics()[widget.sizeMetric].name;
          const xMetricValue = d.measures[widget.xMetric].fval;
          const yMetricValue = d.measures[widget.yMetric].fval;
          const sizeMetricValue = d.measures[widget.sizeMetric].fval;

          return '<div class="text-left">' +
              collapsedDirFromPath(d.longName) + '<br>' +
              fileFromPath(d.longName) + '<br>' + '<br>' +
              xMetricName + ': ' + xMetricValue + '<br>' +
              yMetricName + ': ' + yMetricValue + '<br>' +
              sizeMetricName + ': ' + sizeMetricValue +
              '</div>';
        })
        .attr('data-placement', 'bottom')
        .attr('data-toggle', 'tooltip');

    this.items.exit().remove();

    this.items.sort(function (a, b) {
      return widget.getSizeMetric(b) - widget.getSizeMetric(a);
    });
  };


  window.SonarWidgets.BubbleChart.prototype.initBubbleEvents = function () {
    const widget = this;
    this.items
        .on('click', function (d) {
          window.location = widget.options().baseUrl + '?id=' + encodeURIComponent(d.key);
        })
        .on('mouseenter', function () {
          d3.select(this).select('circle')
              .style('fill-opacity', 0.8);
        })
        .on('mouseleave', function () {
          d3.select(this).select('circle')
              .style('fill-opacity', 0.2);
        });
  };


  window.SonarWidgets.BubbleChart.prototype.initAxes = function () {
    // X
    this.xAxis = d3.svg.axis()
        .scale(this.x)
        .orient('bottom');

    this.gxAxisLabel = this.gxAxis.append('text')
        .text(this.metrics()[this.xMetric].name)
        .style('font-weight', 'bold')
        .style('text-anchor', 'middle');


    // Y
    this.yAxis = d3.svg.axis()
        .scale(this.y)
        .orient('left');

    this.gyAxis.attr('transform', trans(60 - this.margin().left, 0));

    this.gyAxisLabel = this.gyAxis.append('text')
        .text(this.metrics()[this.yMetric].name)
        .style('font-weight', 'bold')
        .style('text-anchor', 'middle');
  };


  window.SonarWidgets.BubbleChart.prototype.initGrid = function () {
    this.gxGridLines = this.gxGrid.selectAll('line').data(this.x.ticks()).enter()
        .append('line');

    this.gyGridLines = this.gyGrid.selectAll('line').data(this.y.ticks()).enter()
        .append('line');

    this.gGrid.selectAll('line')
        .style('stroke', '#000')
        .style('stroke-opacity', 0.25);
  };


  window.SonarWidgets.BubbleChart.prototype.render = function (container) {
    const containerS = container;

    container = d3.select(container);

    if (!this.hasValidData()) {
      container.text(this.options().noMainMetric);
      return;
    }

    this.init(container);
    this.initMetrics();
    this.initScales();
    this.initBubbles();
    this.initBubbleEvents();
    this.initAxes();
    this.initGrid();
    this.update(containerS);

    $('[data-toggle="tooltip"]').tooltip({ container: 'body', html: true });

    return this;
  };


  window.SonarWidgets.BubbleChart.prototype.adjustScalesAfterUpdate = function () {
    const widget = this;
    // X
    const minX = d3.min(this.components(), function (d) {
      return widget.x(widget.getXMetric(d)) - widget.size(widget.getSizeMetric(d));
    });
    const maxX = d3.max(this.components(), function (d) {
      return widget.x(widget.getXMetric(d)) + widget.size(widget.getSizeMetric(d));
    });
    const dMinX = minX < 0 ? this.x.range()[0] - minX : this.x.range()[0];
    const dMaxX = maxX > this.x.range()[1] ? maxX - this.x.range()[1] : 0;
    this.x.range([dMinX, this.availableWidth - dMaxX]);

    // Y
    const minY = d3.min(this.components(), function (d) {
      return widget.y(widget.getYMetric(d)) - widget.size(widget.getSizeMetric(d));
    });
    const maxY = d3.max(this.components(), function (d) {
      return widget.y(widget.getYMetric(d)) + widget.size(widget.getSizeMetric(d));
    });
    const dMinY = minY < 0 ? this.y.range()[1] - minY : this.y.range()[1];
    const dMaxY = maxY > this.y.range()[0] ? maxY - this.y.range()[0] : 0;
    this.y.range([this.availableHeight - dMaxY, dMinY]);


    // Format improvement for log scales
    // X
    if (this.xLog()) {
      this.xAxis.tickFormat(function (d) {
        const ticksCount = widget.availableWidth / 50;
        return widget.x.tickFormat(ticksCount, d3.format(',d'))(d);
      });
    }

    // Y
    if (this.yLog()) {
      this.yAxis.tickFormat(function (d) {
        const ticksCount = widget.availableHeight / 30;
        return widget.y.tickFormat(ticksCount, d3.format(',d'))(d);
      });
    }

    // Make scale's domains nice
    this.x.nice();
    this.y.nice();
  };


  window.SonarWidgets.BubbleChart.prototype.updateScales = function () {
    const widget = this;
    this.x.range([0, this.availableWidth]);
    this.y.range([this.availableHeight, 0]);

    if (this.components().length > 1) {
      this.x.domain(d3.extent(this.components(), function (d) {
        return widget.getXMetric(d);
      }));
      this.y.domain(d3.extent(this.components(), function (d) {
        return widget.getYMetric(d);
      }));
    } else {
      const singleComponent = this.components()[0];
      const xm = this.getXMetric(singleComponent);
      const ym = this.getYMetric(singleComponent);
      const sm = this.getSizeMetric(singleComponent);
      this.x.domain([xm * 0.8, xm * 1.2]);
      this.y.domain([ym * 0.8, ym * 1.2]);
      this.size.domain([sm * 0.8, sm * 1.2]);
    }

    if (this.x.domain()[0] === 0 && this.x.domain()[1] === 0) {
      this.x.domain([0, 1]);
    }
    if (this.y.domain()[0] === 0 && this.y.domain()[1] === 0) {
      this.y.domain([0, 1]);
    }

    // Avoid zero values when using log scale
    if (this.xLog) {
      const xDomain = this.x.domain();
      this.x
          .domain([xDomain[0] > 0 ? xDomain[0] : 0.1, xDomain[1]])
          .clamp(true);
    }

    if (this.yLog) {
      const yDomain = this.y.domain();
      this.y
          .domain([yDomain[0] > 0 ? yDomain[0] : 0.1, yDomain[1]])
          .clamp(true);
    }
  };


  window.SonarWidgets.BubbleChart.prototype.updateBubbles = function () {
    const widget = this;
    this.items
        .transition()
        .attr('transform', function (d) {
          return trans(widget.x(widget.getXMetric(d)), widget.y(widget.getYMetric(d)));
        });
  };


  window.SonarWidgets.BubbleChart.prototype.updateAxes = function () {
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
  };


  window.SonarWidgets.BubbleChart.prototype.updateGrid = function () {
    const widget = this;
    this.gxGridLines
        .transition()
        .attr({
          x1 (d) {
            return widget.x(d);
          },
          x2 (d) {
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
          y1 (d) {
            return widget.y(d);
          },
          y2 (d) {
            return widget.y(d);
          }
        });
  };


  window.SonarWidgets.BubbleChart.prototype.update = function (container) {
    container = d3.select(container);

    const width = container.property('offsetWidth');

    this.width(width > 100 ? width : 100);

    // Update svg canvas
    this.svg
        .attr('width', this.width())
        .attr('height', this.height());

    // Update available size
    this.availableWidth = this.width() - this.margin().left - this.margin().right;
    this.availableHeight = this.height() - this.margin().top - this.margin().bottom;

    this.updateScales();
    this.adjustScalesAfterUpdate();
    this.updateBubbles();
    this.updateAxes();
    this.updateGrid();
  };


  window.SonarWidgets.BubbleChart.defaults = {
    width: 350,
    height: 150,
    margin: { top: 10, right: 10, bottom: 50, left: 70 },
    xLog: false,
    yLog: false,
    bubbleColor: '#4b9fd5',
    bubbleColorUndefined: '#b3b3b3'
  };


  // Some helper functions

  // Gets or sets parameter
  function param (name, value) {
    if (value == null) {
      return this[name];
    } else {
      this[name] = value;
      return this;
    }
  }

  // Helper for create the translate(x, y) string
  function trans (left, top) {
    return 'translate(' + left + ', ' + top + ')';
  }

})();
