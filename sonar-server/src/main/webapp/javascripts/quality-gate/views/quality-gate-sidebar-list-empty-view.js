(function() {
  var __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  define(['backbone.marionette', 'handlebars'], function(Marionette, Handlebars) {
    var QualityGateSidebarListEmptyView;
    return QualityGateSidebarListEmptyView = (function(_super) {
      __extends(QualityGateSidebarListEmptyView, _super);

      function QualityGateSidebarListEmptyView() {
        return QualityGateSidebarListEmptyView.__super__.constructor.apply(this, arguments);
      }

      QualityGateSidebarListEmptyView.prototype.tagName = 'li';

      QualityGateSidebarListEmptyView.prototype.className = 'empty';

      QualityGateSidebarListEmptyView.prototype.template = Handlebars.compile(jQuery('#quality-gate-sidebar-list-empty-template').html());

      return QualityGateSidebarListEmptyView;

    })(Marionette.ItemView);
  });

}).call(this);
