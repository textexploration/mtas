# Heatmap

Mtas can compute geographical distribution for Mtas queries. To get these heatmaps, in Solr requests, besides the parameter to enable the [Mtas query component](search_component.html), the following parameter should be provided.

| Parameter             | Value  | Obligatory  |
|-----------------------|--------|-------------|
| mtas.heatmap            | true   | yes         |

Multiple heatmaps can be produced within the same request. To distinguish them, a unique identifier has to be provided for each of the required document results.

| Parameter                                       | Value        | Info                           | Obligatory  |
|-------------------------------------------------|--------------|--------------------------------|-------------|
| mtas.heatmap.\<identifier\>.key         | \<string\>   | key used in response           | no          |


## Queries

One or multiple queries on the defined Mtas field have to be defined

| Parameter                                       | Value        | Info                           | Obligatory  |
|-------------------------------------------------|--------------|--------------------------------|-------------|
| mtas.heatmap.\<identifier\>.query.\<identifier query\>.type       | \<string\>   | query language: [cql](search_cql.html)  | yes         |
| mtas.heatmap.\<identifier\>.query.\<identifier query\>.value      | \<string\>   | query: [cql](search_cql.html)            | yes         |
| mtas.heatmap.\<identifier\>.query.\<identifier query\>.prefix     | \<string\>   | default prefix            | no         |
| mtas.heatmap.\<identifier\>.query.\<identifier query\>.ignore      | \<string\>   | ignore query: [cql](search_cql.html)            | no         |
| mtas.heatmap.\<identifier\>.query.\<identifier query\>.maximumIgnoreLength      | \<integer\>   | maximum number of succeeding occurrences to ignore            | no         |

### Variables

The query may contain one or more variables, and the value(s) of these variables have to be defined 

| Parameter                                       | Value        | Info                           | Obligatory  |
|-------------------------------------------------|--------------|--------------------------------|-------------|
| mtas.heatmap.\<identifier\>.query.\<identifier query\>.variable.\<identifier variable\>.name      | \<string\>   | name of variable                 | yes        |
| mtas.heatmap.\<identifier\>.query.\<identifier query\>.variable.\<identifier variable\>.value      | \<string\>   | comma separated list of values  | yes        |

## Geospatial

One geospatial field has to be defined, and like for the Solr facet.heatmap several parameters can be configured. 

| Parameter                                       | Value        | Info                           | Obligatory  |
|-------------------------------------------------|--------------|--------------------------------|-------------|
| mtas.heatmap.\<identifier\>.heatmapField       | \<string\>   | Geospatial field                      | yes         |
| mtas.heatmap.\<identifier\>.geom       | \<string\>   | The region to compute the heatmap on, specified using the rectangle-range syntax or WKT. It defaults to the world.                       | no         |
| mtas.heatmap.\<identifier\>.gridLevel      | \<integer\>   | A specific grid level, which determines how big each grid cell is. Defaults to being computed via distErrPct (or distErr).                     | no         |
| mtas.heatmap.\<identifier\>.distErrPct      | \<double\>   | A fraction of the size of geom used to compute gridLevel. Defaults to 0.15. It’s computed the same as a similarly named parameter for RPT.                    | no         |
| mtas.heatmap.\<identifier\>.distErr     | \<double\>   | A cell error distance used to pick the grid level indirectly. It’s computed the same as a similarly named parameter for RPT.                    | no         |
| mtas.heatmap.\<identifier\>.maxCells    | \<integer\>   | The maximum number of cells.                    | no         |

# Statistics

Optionally, the type of statistics and minimum/maximum number of occurrences can be defined.

| Parameter                                       | Value        | Info                           | Obligatory  |
|-------------------------------------------------|--------------|--------------------------------|----|
|mtas.heatmap.\<identifier\>.type      | \<string\>   | required [type of statistics](search_stats.html) | no  |
| mtas.heatmap.\<identifier\>.minimum     | \<double\>   | minimum number of occurrences span  | no          |
| mtas.heatmap.\<identifier\>.maximum     | \<double\>   | maximum number of occurrences span  | no          |



### Functions

To compute statistics for values based on the occurrence of one or multiple spans, optionally [functions](search_functions.html) can be added. The parameters for these functions are the number of occurrences *$q0*, *$q1*, ... for each query and the number of positions *$n* in a document. Statistics on the value computed for each document in the set are added to the response.

| Parameter                                       | Value        | Info                           | Obligatory  |
|-------------------------------------------------|--------------|--------------------------------|-------------|
| mtas.heatmap.\<identifier\>.function.\<identifier function\>.key       | \<string\>   | key used in response                    | no         |
| mtas.heatmap.\<identifier\>.function.\<identifier function\>.expression       | \<string\>   | see [functions](search_functions.html)       | yes        |
| mtas.heatmap.\<identifier\>.function.\<identifier function\>.type      | \<string\>   | required [type of statistics](search_stats.html)                   | no         |

The key is added to the response and may be used to distinguish between multiple functions, and should therefore be unique within the set of functions for each heatmap.

---

## Examples
1. [Basic](#basic) : TODO
2. [Variable](#variable) : TODO
3. [Function](#function) : TODO

---

<a name="basic"></a>  

### Basic

**Example**  
Heatmap for CQL query `[pos="N"]`.

**Request and response**  
`q=*:*&wt=json&indent=true`

``` json
"mtas":{}
```

<a name="variable"></a>  

### Variable

**Example**  
Heatmap for CQL query `[pos=$1]` with `$1` equal to `N,ADJ`.

**Request and response**  
`q=*:*&wt=json&indent=true`

``` json
"mtas":{}
```

<a name="function"></a>  

### Function

**Example**  
Heatmap for CQL query `[pos="N"]` with function.

**Request and response**  
`q=*:*&wt=json&indent=true`

``` json
"mtas":{}
```


**Lucene**

To produce facets on metadata [directly in Lucene](installation_lucene.html), *ComponentHeatmap* together with the provided *collect* method can be used.