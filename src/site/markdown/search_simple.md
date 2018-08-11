# Simple Query Language

See also the [cql](search_cql.html) query language. 

This query language provides a limited but possibly more familiar alternative for [cql](search_cql.html). Search terms can be entered directly, and specific patterns can be defined by using an approach similar to conventions used in modern search engines.

For each *Simple* query, alway a field and (default) prefix have to be defined.

## Comparison Simple and CQL

Assume the default *prefix* for the *Simple* request is *t*

| Simple | CQL   | Description |
|--------|-------|-------------|
| `koe` | `[t="koe"]`| Matches a single position token |
| `"de koe"` | `[t="de"][t="koe"]`| Matches a sequence of single position tokens |
| `koe paard` | `[t="koe"]` and `[t="paard"]` | Matches multiple single position tokens |
| `"de koe" paard` | `[t="de"][t="koe"]` and `[t="paard"]` | Sequence combined with additional condition |
| `lemma:N` | `[pos="N"]`| Search non-default prefix |
| `"lemma:ADJ koe"` | `[pos="ADJ"] [t="koe"]`| Search on multiple prefixes in a sequence |

