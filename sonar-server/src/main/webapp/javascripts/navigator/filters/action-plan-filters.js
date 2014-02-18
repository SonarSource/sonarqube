define(['backbone', 'navigator/filters/base-filters', 'navigator/filters/select-filters'], function (Backbone, BaseFilters, SelectFilters) {

  return SelectFilters.SelectFilterView.extend({

    initialize: function() {
      SelectFilters.SelectFilterView.prototype.initialize.apply(this, arguments);
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
      this.$el.removeClass('navigator-filter-inactive');
    },


    makeInactive: function() {
      this.selection.reset([]);
      this.choices.reset([]);
      this.detailsView.updateLists();
      this.detailsView.updateValue();
      this.$el.addClass('navigator-filter-inactive');
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
                text: plan.name
              }
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
    }

  });

});
