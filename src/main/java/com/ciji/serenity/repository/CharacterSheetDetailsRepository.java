package com.ciji.serenity.repository;

import com.ciji.serenity.model.CharacterSheetDetails;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface CharacterSheetDetailsRepository extends CrudRepository<CharacterSheetDetails, String> {

    Optional<CharacterSheetDetails> findByName(String name);
}
