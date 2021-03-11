package com.ciji.demo.dao;

import com.ciji.demo.model.BotParam;

import org.springframework.data.repository.CrudRepository;

public interface BotParameterDao extends CrudRepository<BotParam, String> {
    
}
