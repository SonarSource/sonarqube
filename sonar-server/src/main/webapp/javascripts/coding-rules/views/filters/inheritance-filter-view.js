(function() {
  var __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  define(['navigator/filters/choice-filters'], function(ChoiceFilters) {
    var InheritanceFilterView;
    return InheritanceFilterView = (function(_super) {
      __extends(InheritanceFilterView, _super);

      function InheritanceFilterView() {
        return InheritanceFilterView.__super__.constructor.apply(this, arguments);
      }

      InheritanceFilterView.prototype.initialize = function() {
        InheritanceFilterView.__super__.initialize.apply(this, arguments);
        this.qualityProfileFilter = this.model.get('qualityProfileFilter');
        this.listenTo(this.qualityProfileFilter, 'change:value', this.onChangeQualityProfile);
        return this.onChangeQualityProfile();
      };

      InheritanceFilterView.prototype.onChangeQualityProfile = function() {
        var parentQualityProfile, qualityProfile;
        qualityProfile = this.qualityProfileFilter.get('value');
        parentQualityProfile = this.qualityProfileFilter.get('parentQualityProfile');
        if (_.isArray(qualityProfile) && qualityProfile.length === 1 && parentQualityProfile) {
          return this.makeActive();
        } else {
          return this.makeInactive();
        }
      };

      InheritanceFilterView.prototype.makeActive = function() {
        this.model.set({
          inactive: false,
          title: ''
        });
        this.model.trigger('change:enabled');
        return this.$el.removeClass('navigator-filter-inactive').prop('title', '');
      };

      InheritanceFilterView.prototype.makeInactive = function() {
        this.model.set({
          inactive: true,
          title: t('coding_rules.filters.inheritance.inactive')
        });
        this.model.trigger('change:enabled');
        this.choices.each(function(model) {
          return model.set('checked', false);
        });
        this.detailsView.updateLists();
        this.detailsView.updateValue();
        return this.$el.addClass('navigator-filter-inactive').prop('title', t('coding_rules.filters.inheritance.inactive'));
      };

      InheritanceFilterView.prototype.showDetails = function() {
        if (!this.$el.is('.navigator-filter-inactive')) {
          return InheritanceFilterView.__super__.showDetails.apply(this, arguments);
        }
      };

      InheritanceFilterView.prototype.restore = function(value) {
        if (_.isString(value)) {
          value = value.split(',');
        }
        if (this.choices && value.length > 0) {
          this.model.set({
            value: value,
            enabled: true
          });
          return this.onChangeQualityProfile;
        } else {
          return this.clear();
        }
      };

      return InheritanceFilterView;

    })(ChoiceFilters.ChoiceFilterView);
  });

}).call(this);
