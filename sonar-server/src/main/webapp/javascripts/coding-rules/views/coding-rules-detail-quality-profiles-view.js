(function() {
  var __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  define(['backbone.marionette', 'coding-rules/views/coding-rules-detail-quality-profile-view'], function(Marionette, CodingRulesDetailQualityProfileView) {
    var CodingRulesDetailQualityProfilesView;
    return CodingRulesDetailQualityProfilesView = (function(_super) {
      __extends(CodingRulesDetailQualityProfilesView, _super);

      function CodingRulesDetailQualityProfilesView() {
        return CodingRulesDetailQualityProfilesView.__super__.constructor.apply(this, arguments);
      }

      CodingRulesDetailQualityProfilesView.prototype.itemView = CodingRulesDetailQualityProfileView;

      CodingRulesDetailQualityProfilesView.prototype.itemViewOptions = function() {
        return {
          qualityProfiles: this.collection
        };
      };

      return CodingRulesDetailQualityProfilesView;

    })(Marionette.CollectionView);
  });

}).call(this);
