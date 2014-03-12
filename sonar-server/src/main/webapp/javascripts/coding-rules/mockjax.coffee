define ['jquery.mockjax'], ->

  jQuery.mockjaxSettings.contentType = 'text/json';
  jQuery.mockjaxSettings.responseTime = 250;

  # GET /api/codingrules/app
  jQuery.mockjax
    url: "#{baseUrl}/api/codingrules/app"
    responseText: JSON.stringify
      qualityprofiles: [
        { name: 'Sonar Way', lang: 'Java' }
        { name: 'Sonar Way', lang: 'JavaScript' }
        { name: 'Quality Profile 1', lang: 'Java' }
      ]
      messages:
        'all': 'All'
        'bulk_change': 'Bulk Change'
        'moreCriteria': '+ More Criteria'
        'search_verb': 'Search'
        'update': 'Update'

        'severity.BLOCKER': 'Blocker'
        'severity.CRITICAL': 'Critical'
        'severity.MAJOR': 'Major'
        'severity.MINOR': 'Minor'
        'severity.INFO': 'Info'

        'coding_rules.page': 'Coding Rules'
        'coding_rules.new_search': 'New Search'
        'coding_rules.no_results': 'No Coding Rules'
        'coding_rules.found': 'Found'
        'coding_rules.quality_profiles': 'Quality Profiles'
        'coding_rules.activate_quality_profile': 'Activate Quality Profile'
        'coding_rules.deactivate_quality_profile': 'Deactivate'

        'coding_rules.filters.active_in': 'Active In'
        'coding_rules.filters.inactive_in': 'Inactive In'
        'coding_rules.filters.name_key': 'Name/Key'
        'coding_rules.filters.repository': 'Repository'
        'coding_rules.filters.severity': 'Severity'
        'coding_rules.filters.status': 'Status'
        'coding_rules.filters.tag': 'Tag'


  # GET /api/codingrules/search
  jQuery.mockjax
    url: "#{baseUrl}/api/codingrules/search"
    responseText: JSON.stringify
      codingrules: [
        {
          name: 'Array designators "[]" should be located after the type in method signatures'
          language: 'Java'
          severity: 'MAJOR'
        },
        {
          name: 'Avoid Array Loops'
          language: 'Java'
          severity: 'CRITICAL'
        },
        {
          name: 'Bad practice - Abstract class defines covariant compareTo() method'
          language: 'Java'
          severity: 'MAJOR'
        },
        {
          name: 'Correctness - Use of class without a hashCode() method in a hashed data structure'
          language: 'Java'
          severity: 'MINOR'
        },
        {
          name: 'Useless Operation On Immutable'
          language: 'Java'
          severity: 'MAJOR'
        }
      ]
      paging:
        total: 5
        fTotal: '5'



  # GET /api/codingrules/show
  jQuery.mockjax
    url: "#{baseUrl}/api/codingrules/show"
    responseText: JSON.stringify
      codingrule:
        name: 'Array designators "[]" should be located after the type in method signatures'
        language: 'Java'
        description: '''
            <p>
            According to the Java Language Specification:
            </p>

            <pre>For compatibility with older versions of the Java SE platform,
            the declaration of a method that returns an array is allowed to place (some or all of)
            the empty bracket pairs that form the declaration of the array type after
            the formal parameter list. This obsolescent syntax should not be used in new code.
            </pre>

            <p>The following code snippet illustrates this rule:</p>

            <pre>public int getVector()[] { /* ... */ }    // Non-Compliant

            public int[] getVector() { /* ... */ }    // Compliant

            public int[] getMatrix()[] { /* ... */ }  // Non-Compliant

            public int[][] getMatrix() { /* ... */ }  // Compliant
            </pre>'''

        qualityProfiles: [
          {
            name: 'SonarWay'
            severity: 'MINOR'
            canDeactivate: true
            canUpdate: true
            parameters: [
              { key: 'max', value: 8 }
            ]

          },
          {
            name: 'Quality Profile 1'
            severity: 'MAJOR'
            canDeactivate: false
            canUpdate: false
            parameters: [
              { key: 'max', value: 6 }
            ]

          }
        ]



  # GET /api/qualityprofiles/list
  jQuery.mockjax
    url: "#{baseUrl}/api/qualityprofiles/list"
    responseText: JSON.stringify
      more: false
      results: [
        { id: 'sonarway', text: 'Sonar Way' },
        { id: 'qp1', text: 'Quality Profile 1' },
        { id: 'qp2', text: 'Quality Profile 2' },
        { id: 'qp3', text: 'Quality Profile 3' },
      ]

