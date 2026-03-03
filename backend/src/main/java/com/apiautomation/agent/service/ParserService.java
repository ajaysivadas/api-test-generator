package com.apiautomation.agent.service;

import com.apiautomation.agent.model.ApiSpec;

public interface ParserService {
    boolean canParse(String content, String filename);
    ApiSpec parse(String content, String filename);
}
