(function() {
  var __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  define(['backbone.marionette', 'common/handlebars-extensions'], function(Marionette) {
    var CodingRulesListEmptyView;
    return CodingRulesListEmptyView = (function(_super) {
      __extends(CodingRulesListEmptyView, _super);

      function CodingRulesListEmptyView() {
        return CodingRulesListEmptyView.__super__.constructor.apply(this, arguments);
      }

      CodingRulesListEmptyView.prototype.tagName = 'li';

      CodingRulesListEmptyView.prototype.className = 'navigator-results-no-results';

      CodingRulesListEmptyView.prototype.template = getTemplate('#coding-rules-list-empty-template');

      return CodingRulesListEmptyView;

    })(Marionette.ItemView);
  });

}).call(this);
