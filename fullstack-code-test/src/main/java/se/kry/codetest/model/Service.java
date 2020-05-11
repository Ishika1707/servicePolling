package se.kry.codetest.model;

import java.time.Instant;

public class Service {
    private final String name;
    private final String url;
    private final String status;
    private final Instant createdAt;


    public Service(String name, String url, String status, Instant createdAt) {
        this.name = name;
        this.url = url;
        this.status = status;
        this.createdAt = createdAt;
    }


    public String getName() {
        return name;
    }

    public String getUrl(){ return url; };

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }


}
