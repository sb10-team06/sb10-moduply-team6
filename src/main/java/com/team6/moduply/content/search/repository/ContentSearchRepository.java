package com.team6.moduply.content.search.repository;

import com.team6.moduply.content.search.document.ContentSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ContentSearchRepository extends ElasticsearchRepository<ContentSearchDocument, String> {
}
