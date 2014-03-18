(function() {
  var __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  define(['backbone.marionette', 'common/handlebars-extensions'], function(Marionette) {
    var CodingRulesBulkChangeView;
    return CodingRulesBulkChangeView = (function(_super) {
      __extends(CodingRulesBulkChangeView, _super);

      function CodingRulesBulkChangeView() {
        return CodingRulesBulkChangeView.__super__.constructor.apply(this, arguments);
      }

      CodingRulesBulkChangeView.prototype.className = 'modal';

      CodingRulesBulkChangeView.prototype.template = getTemplate('#coding-rules-bulk-change-template');

      CodingRulesBulkChangeView.prototype.events = {
        'submit form': 'onSubmit',
        'click #coding-rules-cancel-bulk-change': 'hide',
        'click label': 'enableAction',
        'change select': 'enableAction'
      };

      CodingRulesBulkChangeView.prototype.onRender = function() {
        var format;
        this.$el.dialog({
          dialogClass: 'no-close',
          width: '600px',
          draggable: false,
          autoOpen: false,
          modal: true,
          minHeight: 50,
          resizable: false,
          title: null
        });
        this.$('#coding-rules-bulk-change-activate-on, #coding-rules-bulk-change-deactivate-on').select2({
          width: '250px',
          minimumResultsForSearch: 1
        });
        format = function(state) {
          if (!state.id) {
            return state.text;
          }
          return "<i class='icon-severity-" + (state.id.toLowerCase()) + "'></i> " + state.text;
        };
        return this.$('#coding-rules-bulk-change-severity').select2({
          width: '250px',
          minimumResultsForSearch: 999,
          formatResult: format,
          formatSelection: format,
          escapeMarkup: function(m) {
            return m;
          }
        });
      };

      CodingRulesBulkChangeView.prototype.show = function() {
        this.render();
        return this.$el.dialog('open');
      };

      CodingRulesBulkChangeView.prototype.hide = function() {
        return this.$el.dialog('close');
      };

      CodingRulesBulkChangeView.prototype.prepareQuery = function() {
        var actions, activateIn, deactivateIn, query, severity;
        query = this.options.app.getQuery();
        activateIn = [];
        deactivateIn = [];
        severity = null;
        if (this.$('#coding-rules-bulk-change-activate-qp').is(':checked')) {
          activateIn.push(this.options.app.getInactiveQualityProfile());
        }
        if (this.$('#coding-rules-bulk-change-activate').is(':checked')) {
          activateIn.push(this.$('#coding-rules-bulk-change-activate-on').val());
        }
        if (this.$('#coding-rules-bulk-change-deactivate-qp').is(':checked')) {
          deactivateIn.push(this.options.app.getActiveQualityProfile());
        }
        if (this.$('#coding-rules-bulk-change-deactivate').is(':checked')) {
          deactivateIn.push(this.$('#coding-rules-bulk-change-deactivate-on').val());
        }
        if (this.$('#coding-rules-bulk-change-set-severity').is(':checked')) {
          severity = this.$('#coding-rules-bulk-change-severity').val();
        }
        actions = [];
        if (activateIn.length > 0) {
          actions.push('bulk_activate');
          _.extend(query, {
            bulk_activate_in: activateIn.join(',')
          });
        }
        if (deactivateIn.length > 0) {
          actions.push('bulk_deactivate');
          _.extend(query, {
            bulk_deactivate_in: deactivateIn.join(',')
          });
        }
        if (severity) {
          actions.push('bulk_set_severity');
          _.extend(query, {
            bulk_severity: severity
          });
        }
        return _.extend(query, {
          bulk_actions: actions
        });
      };

      CodingRulesBulkChangeView.prototype.onSubmit = function(e) {
        e.preventDefault();
        return jQuery.ajax({
          type: 'POST',
          url: "" + baseUrl + "/api/codingrules/bulk_change",
          data: this.prepareQuery()
        }).done((function(_this) {
          return function() {
            _this.options.app.fetchFirstPage();
            return _this.hide();
          };
        })(this));
      };

      CodingRulesBulkChangeView.prototype.enableAction = function(e) {
        return jQuery(e.target).siblings('input[type=checkbox]').prop('checked', true);
      };

      CodingRulesBulkChangeView.prototype.serializeData = function() {
        return {
          paging: this.options.app.codingRules.paging,
          qualityProfiles: this.options.app.qualityProfiles,
          activeQualityProfile: this.options.app.getActiveQualityProfile(),
          activeQualityProfileName: this.options.app.activeInFilter.view.renderValue(),
          activateOnQualityProfiles: _.reject(this.options.app.qualityProfiles, (function(_this) {
            return function(q) {
              return q.key === _this.options.app.getInactiveQualityProfile();
            };
          })(this)),
          inactiveQualityProfile: this.options.app.getInactiveQualityProfile(),
          inactiveQualityProfileName: this.options.app.inactiveInFilter.view.renderValue(),
          deactivateOnQualityProfiles: _.reject(this.options.app.qualityProfiles, (function(_this) {
            return function(q) {
              return q.key === _this.options.app.getActiveQualityProfile();
            };
          })(this)),
          severities: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']
        };
      };

      return CodingRulesBulkChangeView;

    })(Marionette.ItemView);
  });

}).call(this);
