package com.ciji.serenity.repository;

import java.util.Optional;

import org.springframework.cloud.gcp.data.firestore.FirestoreReactiveRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.ciji.serenity.model.CharacterSheetDetails;


@Repository
public interface CharacterSheetDetailsRepository extends FirestoreReactiveRepository<CharacterSheetDetails> {

    Optional<CharacterSheetDetails> findByName(String name);
}
