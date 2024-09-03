package com.ciji.serenity.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;

@Data
@RedisHash
public class CharacterSheet implements Serializable {

    @Id
    @ToString.Include
    private String id;

    @NotNull
    @Size(min = 3, max = 25)
    @ToString.Include
    @Indexed
    private String name;

    @NotNull
    @Indexed
    private String ownerId;
}
