package com.team6.moduply.content.service;

import com.team6.moduply.content.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentService {

  private final ContentRepository contentRepository;
}
