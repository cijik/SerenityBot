package com.ciji.serenity.dao;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.ciji.serenity.model.CharacterSheetDetails;


@Repository
public interface CharacterSheetDetailsDao extends CrudRepository<CharacterSheetDetails, String> {

    Optional<CharacterSheetDetails> findByName(String name);
}
