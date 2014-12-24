define([
  'components/navigator/facets-view',
  'coding-rules/facets/base-facet',
  'coding-rules/facets/query-facet',
  'coding-rules/facets/language-facet',
  'coding-rules/facets/repository-facet',
  'coding-rules/facets/quality-profile-facet',
  'coding-rules/facets/characteristic-facet',
  'coding-rules/facets/severity-facet',
  'coding-rules/facets/status-facet',
  'coding-rules/facets/available-since-facet',
  'coding-rules/facets/inheritance-facet',
  'coding-rules/facets/active-severity-facet'
],
    function (FacetsView,
              BaseFacet,
              QueryFacet,
              LanguageFacet,
              RepositoryFacet,
              QualityProfileFacet,
              CharacteristicFacet,
              SeverityFacet,
              StatusFacet,
              AvailableSinceFacet,
              InheritanceFacet,
              ActiveSeverityFacet) {

      return FacetsView.extend({

        getItemView: function (model) {
          switch (model.get('property')) {
            case 'q':
              return QueryFacet;
            case 'languages':
              return LanguageFacet;
            case 'repositories':
              return RepositoryFacet;
            case 'qprofile':
              return QualityProfileFacet;
            case 'debt_characteristics':
              return CharacteristicFacet;
            case 'severities':
              return SeverityFacet;
            case 'statuses':
              return StatusFacet;
            case 'available_since':
              return AvailableSinceFacet;
            case 'inheritance':
              return InheritanceFacet;
            case 'active_severities':
              return ActiveSeverityFacet;
            default:
              return BaseFacet;
          }
        }

      });

    });
