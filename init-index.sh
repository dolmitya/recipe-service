#!/bin/bash

curl -X PUT "http://elasticsearch:9200/products" -H 'Content-Type: application/json' -d'
{
  "settings": {
    "analysis": {
      "filter": {
        "ru_synonyms": {
          "type": "synonym",
          "synonyms_path": "synonyms/ru_synonyms.txt"
        }
      },
      "analyzer": {
        "ru_synonym_analyzer": {
          "tokenizer": "standard",
          "filter": ["lowercase", "ru_synonyms"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "name": {
        "type": "text",
        "analyzer": "ru_synonym_analyzer"
      },
      "unit": {
        "type": "keyword"
      }
    }
  }
}'