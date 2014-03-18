(function() {
  var __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  define(['backbone.marionette', 'common/handlebars-extensions'], function(Marionette) {
    var CodingRulesHeaderView;
    return CodingRulesHeaderView = (function(_super) {
      __extends(CodingRulesHeaderView, _super);

      function CodingRulesHeaderView() {
        return CodingRulesHeaderView.__super__.constructor.apply(this, arguments);
      }

      CodingRulesHeaderView.prototype.template = getTemplate('#coding-rules-header-template');

      CodingRulesHeaderView.prototype.events = {
        'click #coding-rules-new-search': 'newSearch'
      };

      CodingRulesHeaderView.prototype.newSearch = function() {
        return this.options.app.router.navigate('', {
          trigger: true
        });
      };

      return CodingRulesHeaderView;

    })(Marionette.ItemView);
  });

}).call(this);
