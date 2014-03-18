(function() {
  var __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  define(['backbone', 'quality-gate/models/condition'], function(Backbone, Condition) {
    var Conditions;
    return Conditions = (function(_super) {
      __extends(Conditions, _super);

      function Conditions() {
        return Conditions.__super__.constructor.apply(this, arguments);
      }

      Conditions.prototype.model = Condition;

      return Conditions;

    })(Backbone.Collection);
  });

}).call(this);
