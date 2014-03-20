/*global d3:false, SonarWidgets:false */
/*jshint eqnull:true */

window.SonarWidgets = window.SonarWidgets == null ? {} : window.SonarWidgets;

(function () {

  window.SonarWidgets.Widget = function () {
    // Set default values
    this._type = null;
    this._source = null;
    this._metricsPriority = null;
    this._height = null;
    this._options = {};


    // Export global variables
    this.type = function (_) {
      return param.call(this, '_type', _);
    };

    this.source = function (_) {
      return param.call(this, '_source', _);
    };

    this.metricsPriority = function (_) {
      return param.call(this, '_metricsPriority', _);
    };

    this.height = function (_) {
      return param.call(this, '_height', _);
    };

    this.options = function (_) {
      return param.call(this, '_options', _);
    };
  };


  window.SonarWidgets.Widget.prototype.render = function(container) {
    var that = this;

    d3.json(this.source(), function(error, response) {
      if (response && !error) {
        if (response.components.length > 0) {
          that.widget = new SonarWidgets[that.type()]();
          that.widget
              .metrics(response.metrics)
              .metricsPriority(that.metricsPriority())
              .components(response.components)
              .options(that.options());
          if (typeof that.widget.maxResultsReached === 'function') {
            that.widget.maxResultsReached(response.paging.pages > 1);
          }
          if (that.height()) {
            that.widget.height(that.height());
          }
          that.widget.render(container);
        } else {
          d3.select(container).html(that.options().noData);
        }
      }
    });
  };


  window.SonarWidgets.Widget.prototype.update = function(container) {
    return this.widget && this.widget.update(container);
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

})();
