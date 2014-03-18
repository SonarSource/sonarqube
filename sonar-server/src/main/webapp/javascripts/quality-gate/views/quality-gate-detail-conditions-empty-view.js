(function() {
  var __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  define(['backbone.marionette', 'handlebars'], function(Marionette, Handlebars) {
    var QualityGateDetailConditionsView;
    return QualityGateDetailConditionsView = (function(_super) {
      __extends(QualityGateDetailConditionsView, _super);

      function QualityGateDetailConditionsView() {
        return QualityGateDetailConditionsView.__super__.constructor.apply(this, arguments);
      }

      QualityGateDetailConditionsView.prototype.tagName = 'tr';

      QualityGateDetailConditionsView.prototype.template = Handlebars.compile(jQuery('#quality-gate-detail-conditions-empty-template').html());

      return QualityGateDetailConditionsView;

    })(Marionette.ItemView);
  });

}).call(this);
