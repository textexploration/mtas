# Index

Mtas can produce indices for Mtas queries within the listed documents. To get this information, in Solr requests, besides the parameter to enable the [Mtas query component](search_component.html), the following parameter should be provided.

| Parameter             | Value  | Obligatory  |
|-----------------------|--------|-------------|
| mtas.index             | true   | yes         |

Index results on multiple spans can be produced within the same request. To distinguish them, a unique identifier has to be provided for each of the required indices. 

| Parameter                                       | Value        | Info                           | Obligatory  |
|-------------------------------------------------|--------------|--------------------------------|-------------|
| mtas.index.\<identifier\>.key         | \<string\>   | key used in response           | no          |
| mtas.index.\<identifier\>.field       | \<string\>   | Mtas field                      | yes         |
| mtas.index.\<identifier\>.query.type       | \<string\>   | query language: [cql](search_cql.html)  | yes         |
| mtas.index.\<identifier\>.query.value      | \<string\>   | query: [cql](search_cql.html)            | yes         |
| mtas.index.\<identifier\>.query.prefix     | \<string\>   | default prefix            | no         |
| mtas.index.\<identifier\>.query.ignore      | \<string\>   | ignore query: [cql](search_cql.html)            | no         |
| mtas.index.\<identifier\>.query.maximumIgnoreLength      | \<integer\>   | maximum number of succeeding occurrences to ignore            | no         |


## Variables

The query may contain one or more variables, and the value(s) of these variables have to be defined 

| Parameter                                       | Value        | Info                           | Obligatory  |
|-------------------------------------------------|--------------|--------------------------------|-------------|
| mtas.index.\<identifier\>.query.variable.\<identifier variable\>.name      | \<string\>   | name of variable                 | yes        |
| mtas.index.\<identifier\>.query.variable.\<identifier variable\>.value      | \<string\>   | comma separated list of values  | yes        |



## Blocks

The index for a document can be seen as counting the number of hits within a certain division of this document into *blocks*. Several types of such a division are possible: by number, size or query. 

### Number of blocks

To divide the document into a fixed number of separate blocks of equal length (as closely as possible), this number has to be provided. The size of the blocks will depend on the size of the document.


| Parameter                                       | Value        | Info                           | Obligatory  |
|-------------------------------------------------|--------------|--------------------------------|-------------|
| mtas.index.\<identifier\>.block.number      | \<integer\>   | number of blocks within the document                 | yes        |

### Size of blocks

To divide the document into separate blocks of fixed length (as far is possible), this size has to be provided. The number of blocks will depend on the size of the document.


| Parameter                                       | Value        | Info                           | Obligatory  |
|-------------------------------------------------|--------------|--------------------------------|-------------|
| mtas.index.\<identifier\>.block.size      | \<integer\>   | size of blocks within the document                 | yes        |

### Query based blocks

To divide the document into separate blocks defined by a query (other than the main query for the index), this query has to be provided. 


| Parameter                                       | Value        | Info                           | Obligatory  |
|-------------------------------------------------|--------------|--------------------------------|-------------|
| mtas.index.\<identifier\>.block.query.type       | \<string\>   | query language: [cql](search_cql.html)  | yes         |
| mtas.index.\<identifier\>.block.query.value      | \<string\>   | query: [cql](search_cql.html)            | yes         |
| mtas.index.\<identifier\>.block.query.prefix     | \<string\>   | default prefix            | no         |
| mtas.index.\<identifier\>.block.query.ignore      | \<string\>   | ignore query: [cql](search_cql.html)            | no         |
| mtas.index.\<identifier\>.block.query.maximumIgnoreLength      | \<integer\>   | maximum number of succeeding occurrences to ignore            | no         |

And of course, this query can contain variables.

| Parameter                                       | Value        | Info                           | Obligatory  |
|-------------------------------------------------|--------------|--------------------------------|-------------|
| mtas.index.\<identifier\>.block.query.variable.\<identifier variable\>.name      | \<string\>   | name of variable                 | yes        |
| mtas.index.\<identifier\>.block.query.variable.\<identifier variable\>.value      | \<string\>   | comma separated list of values  | yes        |

## Match

Also because a match for the query may contain multiple positions, it can occur that a match intersects with multiple blocks. By default, a match is taken into account for this block, if it intersects. However, this behavior can be configured to containing the start position of the match, or to contain the match completely. 


| Parameter                                       | Value        | Info                           | Obligatory  |
|-------------------------------------------------|--------------|--------------------------------|-------------|
| mtas.index.\<identifier\>.match      | \<string\>   | "intersect", "start", or "complete"                       | no       |

## List

Within a block, not only the total number of matches for the query can be listed, but also a list of grouped prefixes for these matches. Typically, this may result in words matching your query. Optionally, the size of the list can be limited by providing a maximum number. Default the list is sorted descending by frequency (count), but sorting by *term frequency–inverse block frequency* (variant of tf-idf) can be used to emphasis block specific words.


| Parameter                                       | Value        | Info                           | Obligatory  |
|-------------------------------------------------|--------------|--------------------------------|-------------|
| mtas.index.\<identifier\>.list.prefix       | \<string\>   | comma separated list of prefixes                      | yes         |
| mtas.index.\<identifier\>.list.number      | \<integer\>   | maximum number of items in the list     | no         |
| mtas.index.\<identifier\>.list.sort      | \<string\>   | "index", "count", "tfibf"     | no         |




---

## Examples
1. [Basic](#basic) : Create an index with the number of adjectives in each block.
2. [Block number](#block-number) : Create an index with the number of adjectives in each block
3. [Block size](#block-size) : Create an index with the number of adjectives in each block
4. [Block query](#block-query) : Create an index with the number of adjectives in each block
5. [List](#list) : Create an index with the number of adjectives in each block
---

<a name="basic"></a>  

### Basic

**Example**  
Create an index with the number of adjectives in each block.

**CQL**  
`[pos="ADJ"]`

**Request and response**  
`q=*:*&mtas=true&mtas.index=true&mtas.index.0.field=text&mtas.index.0.query.type=cql&mtas.index.0.query.value=[pos="ADJ"]&mtas.index.0.key=adjective&fl=id&rows=2&wt=json&indent=true`

```json
"mtas":{
    "index":[{
        "key":"adjective",
        "list":[{
            "documentKey":"e6a7cd11-36d3-494a-b131-eadeb2df77c4",
            "documentMinPosition":0,
            "documentMaxPosition":18437,
            "data":[{
                "positionStart":0,
                "positionEnd":1843,
                "number":67},
              {
                "positionStart":1844,
                "positionEnd":3687,
                "number":38},
                ...
              {
                "positionStart":16596,
                "positionEnd":18437,
                "number":72}]},
          {
            "documentKey":"447616c5-ee0d-4404-a987-73c4b43630ee",
            "documentMinPosition":0,
            "documentMaxPosition":186,
            "data":[{
                "positionStart":0,
                "positionEnd":18,
                "number":1},
              {
                "positionStart":19,
                "positionEnd":37,
                "number":1}, 
                ...
              {
                "positionStart":171,
                "positionEnd":186,
                "number":0}]}]}]}
```

<a name="block-number"></a>  

### Block number

**Example**  
Create an index with the number of adjectives in each block.

**CQL**  
`[pos="ADJ"]`

**Request and response**  
``

```json
```

<a name="block-size"></a>  

### Block size

**Example**  
Create an index with the number of adjectives in each block.

**CQL**  
`[pos="ADJ"]`

**Request and response**  
``

```json
```

<a name="block-query"></a>  

### Block query

**Example**  
Create an index with the number of adjectives in each block.

**CQL**  
`[pos="ADJ"]`

**Request and response**  
``

```json
```

<a name="list"></a>  

### List

**Example**  
Create an index with the number of adjectives in each block.

**CQL**  
`[pos="ADJ"]`

**Request and response**  
``

```json
```

---

##Lucene

To use keywords in context [directly in Lucene](installation_lucene.html), *ComponentIndex* together with the provided *collect* method can be used.
