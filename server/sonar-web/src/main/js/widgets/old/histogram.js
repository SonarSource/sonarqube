(function () {

  function Histogram () {
    window.SonarWidgets.BaseWidget.apply(this, arguments);
    this.addField('width', 0);
    this.addField('height', window.SonarWidgets.Histogram.defaults.height);
    this.addField('margin', window.SonarWidgets.Histogram.defaults.margin);
    this.addField('legendWidth', window.SonarWidgets.Histogram.defaults.legendWidth);
    this.addField('maxResultsReached', false);
  }

  Histogram.prototype = new window.SonarWidgets.BaseWidget();

  Histogram.prototype.barHeight = 16;

  Histogram.prototype.barFill = '#1f77b4';

  Histogram.prototype.isDataValid = function () {
    var that = this;
    return this.components().reduce(function (p, c) {
      return p && !!c.measures[that.mainMetric.key];
    }, true);
  };

  Histogram.prototype.truncate = function (text, type) {
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
        if (text.length > maxLength) {
          return text.substr(0, maxLength - 3) + '...';
        } else {
          return text;
        }
    }
  };

  Histogram.prototype.render = function (container) {
    var box = d3.select(container);
    this.addMetric('mainMetric', 0);
    if (!this.isDataValid()) {
      box.text(this.options().noMainMetric);
      return;
    }
    this.width(box.property('offsetWidth'));
    this.svg = box.append('svg').classed('sonar-d3', true);
    this.gWrap = this.svg.append('g');
    this.gWrap.attr('transform', this.trans(this.margin().left, this.margin().top));
    this.plotWrap = this.gWrap.append('g').classed('plot', true);
    this.x = d3.scale.linear();
    this.y = d3.scale.ordinal();
    this.metricLabel = this.gWrap.append('text').text(this.mainMetric.name);
    this.metricLabel.attr('dy', '9px').style('font-size', '12px');
    if (this.maxResultsReached()) {
      this.maxResultsReachedLabel = this.gWrap.append('text').classed('max-results-reached-message', true);
      this.maxResultsReachedLabel.text(this.options().maxItemsReachedMessage);
    }
    return window.SonarWidgets.BaseWidget.prototype.render.apply(this, arguments);
  };

  Histogram.prototype.update = function (container) {
    var that = this;
    var box = d3.select(container);
    this.width(box.property('offsetWidth'));
    var availableWidth = this.width() - this.margin().left - this.margin().right - this.legendWidth(),
        availableHeight = this.barHeight * this.components().length + this.lineHeight,
        totalHeight = availableHeight + this.margin().top + this.margin().bottom;
    if (this.maxResultsReached()) {
      totalHeight += this.lineHeight;
    }
    this.height(totalHeight);
    this.svg.attr('width', this.width()).attr('height', this.height());
    this.plotWrap.attr('transform', this.trans(0, this.lineHeight));
    var xDomain = d3.extent(this.components(), function (d) {
      return that.mainMetric.value(d);
    });
    if (!this.options().relativeScale) {
      if (this.mainMetric.type === 'PERCENT') {
        xDomain = [0, 100];
      } else {
        xDomain[0] = 0;
      }
    }
    this.x.domain(xDomain).range([0, availableWidth]);
    this.y.domain(this.components().map(function (d, i) {
      return i;
    })).rangeRoundBands([0, availableHeight], 0);
    this.bars = this.plotWrap.selectAll('.bar').data(this.components());
    this.barsEnter = this.bars.enter().append('g').classed('bar', true).attr('transform', function (d, i) {
      return that.trans(0, i * that.barHeight);
    });
    this.barsEnter.append('rect').style('fill', this.barFill);
    this.barsEnter.append('text')
        .classed('legend-text component', true)
        .style('text-anchor', 'end')
        .attr('dy', '-0.35em')
        .text(function (d) {
          return that.truncate(d.longName, d.qualifier);
        })
        .attr('transform', function () {
          return that.trans(that.legendWidth() - 10, that.barHeight);
        });
    this.barsEnter.append('text')
        .classed('legend-text value', true)
        .attr('dy', '-0.35em')
        .text(function (d) {
          return that.mainMetric.formattedValue(d);
        })
        .attr('transform', function (d) {
          return that.trans(that.legendWidth() + that.x(that.mainMetric.value(d)) + 5, that.barHeight);
        });
    this.bars.selectAll('rect')
        .transition()
        .attr('x', this.legendWidth())
        .attr('y', 0)
        .attr('width', function (d) {
          return Math.max(2, that.x(that.mainMetric.value(d)));
        })
        .attr('height', this.barHeight);
    this.bars.selectAll('.component')
        .transition()
        .attr('transform', function () {
          return that.trans(that.legendWidth() - 10, that.barHeight);
        });
    this.bars.selectAll('.value')
        .transition()
        .attr('transform', function (d) {
          return that.trans(that.legendWidth() + that.x(that.mainMetric.value(d)) + 5, that.barHeight);
        });
    this.bars.exit().remove();
    this.bars.on('click', function (d) {
      window.location = that.options().baseUrl + '?id=' + encodeURIComponent(d.key);
    });
    this.metricLabel.attr('transform', this.trans(this.legendWidth(), 0));
    if (this.maxResultsReached()) {
      this.maxResultsReachedLabel.attr('transform',
          this.trans(this.legendWidth(), this.height() - this.margin().bottom - 3));
    }
    return window.SonarWidgets.BaseWidget.prototype.update.apply(this, arguments);
  };

  window.SonarWidgets.Histogram = Histogram;
  window.SonarWidgets.Histogram.defaults = {
    height: 300,
    margin: {
      top: 4,
      right: 50,
      bottom: 4,
      left: 10
    },
    legendWidth: 220
  };

})();
