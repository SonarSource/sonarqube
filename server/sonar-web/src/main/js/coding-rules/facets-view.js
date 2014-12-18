define([
  'components/navigator/facets-view',
  'coding-rules/facets/base-facet',
  'coding-rules/facets/language-facet',
  'coding-rules/facets/repository-facet',
  'coding-rules/facets/quality-profile-facet',
  'coding-rules/facets/characteristic-facet',
  'coding-rules/facets/severity-facet',
  'coding-rules/facets/status-facet'
],
    function (FacetsView,
              BaseFacet,
              LanguageFacet,
              RepositoryFacet,
              QualityProfileFacet,
              CharacteristicFacet,
              SeverityFacet,
              StatusFacet) {

      return FacetsView.extend({

        getItemView: function (model) {
          switch (model.get('property')) {
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
            default:
              return BaseFacet;
          }
        }

      });

    });
