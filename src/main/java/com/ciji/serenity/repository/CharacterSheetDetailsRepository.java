package com.ciji.serenity.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.ciji.serenity.model.CharacterSheetDetails;


@Repository
public interface CharacterSheetDetailsRepository extends CrudRepository<CharacterSheetDetails, String> {

    Optional<CharacterSheetDetails> findByName(String name);
}
