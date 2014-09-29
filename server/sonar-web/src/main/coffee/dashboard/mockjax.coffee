define ['third-party/jquery.mockjax'], ->

  jQuery.mockjaxSettings.contentType = 'text/json';
  jQuery.mockjaxSettings.responseTime = 250;

  jQuery.mockjax
    url: "#{baseUrl}/api/dashboards/details"
    responseText: JSON.stringify
      name: 'Helicopter View'
      description: ''
      shared: true
      layout: '50%-50%'

      canManageWidgets: true

      widgets: [
        {
          id: 1
          key: 'measure_filter_list'
          name: 'Measure Filter as List'
          properties: [
            {
              key: 'filter'
              value: 48
            }
          ]
          layout: {
            column: 1
            row: 1
          }
        }
        {
          id: 2
          key: 'my_reviews'
          name: 'My Unresolved Issues'
          properties: [
            {
              key: 'numberOfLines'
              value: 5
            }
          ]
          layout: {
            column: 1
            row: 2
          }
        }
        {
          id: 3
          key: 'hotspot_most_violated_rules'
          name: 'Most Violated Rules'
          properties: [
            {
              key: 'numberOfLines'
              value: 5
            }
          ]
          layout: {
            column: 2
            row: 1
          }
        }
      ]

  jQuery.mockjax
    url: "#{baseUrl}/api/dashboards/widgets"
    responseText: JSON.stringify
      widgets: [
        {
          key: 'action_plans'
          name: 'Action Plans'
          description: 'Shows all the open action plans of the project.'
          category: ''
        }
        {
          key: ''
          name: ''
          description: ''
          category: ''
        }
        {
          key: ''
          name: ''
          description: ''
          category: ''
        }
        {
          key: ''
          name: ''
          description: ''
          category: ''
        }
        {
          key: ''
          name: ''
          description: ''
          category: ''
        }
        {
          key: ''
          name: ''
          description: ''
          category: ''
        }
        {
          key: ''
          name: ''
          description: ''
          category: ''
        }
      ]


  jQuery.mockjax
    url: "#{baseUrl}/api/dashboards/configure_widget"
    responseText: JSON.stringify
      "widget": {
        "key": "measure_filter_list",
        "properties": [
          {
            "key": "filter",
            "value": 48,
            "type": "FILTER",
            "optional": false,
            "options": {
              "39": "My favourites"
              "86": "Nemo &amp; Dory"
              "36": "New recent issues by developer"
              "37": "New recent violations by project"
              "54": "Projects"
              "50": "Projects Activity since last version"
              "38": "Projects Treemap"
              "49": "Projects by license"
              "100": "Projects not analyzed since 2 days"
              "82": "Quality Gate"
              "57": "Super Heroes with new issues"
              "32": "Super Heroes"
              "48": "Teams"
            }
          },
          {
            "key": "pageSize",
            "type": "INTEGER",
            "defaultValue": "30",
            "optional": true
          },
          {
            "key": "displayFilterDescription",
            "type": "BOOLEAN",
            "defaultValue": "false",
            "optional": true
          }
        ]
      }


    jQuery.mockjax
      url: "#{baseUrl}/api/dashboards/save_widget"
      responseText: JSON.stringify
        widget: {
          key: 'measure_filter_list'
          name: 'Measure Filter as List'
          properties: [
            {
              key: 'filter'
              value: 48
            }
          ]
          layout: {
            column: 1
            row: 1
          }
        }


        jQuery.mockjax
          url: "#{baseUrl}/api/dashboards/save"
          responseText: JSON.stringify
            status: 'ok'
