define(['backbone', 'navigator/filters/base-filters', 'navigator/filters/select-filters'], function (Backbone, BaseFilters, SelectFilters) {

  var UNPLANNED = '<unplanned>';


  var ActionPlanDetailsFilterView = SelectFilters.DetailsSelectFilterView.extend({

    addToSelection: function(e) {
      var id = $j(e.target).val(),
          model = this.options.filterView.choices.findWhere({ id: id });

      if (this.model.get('multiple') && id !== UNPLANNED) {
        this.options.filterView.selection.add(model);
        this.options.filterView.choices.remove(model);

        var unplanned = this.options.filterView.selection.findWhere({ id: UNPLANNED });
        if (unplanned) {
          this.options.filterView.choices.add(unplanned);
          this.options.filterView.selection.remove(unplanned);
        }
      } else {
        this.options.filterView.choices.add(this.options.filterView.selection.models);
        this.options.filterView.choices.remove(model);
        this.options.filterView.selection.reset([model]);
      }

      this.updateValue();
      this.updateLists();
    },


    resetChoices: function() {
      if (this.options.filterView.selection.findWhere({ id: UNPLANNED })) {
        this.options.filterView.choices.reset([]);
      } else {
        this.options.filterView.choices.reset([{
          id: UNPLANNED,
          text: window.SS.phrases.unplanned,
          special: true
        }]);
      }
    }
  });


  return SelectFilters.SelectFilterView.extend({

    initialize: function() {
      SelectFilters.SelectFilterView.prototype.initialize.call(this, {
        detailsView: ActionPlanDetailsFilterView
      });
      this.projectFilter = this.model.get('projectFilter');
      this.listenTo(this.projectFilter, 'change:value', this.onChangeProjectFilter);
      this.onChangeProjectFilter();
    },


    onChangeProjectFilter: function() {
      var projects = this.projectFilter.get('value');
      if (_.isArray(projects) && projects.length === 1) {
        return this.fetchActionPlans(projects[0]);
      } else {
        return this.makeInactive();
      }
    },


    showDetails: function() {
      if (!this.$el.is('.navigator-filter-inactive')) {
        SelectFilters.SelectFilterView.prototype.showDetails.apply(this, arguments);
      }
    },


    makeActive: function() {
      this.model.set({
        inactive: false,
        title: ''
      });
      this.model.trigger('change:enabled');
      this.$el.removeClass('navigator-filter-inactive').prop('title', '');
    },


    makeInactive: function() {
      this.model.set({
        inactive: true,
        title: window.SS.phrases.actionPlanNotAvailable
      });
      this.model.trigger('change:enabled');
      this.selection.reset([]);
      this.choices.reset([]);
      this.detailsView.updateLists();
      this.detailsView.updateValue();
      this.$el.addClass('navigator-filter-inactive')
          .prop('title', window.SS.phrases.actionPlanNotAvailable);
    },


    fetchActionPlans: function(project) {
      var that = this;
      return jQuery.ajax({
        url: baseUrl + '/api/action_plans/search',
        type: 'GET',
        data: { project: project }
      }).done(function(r) {
            var nonClosedActionPlans =
                _.sortBy(_.reject(r.actionPlans, function(plan) {
                      return plan.status === 'CLOSED';
                    }), 'name');

            that.selection.reset([]);
            that.choices.reset(nonClosedActionPlans.map(function(plan) {
              return {
                id: plan.key,
                text: plan.name,
                category: plan.fDeadLine
              }
            }));
            that.choices.add(new Backbone.Model({
              id: UNPLANNED,
              text: window.SS.phrases.unplanned,
              special: true
            }));

            var value = that.model.get('value');
            if (that.choices && that.selection && value && value.length > 0) {
              value.forEach(function(id) {
                var model = that.choices.findWhere({ id: id });
                that.selection.add(model);
                that.choices.remove(model);
              });
            }
            that.detailsView.updateValue();
            that.render();

            that.makeActive();
          });
    },


    restoreFromQuery: function(q) {
      var param = _.findWhere(q, { key: this.model.get('property') }),
          planned = _.findWhere(q, { key: 'planned' });

      if (!!planned) {
        if (!param) {
          param = { value: UNPLANNED };
        } else {
          param.value += ',' + UNPLANNED;
        }
      }

      if (param && param.value) {
        this.model.set('enabled', true);
        this.restore(param.value);
      } else {
        this.clear();
      }
    },


    restore: function(value) {
      if (_.isString(value)) {
        value = value.split(',');
      }

      if (this.choices && this.selection && value.length > 0) {
        this.model.set({ value: value, enabled: true });
        this.onChangeProjectFilter();
      } else {
        this.clear();
      }
    },


    formatValue: function() {
      var q = {};
      if (this.model.has('property') && this.model.has('value') && this.model.get('value').length > 0) {
        var assignees = _.without(this.model.get('value'), UNPLANNED);
        if (assignees.length > 0) {
          q[this.model.get('property')] = assignees.join(',');
        }
        if (this.model.get('value').length > assignees.length) {
          q.planned = false;
        }
      }
      return q;
    }

  });

});
