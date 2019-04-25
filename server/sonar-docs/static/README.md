These three `*.NavigationTree.json` files control the navigation trees of the three versions of the documentation.

Each one contains a JSON array. 

**Array elements may either be:**

* a path string
* a node

**Nodes contain two elements:**  
* title - string. This is the "parent" node name to be used in the navigation tree
* children - array 

**Children may either be:**  
* path string
* node

e.g.
```
  {
    "title": "Analyzing Source Code",
    "children": [
      "/analysis/overview/",
      "/analysis/analysis-parameters/",
      "/analysis/coverage/",
      "/analysis/external-issues/",
      "/analysis/background-tasks/",
      "/analysis/generic-issue/",
      "/analysis/generic-test/",
      "/analysis/pull-request/",
      {
        "title": "Sub child",
        "children": [
          "/analysis/supported-languages/",
          {
            "title": "Sub sub child",
            "children": ["/analysis/background-tasks/"]
          }
    ]
  }
```


**Paths**
* begin with '/'
* end with '/'
* match the `url:` value of a page. 
* **do not** include the trailing `.md` in the file name

**What is the URL value of a page?**  
The url value can be implicitly defined by the document's path under the `pages` directory, or explicitly overridden by in the page metadata by setting `url: [path here]`.

Paths must always start and end with '/'. That includes:
* page metadata
* navigation tree files
* links between pages
