package com.ciji.serenity.repository;

import com.ciji.serenity.model.CharacterSheet;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CharacterSheetRepository extends CrudRepository<CharacterSheet, String> {

    Optional<CharacterSheet> findByName(String name);

    CharacterSheet findByNameAndOwnerId(String name, String ownerId);

    List<CharacterSheet> findAllByOwnerId(String ownerId);
}
