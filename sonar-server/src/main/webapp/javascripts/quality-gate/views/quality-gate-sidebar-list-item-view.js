(function() {
  var __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  define(['backbone.marionette', 'handlebars'], function(Marionette, Handlebars) {
    var QualityGateSidebarListItemView;
    return QualityGateSidebarListItemView = (function(_super) {
      __extends(QualityGateSidebarListItemView, _super);

      function QualityGateSidebarListItemView() {
        return QualityGateSidebarListItemView.__super__.constructor.apply(this, arguments);
      }

      QualityGateSidebarListItemView.prototype.tagName = 'li';

      QualityGateSidebarListItemView.prototype.template = Handlebars.compile(jQuery('#quality-gate-sidebar-list-item-template').html());

      QualityGateSidebarListItemView.prototype.modelEvents = {
        'change': 'render'
      };

      QualityGateSidebarListItemView.prototype.events = {
        'click': 'showQualityGate'
      };

      QualityGateSidebarListItemView.prototype.onRender = function() {
        return this.$el.toggleClass('active', this.options.highlighted);
      };

      QualityGateSidebarListItemView.prototype.showQualityGate = function() {
        return this.options.app.router.navigate("show/" + this.model.id, {
          trigger: true
        });
      };

      return QualityGateSidebarListItemView;

    })(Marionette.ItemView);
  });

}).call(this);
