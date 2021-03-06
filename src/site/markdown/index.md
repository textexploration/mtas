#Multi Tier Annotation Search

In recent years, multiple solutions have come available providing search on huge amounts of plain text and metadata. Scalable searchability on annotated text however still appears to be problematic. Using Mtas, we not only take advantage of the strength from Lucene and Solr, but extend queries with [CQL](search_cql.html) conditions on annotated text

> `[pos="LID"] [pos="ADJ"]? [lemma="amsterdam"]`
>
> `<entity="location/> within (<s/> containing [lemma="utrecht"])`

Also a [Simple](search_simple.html) query parser can be used

> `koe paard`
>
> `"de koe" paard`
>
> `"de pos:ADJ koe"`

Parsers for several [document formats](indexing_formats.html) are provided, each with extended possibilities for [configuration](indexing_configuration.html).

Advanced query [features](features.html) like [statistics](search_component_stats.html), [termvectors](search_component_termvector.html) and [kwic](search_component_kwic.html) are included.

Source code and releases are available on [GitHub](https://github.com/textexploration/mtas/), see [installation instructions](installation.html) on how to get started.

---

This project builds upon the latest commit from April 30, 2018 for [meertensinstituut/mtas](https://github.com/meertensinstituut/mtas/tree/5c862d53014b15fb87de83da0b33fd91518642ec). See also the related [broker](https://github.com/textexploration/broker) project, another continuation of previous work.

---

**Nederlab** 

One of the primary use cases for Mtas, the [Nederlab project](https://www.nederlab.nl/), currently<sup>1</sup> provides access, both in terms of metadata and 
annotated text, to over 74 million items for search and analysis as specified below. 

|                 | Total          | Mean      | Min   | Max        |
|-----------------|---------------:|----------:|------:|-----------:|
| Solr index size | 2,715 G        | 60.3 G    | 75 k  | 288 G      |
| Solr documents  | 74,762,559     | 1,661,390 | 119   | 11,912,415 |

Collections are added and updated regularly by adding new cores, replacing cores and/or merging new cores with existing ones. Currently, the data is divided over 44 separate cores. For 41,437,881 of these documents, annotated text varying in size from 1 to over 3.5 million words is included:

|                 | Total           | Mean         | Min   | Max        |
|-----------------|----------------:|-------------:|------:|-----------:|
| Words           | 18,494,454,357  | 446          | 1     | 3,537,883  |
| Annotations     | 95,921,919,849  | 2,314        | 4     | 23,589,831 |

---

**Middelnederlands** 

Mtas is also used on [Middelnederlands.nl](https://www.middelnederlands.nl/), including geographical selections and new analysis options.<sup>2</sup>

![example document](images/example_document.jpg "Show document")

***Keyword in context*** 

![example kwic](images/example_kwic.jpg "Keyword in context")

***Group results*** 

![example group](images/example_group.jpg "Group results")

***Geographic conditions*** 

![example geographic](images/example_geographic.jpg "Geographic condition")

***Correlation analysis*** 

![example correlation](images/example_correlation.jpg "Correlation analysis")

***Geographical analysis*** 

![example map1](images/example_map_1.jpg "Geographical analysis")

![example map1](images/example_map_2.jpg "Geographical analysis")

---

<sup><a name="footnote">1</a></sup> <small>situation June 2018</small>

<sup><a name="footnote">2</a></sup> <small>release April 2020</small>