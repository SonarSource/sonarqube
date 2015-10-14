import _ from 'underscore';

(function () {

  if (window.SonarWidgets == null) {
    window.SonarWidgets = {};
  }

  function BaseWidget () {
    this.addField('components', []);
    this.addField('metrics', []);
    this.addField('metricsPriority', []);
    this.addField('options', []);
  }

  BaseWidget.prototype.lineHeight = 20;
  BaseWidget.prototype.colors4 = ['#ee0000', '#f77700', '#80cc00', '#00aa00'];
  BaseWidget.prototype.colors4r = ['#00aa00', '#80cc00', '#f77700', '#ee0000'];
  BaseWidget.prototype.colors5 = ['#ee0000', '#f77700', '#ffee00', '#80cc00', '#00aa00'];
  BaseWidget.prototype.colors5r = ['#00aa00', '#80cc00', '#ffee00', '#f77700', '#ee0000'];
  BaseWidget.prototype.colorsLevel = ['#d4333f', '#ff9900', '#85bb43', '##b4b4b4'];
  BaseWidget.prototype.colorUnknown = '#777';


  BaseWidget.prototype.addField = function (name, defaultValue) {
    var privateName = '_' + name;
    this[privateName] = defaultValue;
    this[name] = function (d) {
      return this.param.call(this, privateName, d);
    };
    return this;
  };

  BaseWidget.prototype.param = function (name, value) {
    if (value == null) {
      return this[name];
    }
    this[name] = value;
    return this;
  };

  BaseWidget.prototype.addMetric = function (property, index) {
    var key = this.metricsPriority()[index];
    this[property] = _.extend(this.metrics()[key], {
      key: key,
      value: function (d) {
        if (d.measures[key] != null) {
          if (d.measures[key].text != null) {
            return d.measures[key].text;
          } else if (d.measures[key].data != null) {
            return d.measures[key].data;
          } else {
            return d.measures[key].val;
          }
        }
      },
      formattedValue: function (d) {
        if (d.measures[key] != null) {
          if (d.measures[key].text != null) {
            return d.measures[key].text;
          } else {
            return d.measures[key].fval;
          }
        }
      }
    });
    return this;
  };

  BaseWidget.prototype.trans = function (left, top) {
    return 'translate(' + left + ',' + top + ')';
  };

  BaseWidget.prototype.render = function (container) {
    this.update(container);
    return this;
  };

  BaseWidget.prototype.update = function () {
    return this;
  };

  BaseWidget.prototype.tooltip = function (d) {
    /* jshint nonbsp: false */
    var title = d.longName;
    if (this.colorMetric && (this.colorMetric.value(d) != null)) {
      title += '\n' + this.colorMetric.name + ': ' + (this.colorMetric.formattedValue(d));
    }
    if (this.sizeMetric && (this.sizeMetric.value(d) != null)) {
      title += '\n' + this.sizeMetric.name + ': ' + (this.sizeMetric.formattedValue(d));
    }
    return title;
  };

  window.SonarWidgets.BaseWidget = BaseWidget;

})();
