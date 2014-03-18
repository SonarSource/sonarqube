(function() {
  var __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  define(['backbone.marionette', 'handlebars', 'quality-gate/models/quality-gate'], function(Marionette, Handlebars, QualityGate) {
    var QualityGateActionsView;
    return QualityGateActionsView = (function(_super) {
      __extends(QualityGateActionsView, _super);

      function QualityGateActionsView() {
        return QualityGateActionsView.__super__.constructor.apply(this, arguments);
      }

      QualityGateActionsView.prototype.template = Handlebars.compile(jQuery('#quality-gate-actions-template').html());

      QualityGateActionsView.prototype.events = {
        'click #quality-gate-add': 'add'
      };

      QualityGateActionsView.prototype.add = function() {
        var qualityGate;
        qualityGate = new QualityGate();
        this.options.app.qualityGateEditView.method = 'create';
        this.options.app.qualityGateEditView.model = qualityGate;
        return this.options.app.qualityGateEditView.show();
      };

      QualityGateActionsView.prototype.serializeData = function() {
        return _.extend(QualityGateActionsView.__super__.serializeData.apply(this, arguments), {
          canEdit: this.options.app.canEdit
        });
      };

      return QualityGateActionsView;

    })(Marionette.ItemView);
  });

}).call(this);
