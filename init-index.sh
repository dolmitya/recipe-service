#!/bin/bash

curl -X PUT "http://elasticsearch:9200/products" -H 'Content-Type: application/json' -d'
{
  "settings": {
    "analysis": {
      "filter": {
        "russian_stop": {
          "type":       "stop",
          "stopwords":  "_russian_"
        },
        "ru_hunspell": {
          "type":   "hunspell",
          "locale": "ru_RU",
          "dedup":  true
        },
        "ru_synonyms": {
          "type":          "synonym_graph",
          "synonyms_path": "synonyms/ru_synonyms.txt"
        },
        "russian_stemmer": {
          "type":     "stemmer",
          "language": "russian"
        }
      },
      "analyzer": {
        "ru_product_analyzer": {
          "tokenizer": "standard",
          "filter": [
            "lowercase",
            "russian_stop",
            "ru_hunspell",
            "ru_synonyms",
            "russian_stemmer"
          ]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "name": {
        "type":     "text",
        "analyzer": "ru_product_analyzer"
      },
      "unit": {
        "type":     "keyword"
      }
    }
  }
}'