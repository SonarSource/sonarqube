define(['backbone', 'navigator/filters/base-filters', 'navigator/filters/choice-filters'], function (Backbone, BaseFilters, ChoiceFilters) {

  return ChoiceFilters.ChoiceFilterView.extend({

    initialize: function() {
      ChoiceFilters.ChoiceFilterView.prototype.initialize.apply(this, arguments);
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
        ChoiceFilters.ChoiceFilterView.prototype.showDetails.apply(this, arguments);
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

            that.choices.reset(nonClosedActionPlans.map(function(plan) {
              return {
                id: plan.key,
                text: plan.name,
                category: plan.fDeadLine
              }
            }));
            _.each(that.model.get('choices'), function(v, k) {
              that.choices.add(new Backbone.Model({ id: k, text: v }));
            });
            var value = that.model.get('value');
            _.each(value, function(v) {
              var cModel = that.choices.findWhere({ id: v });
              cModel.set('checked', true);
            });
            that.detailsView.updateValue();
            that.render();

            that.makeActive();
          });
    },


    restore: function(value) {
      if (_.isString(value)) {
        value = value.split(',');
      }

      if (this.choices && value.length > 0) {
        this.model.set({ value: value, enabled: true });
        this.onChangeProjectFilter();
      } else {
        this.clear();
      }
    }

  });

});
