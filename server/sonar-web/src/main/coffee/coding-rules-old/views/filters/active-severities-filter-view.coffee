
define [
  'coding-rules-old/views/filters/profile-dependent-filter-view'
], (
  ProfileDependentFilterView
) ->

  class ActiveSeveritiesFilterView extends ProfileDependentFilterView
    tooltip: 'coding_rules.filters.active_severity.inactive'
