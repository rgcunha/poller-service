package se.kry.codetest;

import java.net.URL;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

//TODO: Use POJO to deserialise ResultSet
@JsonIgnoreProperties
public class BackendService {
    private final Integer id;
    private String name;
    private String status;
    private URL url;

    @JsonCreator
    BackendService(
        @JsonProperty("id") Integer id,
        @JsonProperty("name") String name,
        @JsonProperty("status") String status,
        @JsonProperty("url") URL url
    ) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.url = url;
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getStatus() {
        return this.status;
    }
    
    public URL getUrl() {
        return this.url;
    }
}
