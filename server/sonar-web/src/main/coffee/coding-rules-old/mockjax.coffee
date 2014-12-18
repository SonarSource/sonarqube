define ['jquery.mockjax'], ->

  jQuery.mockjaxSettings.contentType = 'text/json';
  jQuery.mockjaxSettings.responseTime = 250;

  # GET /api/codingrules/app
  jQuery.mockjax
    url: "#{baseUrl}/api/codingrules/app"
    responseText: JSON.stringify
      qualityprofiles: [
        { key: 'sonarway', name: 'Sonar Way', lang: 'Java', parent: null },
        { key: 'qualityprofile1', name: 'Quality Profile 1', lang: 'Java', parent: 'sonarway' },
        { key: 'qualityprofile2', name: 'Quality Profile 2', lang: 'JavaScript', parent: 'sonarway' },
        { key: 'qualityprofile3', name: 'Quality Profile 3', lang: 'Java', parent: null },
      ]
      languages:
        java: 'Java'
        javascript: 'JavaScript'
      repositories:
        'checkstyle': 'Checkstyle'
        'common-java': 'Common SonarQube'
        'findbugs': 'FindBugs'
        'pmd': 'PMD'
        'pmd-unit-tests': 'PMD Unit Tests'
        'squid': 'SonarQube'
      statuses:
        'BETA': 'Beta'
        'DEPRECATED': 'Deprecated'
        'READY': 'Ready'
      tags:
        'brain-overload': 'brain-overload'
        'bug': 'bug'
        'comment': 'comment'
        'convention': 'convention'
        'error-handling': 'error-handling'
        'formatting': 'formatting'
        'java8': 'java8'
        'multithreading': 'multithreading'
        'naming': 'naming'
        'pitfall': 'pitfall'
        'security': 'security'
        'size': 'size'
        'unused': 'unused'
        'unused-code': 'unused-code'
      characteristics:
        '1469': 'Changeability'
        '1441': 'Changeability: Architecture related changeability'
        '1470': 'Changeability: Data related changeability'
        '1475': 'Changeability: Logic related changeability'
        '1392': 'Efficiency'
        '1377': 'Efficiency: Memory use'
        '2965': 'Efficiency: Network use'
        '1393': 'Efficiency: Processor use'
        '1154': 'Maintainability'
        '1022': 'Maintainability: Readability'
        '1155': 'Maintainability: Understandability'
        '988': 'Portability'
        '977': 'Portability: Compiler related portability'
        '989': 'Portability: Hardware related portability'
        '994': 'Portability: Language related portability'
        '1000': 'Portability: OS related portability'
        '1006': 'Portability: Software related portability'
        '1021': 'Portability: Time zone related portability'
        '1551': 'Reliability'
        '1496': 'Reliability: Architecture related reliability'
        '1552': 'Reliability: Data related reliability'
        '1596': 'Reliability: Exception handling'
        '1622': 'Reliability: Fault tolerance'
        '1629': 'Reliability: Instruction related reliability'
        '1759': 'Reliability: Logic related reliability'
        '2948': 'Reliability: Resource'
        '1874': 'Reliability: Synchronization related reliability'
        '1925': 'Reliability: Unit tests'
        '975': 'Reusability'
        '974': 'Reusability: Modularity'
        '976': 'Reusability: Transportability'
        '1345': 'Security'
        '1335': 'Security: API abuse'
        '1346': 'Security: Errors'
        '1349': 'Security: Input validation and representation'
        '1364': 'Security: Security features'
        '1933': 'Testability'
        '1932': 'Testability: Integration level testability'
        '1934': 'Testability: Unit level testability'
      messages:
        'all': 'All'
        'any': 'Any'
        'apply': 'Apply'
        'are_you_sure': 'Are you sure?'
        'bold': 'Bold'
        'bulk_change': 'Bulk Change'
        'bulleted_point': 'Bulleted point'
        'cancel': 'Cancel'
        'change': 'Change'
        'code': 'Code'
        'delete': 'Delete'
        'done': 'Done'
        'edit': 'Edit'
        'markdown.helplink': 'Markdown Help'
        'moreCriteria': '+ More Criteria'
        'save': 'Save'
        'search_verb': 'Search'
        'severity': 'Severity'
        'update': 'Update'

        'severity.BLOCKER': 'Blocker'
        'severity.CRITICAL': 'Critical'
        'severity.MAJOR': 'Major'
        'severity.MINOR': 'Minor'
        'severity.INFO': 'Info'

        'coding_rules.activate': 'Activate'
        'coding_rules.activate_in': 'Activate In'
        'coding_rules.activate_in_quality_profile': 'Activate In Quality Profile'
        'coding_rules.activate_in_all_quality_profiles': 'Activate In All {0} Profiles'
        'coding_rules.add_note': 'Add Note'
        'coding_rules.available_since': 'Available Since'
        'coding_rules.bulk_change': 'Bulk Change'
        'coding_rules.change_severity': 'Change Severity'
        'coding_rules.change_severity_in': 'Change Severity In'
        'coding_rules.change_details': 'Change Details of Quality Profile'
        'coding_rules.extend_description': 'Extend Description'
        'coding_rules.deactivate_in': 'Deactivate In'
        'coding_rules.deactivate': 'Deactivate'
        'coding_rules.deactivate_in_quality_profile': 'Deactivate In Quality Profile'
        'coding_rules.deactivate_in_all_quality_profiles': 'Deactivate In All {0} Profiles'
        'coding_rules.found': 'Found'
        'coding_rules.inherits': '"{0}" inherits "{1}"'
        'coding_rules.key': 'Key:'
        'coding_rules.new_search': 'New Search'
        'coding_rules.no_results': 'No Coding Rules'
        'coding_rules.no_tags': 'No tags'
        'coding_rules.order': 'Order'
        'coding_rules.ordered_by': 'Ordered By'
        'coding_rules.original': 'Original:'
        'coding_rules.page': 'Coding Rules'
        'coding_rules.parameters': 'Parameters'
        'coding_rules.parameters.default_value': 'Default Value:'
        'coding_rules.permalink': 'Permalink'
        'coding_rules.quality_profiles': 'Quality Profiles'
        'coding_rules.quality_profile': 'Quality Profile'
        'coding_rules.repository': 'Repository:'
        'coding_rules.revert_to_parent_definition': 'Revert to Parent Definition'
        'coding_rules._rules': 'rules'
        'coding_rules.select_tag': 'Select Tag'

        'coding_rules.filters.activation': 'Activation'
        'coding_rules.filters.activation.active': 'Active'
        'coding_rules.filters.activation.inactive': 'Inactive'
        'coding_rules.filters.activation.help': 'Activation criterion is available when a quality profile is selected'
        'coding_rules.filters.availableSince': 'Available Since'
        'coding_rules.filters.characteristic': 'Characteristic'
        'coding_rules.filters.description': 'Description'
        'coding_rules.filters.quality_profile': 'Quality Profile'
        'coding_rules.filters.inheritance': 'Inheritance'
        'coding_rules.filters.inheritance.inactive': 'Inheritance criterion is available when an inherited quality profile is selected'
        'coding_rules.filters.inheritance.not_inherited': 'Not Inherited'
        'coding_rules.filters.inheritance.inherited': 'Inherited'
        'coding_rules.filters.inheritance.overriden': 'Overriden'
        'coding_rules.filters.key': 'Key'
        'coding_rules.filters.language': 'Language'
        'coding_rules.filters.name': 'Name'
        'coding_rules.filters.repository': 'Repository'
        'coding_rules.filters.severity': 'Severity'
        'coding_rules.filters.status': 'Status'
        'coding_rules.filters.tag': 'Tag'

        'coding_rules.sort.creation_date': 'Creation Date'
        'coding_rules.sort.name': 'Name'


  # GET /api/codingrules/search
  jQuery.mockjax
    url: "#{baseUrl}/api/codingrules/search"
    responseText: JSON.stringify
      codingrules: [
        {
          name: 'Array designators "[]" should be located after the type in method signatures'
          language: 'Java'
          severity: 'MAJOR'
          status: 'DEPRECATED'
        },
        {
          name: 'Avoid Array Loops'
          language: 'Java'
          severity: 'CRITICAL'
          status: 'READY'
        },
        {
          name: 'Bad practice - Abstract class defines covariant compareTo() method'
          language: 'Java'
          severity: 'MAJOR'
          status: 'READY'
        },
        {
          name: 'Correctness - Use of class without a hashCode() method in a hashed data structure'
          language: 'Java'
          severity: 'MINOR'
          status: 'BETA'
        },
        {
          name: 'Useless Operation On Immutable'
          language: 'Java'
          severity: 'MAJOR'
          status: 'READY'
        }
      ]
      paging:
        total: 5
        fTotal: '5'
      facets: [
        {
          name: 'Languages'
          property: 'languages'
          values: [
            { key: 'java', text: 'Java', stat: 45 }
            { key: 'javascript', text: 'JavaScript', stat: 21 }
          ]
        }
        {
          name: 'Tags'
          property: 'tags'
          values: [
            { key: 'brain-overload', text: 'brain-overload', stat: 8 }
            { key: 'bug', text: 'bug', stat: 7 }
            { key: 'comment', text: 'comment', stat: 7 }
            { key: 'convention', text: 'convention', stat: 6 }
            { key: 'error-handling', text: 'error-handling', stat: 5 }
          ]
        }
        {
          name: 'Repositories'
          property: 'repositories'
          values: [
            { key: 'squid', text: 'SonarQube', stat: 57 }
            { key: 'pmd', text: 'PMD', stat: 17 }
          ]
        }
      ]




  # GET /api/codingrules/show
  jQuery.mockjax
    url: "#{baseUrl}/api/codingrules/show"
    responseText: JSON.stringify
      codingrule:
        name: 'Array designators "[]" should be located after the type in method signatures'
        language: 'Java'
        creationDate: '2013-10-15'
        fCreationDate: 'Oct 15, 2013'
        status: 'DEPRECATED'
        repositoryName: 'SonarQube'
        repositoryKey: 'squid'
        characteristic: 'Reliability'
        subcharacteristic: 'Data related reliability'
        key: 'S1190'
        parameters: [
          { key: 'someParameter', type: 'INT', default: 4, description: 'Some parameter description' }
          { key: 'xpath', type: 'TEXT', description: 'XPath, the XML Path Language, is a query language for selecting nodes from an XML document. In addition, XPath may be used to compute values (e.g., strings, numbers, or Boolean values) from the content of an XML document. XPath was defined by the World Wide Web Consortium (W3C).' }
        ]
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
        extra: '''This note is here <b>only for test purposes</b>.'''
        extraRaw: '''This note is here *only for test purposes*.'''

        qualityProfiles: [
          {
            name: 'SonarWay'
            key: 'sonarway'
            severity: 'MINOR'
            parameters: [
              { key: 'someParameter', value: 8 }
              { key: 'xpath', value: '/child::html/child::body/child::*/child::span[attribute::class]' }
            ]
          },
          {
            name: 'Quality Profile 1'
            key: 'qualityprofile1'
            severity: 'MAJOR'
            parameters: [
              { key: 'someParameter', value: 6 }
              { key: 'xpath', value: '/html/body/*/span[@class]' }
            ]
            inherits: 'sonarway'
          }
        ]



  # POST /api/codingrules/extend_description
  jQuery.mockjax
    url: "#{baseUrl}/api/codingrules/extend_description"
    responseText: JSON.stringify
      extra: '''This note is here <i>only for test purposes</i>.'''
      extraRaw: '''This note is here *only for test purposes*.'''


  # POST /api/codingrules/bulk_change
  jQuery.mockjax
    url: "#{baseUrl}/api/codingrules/bulk_change"


  # POST /api/codingrules/set_tags
  jQuery.mockjax
    url: "#{baseUrl}/api/codingrules/set_tags"


  # POST /api/codingrules/activate
  jQuery.mockjax
    url: "#{baseUrl}/api/codingrules/activate"


  # POST /api/codingrules/note
  jQuery.mockjax
    url: "#{baseUrl}/api/codingrules/note"
    responseText: JSON.stringify
      note:
        username: 'Admin Admin'
        html: '''<p>This note is here <b>only for test purposes</b>.</p>'''
        raw: '''This note is here *only for test purposes*.'''
        fCreationDate: 'less than a minute'


  # GET /api/qualityprofiles/list
  jQuery.mockjax
    url: "#{baseUrl}/api/qualityprofiles/list"
    responseText: JSON.stringify
      more: false
      results: [
        { id: 'sonarway', text: 'Sonar Way', category: 'Java', parent: null },
        { id: 'qp1', text: 'Quality Profile 1', category: 'Java', parent: 'sonarway' },
        { id: 'qp2', text: 'Quality Profile 2', category: 'JavaScript', parent: 'sonarway' },
        { id: 'qp3', text: 'Quality Profile 3', category: 'Java', parent: null },
      ]


  # GET /api/qualityprofiles/show
  jQuery.mockjax
    url: "#{baseUrl}/api/qualityprofiles/show"
    responseText: JSON.stringify
      qualityprofile:
        id: 'sonarway', text: 'Sonar Way', category: 'Java', parent: null

