package com.example.Savepoint.Game.Search;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Document(indexName = "games")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private List<String> genres;

    @Field(type = FieldType.Keyword)
    private List<String> platforms;

    @Field(type = FieldType.Keyword)
    private String coverThumb;

    @Field(type = FieldType.Date)
    private LocalDate releaseDate;

    @Field(type = FieldType.Keyword)
    private List<String> developers;
}