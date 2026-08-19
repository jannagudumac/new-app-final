package com.musicwall.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.musicwall.dto.ConcertDTO;
import com.musicwall.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConcertService {
    private final RestClient restClient = RestClient.create("https://app.ticketmaster.com/discovery/v2");
    @Value("${ticketmaster.api-key:}") private String apiKey;
    public List<ConcertDTO> search(String artist, String city) {
        if (apiKey == null || apiKey.isBlank()) throw new BusinessException("Concert search is not configured. Add TICKETMASTER_API_KEY to backend/.env");
        try {
            JsonNode root = restClient.get().uri(builder -> {
                builder.path("/events.json").queryParam("apikey", apiKey).queryParam("keyword", artist)
                        .queryParam("classificationName", "music").queryParam("size", 12).queryParam("sort", "date,asc");
                if (city != null && !city.isBlank()) builder.queryParam("city", city.trim());
                return builder.build();
            }).retrieve().body(JsonNode.class);
            List<ConcertDTO> result = new ArrayList<>();
            JsonNode events = root == null ? null : root.path("_embedded").path("events");
            if (events == null || !events.isArray()) return result;
            events.forEach(event -> {
                ConcertDTO dto = new ConcertDTO(); dto.setName(event.path("name").asText());
                dto.setDate(event.path("dates").path("start").path("localDate").asText());
                JsonNode venue = event.path("_embedded").path("venues").path(0);
                dto.setVenue(venue.path("name").asText()); dto.setCity(venue.path("city").path("name").asText());
                dto.setUrl(event.path("url").asText()); result.add(dto);
            });
            return result;
        } catch (Exception exception) {
            throw new BusinessException("The concert provider is temporarily unavailable");
        }
    }
}
