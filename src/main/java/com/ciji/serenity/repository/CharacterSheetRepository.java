package com.ciji.serenity.repository;

import com.ciji.serenity.model.CharacterSheet;
import org.springframework.cloud.gcp.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CharacterSheetRepository extends FirestoreReactiveRepository<CharacterSheet> {

    Mono<CharacterSheet> findByName(String name);

    Mono<CharacterSheet> findByNameAndOwnerId(String name, String ownerId);

    Flux<CharacterSheet> findAllByOwnerId(String ownerId);
}
