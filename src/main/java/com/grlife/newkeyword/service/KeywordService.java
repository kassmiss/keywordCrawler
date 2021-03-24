package com.grlife.newkeyword.service;

import com.grlife.newkeyword.repository.KeywordRepository;

public class KeywordService {

    private final KeywordRepository keywordRepository;

    public KeywordService(KeywordRepository keywordRepository) {
        this.keywordRepository = keywordRepository;
    }

    public void startKeyword() {
        keywordRepository.startKeyword();
    }

    public void reset() {
        keywordRepository.reset();
    }
}
