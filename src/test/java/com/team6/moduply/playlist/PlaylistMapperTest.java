package com.team6.moduply.playlist;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.team6.moduply.content.enums.ContentType;
import com.team6.moduply.playlist.dto.PlaylistDto;
import com.team6.moduply.playlist.entity.Playlist;
import com.team6.moduply.playlist.mapper.PlaylistMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

class PlaylistMapperTest {

  private final PlaylistMapper playlistMapper = new PlaylistMapper();

  @Test
  @DisplayName("플레이리스트 DTO의 리스트 구현체를 Redis 캐시 역직렬화 가능한 타입으로 정규화한다")
  void to_dto_normalizes_list_implementations_for_redis_cache() {
    Playlist playlist = Playlist.builder()
        .ownerId(UUID.randomUUID())
        .title("제목")
        .description("설명")
        .build();

    PlaylistDto.ContentSummaryDto content = new PlaylistDto.ContentSummaryDto(
        UUID.randomUUID(),
        ContentType.movie,
        "콘텐츠",
        "콘텐츠 설명",
        "https://example.com/image.png",
        List.of("tag"),
        4.5,
        10
    );

    PlaylistDto dto = playlistMapper.toDto(
        playlist,
        null,
        0L,
        false,
        List.of(content)
    );

    GenericJackson2JsonRedisSerializer serializer =
        new GenericJackson2JsonRedisSerializer(cacheObjectMapper());

    byte[] serialized = serializer.serialize(dto);
    Object deserialized = serializer.deserialize(serialized);

    assertThat(dto.contents()).isInstanceOf(ArrayList.class);
    assertThat(dto.contents().get(0).tags()).isInstanceOf(ArrayList.class);
    assertThat(deserialized).isInstanceOf(PlaylistDto.class);
  }

  private ObjectMapper cacheObjectMapper() {
    BasicPolymorphicTypeValidator typeValidator =
        BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.team6.moduply.")
            .allowIfSubType(ArrayList.class)
            .allowIfSubType(UUID.class)
            .allowIfSubType(LocalDateTime.class)
            .allowIfSubType(Instant.class)
            .allowIfSubType(Long.class)
            .allowIfSubType(Integer.class)
            .allowIfSubType(Double.class)
            .allowIfSubType(Boolean.class)
            .allowIfSubType(BigDecimal.class)
            .build();

    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    objectMapper.activateDefaultTyping(
        typeValidator,
        ObjectMapper.DefaultTyping.EVERYTHING,
        JsonTypeInfo.As.PROPERTY
    );
    return objectMapper;
  }
}
