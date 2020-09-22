package se.kry.codetest;

import java.net.URL;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class Service {
    private final Integer id;
    private String name;
    private String status;
    private URL url;
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Date createdAt;

    @JsonCreator
    Service(
        @JsonProperty("id") Integer id,
        @JsonProperty("name") String name,
        @JsonProperty("status") String status,
        @JsonProperty("url") URL url,
        @JsonProperty("created_at") Date createdAt
    ) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.url = url;
        this.createdAt = createdAt;
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

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    public Date getCreatedAt() {
        return this.createdAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
    public void setUrl(URL url) {
        this.url = url;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
