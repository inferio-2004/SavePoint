package com.example.Savepoint.User.Search;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

@Document(indexName = "users")
@Setting(settingPath = "elasticsearch/settings.json")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "autocomplete_analyzer", searchAnalyzer = "standard")
    private String displayName;

    @Field(type = FieldType.Keyword)
    private String avatarUrl;
}
