(function() {
  var __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  define(['backbone', 'quality-gate/models/quality-gate'], function(Backbone, QualityGate) {
    var QualityGates;
    return QualityGates = (function(_super) {
      __extends(QualityGates, _super);

      function QualityGates() {
        return QualityGates.__super__.constructor.apply(this, arguments);
      }

      QualityGates.prototype.model = QualityGate;

      QualityGates.prototype.url = function() {
        return "" + baseUrl + "/api/qualitygates/list";
      };

      QualityGates.prototype.parse = function(r) {
        return r.qualitygates.map(function(gate) {
          return _.extend(gate, {
            "default": gate.id === r["default"]
          });
        });
      };

      return QualityGates;

    })(Backbone.Collection);
  });

}).call(this);
