package com.ciji.serenity.dao;

import com.ciji.serenity.model.CharacterSheet;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharacterSheetDao extends CrudRepository<CharacterSheet, String> {

    CharacterSheet findByName(String name);

    CharacterSheet findByNameAndOwnerId(String name, String ownerId);

    List<CharacterSheet> findAllByOwnerId(String ownerId);
}
