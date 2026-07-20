package com.team6.moduply.content.search.document;

import com.team6.moduply.content.enums.ContentType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Document(indexName = "contents", createIndex = false)
public class ContentSearchDocument {

  @Id
  private String id;

  @Field(type = FieldType.Keyword)
  private String externalApiId;

  @Field(type = FieldType.Keyword)
  private ContentType type;

  @MultiField(
      mainField = @Field(type = FieldType.Text, analyzer = "standard"),
      otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
  )
  private String title;

  @Field(type = FieldType.Text, analyzer = "standard")
  private String description;

  @MultiField(
      mainField = @Field(type = FieldType.Text, analyzer = "standard"),
      otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
  )
  private List<String> tags;

  @Field(type = FieldType.Double)
  private BigDecimal averageRating;

  @Field(type = FieldType.Integer)
  private int reviewCount;

  @Field(type = FieldType.Date, format = DateFormat.date_time)
  private Instant createdAt;

  @Field(type = FieldType.Date, format = DateFormat.date_time)
  private Instant updatedAt;
}
