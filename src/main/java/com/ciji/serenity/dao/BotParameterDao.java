package com.ciji.serenity.dao;

import com.ciji.serenity.model.BotParam;

import org.springframework.data.repository.CrudRepository;

public interface BotParameterDao extends CrudRepository<BotParam, String> {
    
}
