# Page

Mtas can produce pages for the listed documents. To get this information, in Solr requests, besides the parameter to enable the [Mtas query component](search_component.html), the following parameter should be provided.

| Parameter             | Value  | Obligatory  |
|-----------------------|--------|-------------|
| mtas.page             | true   | yes         |

Multiple pages can be generated from the same request. To distinguish them, a unique identifier has to be provided for each of the required pages. 

| Parameter                                       | Value        | Info                           | Obligatory  |
|-------------------------------------------------|--------------|--------------------------------|-------------|
| mtas.page.\<identifier\>.key         | \<string\>   | key used in response           | no          |
| mtas.page.\<identifier\>.field       | \<string\>   | Mtas field                      | yes         |
| mtas.page.\<identifier\>.start       | \<double\>   | start position for the page                       | yes         |
| mtas.page.\<identifier\>.end       | \<double\>   | end position for the page                       | yes         |
| mtas.page.\<identifier\>.prefix       | \<string\>   | comma separated list of prefixes                      | no         |


The output for each document is divided into the three position types of Mtas tokens: *word*, *range* and *set* and has this structure:

```json
"word" : [
  <position> : [
                 [ <tokenId>, <prefix>, <postfix>, parentId ],
                 ...
               ],
], "range" : [
  <startPosition> : [
                 [ <tokenId>, [<startPosition>, <endPosition>], <prefix>, <postfix>, parentId ],
                 ...
               ],
], "set" : [
  <startPosition> : [
                 [ <tokenId>, [<position>, <position>, ...], <prefix>, <postfix>, parentId ],
                 ...
               ],
]               
```
             
where *parentId* is only included when available, and *postfix* when available or needed to include *parentId*.

---

## Examples
1. [Page](#page) : Page with prefix *lemma* and *pos* from position *100* to *200*
---

<a name="page"></a>  

### Token

**Example**  
Page with prefixes *lemma* and *pos* from position *100* to *200*


**Request and response**  
`q=%2A%3A%2A&mtas=true&mtas.page=true&mtas.page.0.field=text&mtas.page.0.prefix=lemma%2Cpos&mtas.page.0.start=100&mtas.page.0.end=200&fl=%2A&start=0&rows=1&wt=json&indent=true`

```json
"mtas":{
    "page":[{
        "key":"0",
        "list":[{
            "documentKey":"e6a7cd11-36d3-494a-b131-eadeb2df77c4",
            "documentMinPosition":0,
            "documentMaxPosition":18437,
            "data":{
              "word":{
                "100":[[1014,
                    "lemma",
                    "de"],
                  [1016,
                    "pos",
                    "LID"]],
                "101":[[1024,
                    "lemma",
                    "wol"],
                  [1026,
                    "pos",
                    "N"]],
                ...
                "199":[[2018,
                    "lemma",
                    "het"],
                  [2020,
                    "pos",
                    "LID"]],
                "200":[[2027,
                    "lemma",
                    "garen"],
                  [2029,
                    "pos",
                    "N"]]}}}]}]}
```

---


##Lucene

To use keywords in context [directly in Lucene](installation_lucene.html), *ComponentPage* together with the provided *collect* method can be used.
