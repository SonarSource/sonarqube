import $ from 'jquery';
import _ from 'underscore';

(function () {

  function Treemap () {
    this.addField('width', null);
    this.addField('height', null);
    this.addField('maxResultsReached', false);
    window.SonarWidgets.BaseWidget.apply(this, arguments);
  }

  Treemap.prototype = new window.SonarWidgets.BaseWidget();

  Treemap.prototype.sizeLow = 11;

  Treemap.prototype.sizeHigh = 18;

  Treemap.prototype.filterComponents = function () {
    var that = this,
        components = this.components().filter(function (d) {
          return that.sizeMetric.value(d) != null;
        });
    this.components(components);
  };

  Treemap.prototype.getNodes = function () {
    return this.treemap
        .nodes({ children: this.components() })
        .filter(function (d) {
          return !d.children;
        });
  };

  Treemap.prototype.renderTreemap = function () {
    var that = this;
    this.filterComponents();
    if (!this.components().length) {
      this.maxResultsReachedLabel
          .text(window.t('treemap.all_measures_undefined'))
          .style('display', 'block');
      return;
    }
    var nodes = this.getNodes();
    this.color = that.getColorScale();
    this.cells = this.box.selectAll('.treemap-cell').data(nodes);
    this.cells.exit().remove();
    var cellsEnter = this.cells.enter().append('div');
    cellsEnter.classed('treemap-cell', true);
    cellsEnter.append('div').classed('treemap-inner', true);
    cellsEnter.append('a').classed('treemap-link', true);
    this.cells.attr('title', function (d) {
      return that.tooltip(d);
    });
    this.cells.style('background-color', function (d) {
      if (that.colorMetric.value(d) != null) {
        return that.color(that.colorMetric.value(d));
      } else {
        return that.colorUnknown;
      }
    });
    this.cells.classed('treemap-cell-drilldown', function (d) {
      return (d.qualifier != null) && d.qualifier !== 'FIL' && d.qualifier !== 'CLA';
    });
    var prefix = this.mostCommonPrefix(_.pluck(this.components(), 'longName')),
        prefixLength = prefix.length;
    this.cellsInner = this.box.selectAll('.treemap-inner').data(nodes);
    this.cellsInner.html(function (d) {
      if (prefixLength > 0) {
        return prefix + '<br>' + (d.longName.substr(prefixLength));
      } else {
        return d.longName;
      }
    });
    this.cellsLink = this.box.selectAll('.treemap-link').data(nodes);
    this.cellsLink.html('<i class="icon-link"></i>');
    this.cellsLink.attr('href', function (d) {
      return that.options().baseUrl + '?id=' + encodeURIComponent(d.key);
    });
    this.attachEvents(cellsEnter);
    return this.maxResultsReachedLabel.style('display', this.maxResultsReached() ? 'block' : 'none');
  };

  Treemap.prototype.updateTreemap = function (components, maxResultsReached) {
    this.components(components);
    this.maxResultsReached(maxResultsReached);
    this.renderTreemap();
    return this.positionCells();
  };

  Treemap.prototype.attachEvents = function (cells) {
    var that = this;
    return cells.on('click', function (d) {
      return that.requestChildren(d);
    });
  };

  Treemap.prototype.positionCells = function () {
    var that = this;
    this.cells.style('left', function (d) {
      return d.x + 'px';
    });
    this.cells.style('top', function (d) {
      return d.y + 'px';
    });
    this.cells.style('width', function (d) {
      return d.dx + 'px';
    });
    this.cellsInner.style('max-width', function (d) {
      return d.dx + 'px';
    });
    this.cells.style('height', function (d) {
      return d.dy + 'px';
    });
    this.cells.style('line-height', function (d) {
      return d.dy + 'px';
    });
    this.cells.style('font-size', function (d) {
      return (that.size(d.dx / d.longName.length)) + 'px';
    });
    this.cellsLink.style('font-size', function (d) {
      return (that.sizeLink(Math.min(d.dx, d.dy))) + 'px';
    });
    this.cells.classed('treemap-cell-small', function (d) {
      return (d.dx / d.longName.length) < 1 || d.dy < 40;
    });
    return this.cells.classed('treemap-cell-very-small', function (d) {
      return d.dx < 24 || d.dy < 24;
    });
  };

  Treemap.prototype.renderLegend = function (box) {
    this.legend = box.insert('div', ':first-child');
    this.legend.classed('legend', true);
    this.legend.classed('legend-html', true);
    this.legend.append('span')
        .classed('legend-text', true)
        .html('Size: <span class="legend-text-main">' + this.sizeMetric.name + '</span>');
    return this.legend.append('span')
        .classed('legend-text', true)
        .html('Color: <span class="legend-text-main">' + this.colorMetric.name + '</span>');
  };

  Treemap.prototype.renderBreadcrumbs = function (box) {
    this.breadcrumbsBox = box.append('div').classed('treemap-breadcrumbs', true);
    this.breadcrumbs = [];
    var d = {
      name: '<i class="icon-home"></i>',
      components: this.components(),
      maxResultsReached: this.maxResultsReached()
    };
    return this.addToBreadcrumbs(d);
  };

  Treemap.prototype.updateBreadcrumbs = function () {
    var that = this;
    var breadcrumbs = this.breadcrumbsBox.selectAll('.treemap-breadcrumbs-item').data(this.breadcrumbs);
    breadcrumbs.exit().remove();
    var breadcrumbsEnter = breadcrumbs.enter().append('span').classed('treemap-breadcrumbs-item', true);
    breadcrumbsEnter.attr('title', function (d) {
      if (d.longName != null) {
        return d.longName;
      } else {
        return that.options().resource;
      }
    });
    breadcrumbsEnter.append('i')
        .classed('icon-chevron-right', true)
        .style('display', function (d, i) {
          if (i > 0) {
            return 'inline';
          } else {
            return 'none';
          }
        });
    breadcrumbsEnter.append('i').attr('class', function (d) {
      if (d.qualifier != null) {
        return 'icon-qualifier-' + (d.qualifier.toLowerCase());
      } else {
        return '';
      }
    });
    var breadcrumbsEnterLinks = breadcrumbsEnter.append('a');
    breadcrumbsEnterLinks.html(function (d) {
      return d.name;
    });
    breadcrumbsEnterLinks.on('click', function (d) {
      that.updateTreemap(d.components, d.maxResultsReached);
      return that.cutBreadcrumbs(d);
    });
    this.breadcrumbsBox.style('display', this.breadcrumbs.length < 2 ? 'none' : 'block');
  };

  Treemap.prototype.addToBreadcrumbs = function (d) {
    this.breadcrumbs.push(d);
    this.updateBreadcrumbs();
  };

  Treemap.prototype.cutBreadcrumbs = function (lastElement) {
    var index = null;
    this.breadcrumbs.forEach(function (d, i) {
      if (d.key === lastElement.key) {
        index = i;
      }
    });
    if (index != null) {
      this.breadcrumbs = _.initial(this.breadcrumbs, this.breadcrumbs.length - index - 1);
      this.updateBreadcrumbs();
    }
  };

  Treemap.prototype.getColorScale = function () {
    if (this.colorMetric.type === 'LEVEL') {
      return this.getLevelColorScale();
    }
    if (this.colorMetric.type === 'RATING') {
      return this.getRatingColorScale();
    }
    return this.getPercentColorScale();
  };

  Treemap.prototype.getPercentColorScale = function () {
    var color = d3.scale.linear().domain([0, 25, 50, 75, 100]);
    color.range(this.colorMetric.direction === 1 ? this.colors5 : this.colors5r);
    return color;
  };

  Treemap.prototype.getRatingColorScale = function () {
    var domain = [1, 2, 3, 4, 5];
    if (this.components().length > 0) {
      var colorMetricSample = this.colorMetric.value(_.first(this.components()));
      if (typeof colorMetricSample === 'string') {
        domain = ['A', 'B', 'C', 'D', 'E'];
      }
    }
    return d3.scale.ordinal().domain(domain).range(this.colors5r);
  };

  Treemap.prototype.getLevelColorScale = function () {
    return d3.scale.ordinal().domain(['ERROR', 'WARN', 'OK', 'NONE']).range(this.colorsLevel);
  };

  Treemap.prototype.render = function (container) {
    var that = this;
    var box = d3.select(container).append('div');
    box.classed('sonar-d3', true);
    this.box = box.append('div').classed('treemap-container', true);
    this.addMetric('colorMetric', 0);
    this.addMetric('sizeMetric', 1);
    this.color = this.getColorScale();
    this.size = d3.scale.linear().domain([3, 15]).range([this.sizeLow, this.sizeHigh]).clamp(true);
    this.sizeLink = d3.scale.linear().domain([60, 100]).range([12, 14]).clamp(true);
    this.treemap = d3.layout.treemap();
    this.treemap.sort(function (a, b) {
      return a.value - b.value;
    });
    this.treemap.round(true);
    this.treemap.value(function (d) {
      return that.sizeMetric.value(d);
    });
    this.maxResultsReachedLabel = box.append('div').text(this.options().maxItemsReachedMessage);
    this.maxResultsReachedLabel.classed('max-results-reached-message', true);
    this.renderLegend(box);
    this.renderBreadcrumbs(box);
    this.renderTreemap();
    return window.SonarWidgets.BaseWidget.prototype.render.apply(this, arguments);
  };

  Treemap.prototype.update = function () {
    this.width(this.box.property('offsetWidth'));
    this.height(this.width() / 100.0 * this.options().heightInPercents);
    if (this.components().length) {
      this.box.style('height', (this.height()) + 'px');
      this.treemap.size([this.width(), this.height()]);
      this.cells.data(this.getNodes());
      this.positionCells();
    }
  };

  Treemap.prototype.formatComponents = function (data) {
    var that = this;
    var components = _.filter(data, function (component) {
      var hasSizeMetric = function () {
        return _.findWhere(component.msr, {
          key: that.sizeMetric.key
        });
      };
      return _.isArray(component.msr) && component.msr.length > 0 && hasSizeMetric();
    });
    if (_.isArray(components) && components.length > 0) {
      return components.map(function (component) {
        var measures = {};
        component.msr.forEach(function (measure) {
          measures[measure.key] = {
            val: measure.val,
            fval: measure.frmt_val,
            text: measure.text,
            data: measure.data
          };
        });
        return {
          key: component.copy != null ? component.copy : component.key,
          name: component.name,
          longName: component.lname,
          qualifier: component.qualifier,
          measures: measures
        };
      });
    }
  };

  Treemap.prototype.requestChildren = function (d) {
    var that = this;
    var metrics = this.metricsPriority().join(','),
        RESOURCES_URL = baseUrl + '/api/resources/index';
    return $.get(RESOURCES_URL, {
      resource: d.key,
      depth: 1,
      metrics: metrics
    }).done(function (r) {
      var components = that.formatComponents(r);
      if (components != null) {
        components = _.sortBy(components, function (component) {
          return -that.sizeMetric.value(component);
        });
        components = _.initial(components, components.length - that.options().maxItems - 1);
        that.updateTreemap(components, components.length > that.options().maxItems);
        return that.addToBreadcrumbs(_.extend(d, {
          components: components,
          maxResultsReached: that.maxResultsReached()
        }));
      }
    });
  };

  Treemap.prototype.mostCommonPrefix = function (strings) {
    var sortedStrings = strings.slice(0).sort(),
        firstString = sortedStrings[0],
        firstStringLength = firstString.length,
        lastString = sortedStrings[sortedStrings.length - 1],
        i = 0;
    while (i < firstStringLength && firstString.charAt(i) === lastString.charAt(i)) {
      i++;
    }
    var prefix = firstString.substr(0, i),
        lastPrefixPart = _.last(prefix.split(/[\s\\\/]/));
    return prefix.substr(0, prefix.length - lastPrefixPart.length);
  };

  window.SonarWidgets.Treemap = Treemap;

})();
