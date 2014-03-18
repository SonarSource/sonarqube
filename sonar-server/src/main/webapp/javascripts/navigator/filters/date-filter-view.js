(function() {
  var __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  define(['navigator/filters/string-filters'], function(StringFilterView) {
    var DateFilterView;
    return DateFilterView = (function(_super) {
      __extends(DateFilterView, _super);

      function DateFilterView() {
        return DateFilterView.__super__.constructor.apply(this, arguments);
      }

      DateFilterView.prototype.render = function() {
        DateFilterView.__super__.render.apply(this, arguments);
        return this.detailsView.$('input').prop('placeholder', '1970-01-31');
      };

      return DateFilterView;

    })(StringFilterView);
  });

}).call(this);
