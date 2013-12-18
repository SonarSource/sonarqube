/*global d3:false, _:false */
/*jshint eqnull:true */

window.SonarWidgets = window.SonarWidgets == null ? {} : window.SonarWidgets;

(function () {

  window.SonarWidgets.WordCloud = function () {
    // Set default values
    this._components = [];
    this._metrics = [];
    this._metricsPriority = [];
    this._width = window.SonarWidgets.WordCloud.defaults.width;
    this._height = window.SonarWidgets.WordCloud.defaults.height;
    this._margin = window.SonarWidgets.WordCloud.defaults.margin;
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

    this.options = function (_) {
      return param.call(this, '_options', _);
    };
  };

  window.SonarWidgets.WordCloud.prototype.render = function (container) {
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
    this.colorMetric = this.metricsPriority()[0];
    this.getColorMetric = function(d) {
      return d.measures[widget.colorMetric].val;
    };

    this.sizeMetric = this.metricsPriority()[1];
    this.getSizeMetric = function(d) {
      return d.measures[widget.sizeMetric].val;
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
    this.color = d3.scale.sqrt()
        .domain(d3.extent(this.components(), function(d) {
          return widget.getColorMetric(d);
        }))
        .range(['#ee0000', '#2360bf']);

    this.size = d3.scale.linear()
        .domain(d3.extent(this.components(), function(d) {
          return widget.getSizeMetric(d);
        }))
        .range([10, 48]);


    // Update widget
    this.update(containerS);

    return this;
  };



  window.SonarWidgets.WordCloud.prototype.update = function(container) {
    container = d3.select(container);

    var widget = this,
        width = container.property('offsetWidth');
    this.width(width > 100 ? width : 100);


    // Update available size
    this.availableWidth = this.width() - this.margin().left - this.margin().right;
    this.availableHeight = 1000;
    this.height(this.availableHeight + this.margin().top + this.margin().bottom);


    // Update svg canvas
    this.svg
        .attr('width', this.width())
        .attr('height', this.height());


    // Update plot
    this.plotWrap
        .transition()
        .attr('transform', trans(this.availableWidth / 2, this.availableHeight / 2));


    // Configure cloud
    var wordsData = this.components().map(function(d) {
      return {
        text: d.name,
        size: widget.size(widget.getSizeMetric(d)),
        color: widget.color(widget.getColorMetric(d))
      };
    });

    this.cloud = d3.layout.cloud().size([this.availableWidth, this.availableHeight])
        .words(wordsData)
        .padding(5)
        .rotate(0)
        .font("Arial")
        .fontSize(function(d) { return d.size; })
        .on("end", draw)
        .start();

    function draw(words) {
      widget.words = widget.plotWrap.selectAll("text")
          .data(words);

      widget.words.enter().append("text")
          .style("font-size", function(d) { return d.size + "px"; })
          .style("font-family", "Arial")
          .style("fill", function(d) { return d.color; })
          .attr("text-anchor", "middle")
          .text(function(d) { return d.text; });

      widget.words
          .transition()
          .attr("transform", function(d) {
            return "translate(" + [d.x, d.y] + ")rotate(" + d.rotate + ")";
          });

      widget.words.exit().remove();
    }
  };



  window.SonarWidgets.WordCloud.defaults = {
    width: 350,
    height: 300,
    margin: { top: 0, right: 0, bottom: 0, left: 0 }
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
