package com.grlife.newkeyword;

import com.grlife.newkeyword.repository.KeywordRepository;
import com.grlife.newkeyword.repository.KeywordRepositoryImpl;
import com.grlife.newkeyword.service.KeywordService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class SpringConfig {

    private final DataSource dataSource;

    public SpringConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
    public KeywordService KeywordService() {
        return new KeywordService(KeywordRepository());
    }

    @Bean
    public KeywordRepository KeywordRepository() {
        //return new MemoryMemberRepository();
        return new KeywordRepositoryImpl(dataSource);
    }

}