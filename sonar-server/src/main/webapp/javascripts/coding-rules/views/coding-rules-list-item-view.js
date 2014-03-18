(function() {
  var __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  define(['backbone.marionette', 'coding-rules/views/coding-rules-detail-view', 'common/handlebars-extensions'], function(Marionette, CodingRulesDetailView) {
    var CodingRulesListItemView;
    return CodingRulesListItemView = (function(_super) {
      __extends(CodingRulesListItemView, _super);

      function CodingRulesListItemView() {
        return CodingRulesListItemView.__super__.constructor.apply(this, arguments);
      }

      CodingRulesListItemView.prototype.tagName = 'li';

      CodingRulesListItemView.prototype.template = getTemplate('#coding-rules-list-item-template');

      CodingRulesListItemView.prototype.activeClass = 'active';

      CodingRulesListItemView.prototype.events = function() {
        return {
          'click': 'showDetail'
        };
      };

      CodingRulesListItemView.prototype.showDetail = function() {
        this.$el.siblings().removeClass(this.activeClass);
        this.$el.addClass(this.activeClass);
        this.options.app.layout.showSpinner('detailsRegion');
        return jQuery.ajax({
          url: "" + baseUrl + "/api/codingrules/show"
        }).done((function(_this) {
          return function(r) {
            var detailView;
            _this.model.set(r.codingrule);
            detailView = new CodingRulesDetailView({
              app: _this.options.app,
              model: _this.model
            });
            return _this.options.app.layout.detailsRegion.show(detailView);
          };
        })(this));
      };

      CodingRulesListItemView.prototype.serializeData = function() {
        return _.extend(CodingRulesListItemView.__super__.serializeData.apply(this, arguments), {
          qualityProfile: this.options.app.getActiveQualityProfile(),
          qualityProfileName: this.options.app.activeInFilter.view.renderValue()
        });
      };

      return CodingRulesListItemView;

    })(Marionette.ItemView);
  });

}).call(this);
