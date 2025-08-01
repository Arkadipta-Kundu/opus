package org.arkadipta.opus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class QuoteResponse {
    private String quote;
    private String author;
    private String category;

    // Constructors
    public QuoteResponse() {}

    public QuoteResponse(String quote, String author) {
        this.quote = quote;
        this.author = author;
    }

}
