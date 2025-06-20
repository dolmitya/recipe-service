#!/bin/bash

curl -X PUT "localhost:9200/products" -H 'Content-Type: application/json' -d'
{
  "settings": {
    "analysis": {
      "filter": {
        "russian_morphology": {
          "type": "morphology",
          "language": "russian"
        },
        "synonym_filter": {
          "type": "synonym",
          "synonyms_path": "synonyms/synonyms.txt"
        }
      },
      "analyzer": {
        "custom_russian": {
          "tokenizer": "standard",
          "filter": [
            "lowercase",
            "russian_morphology",
            "synonym_filter"
          ]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "name": {
        "type": "text",
        "analyzer": "custom_russian"
      }
    }
  }
}
'
