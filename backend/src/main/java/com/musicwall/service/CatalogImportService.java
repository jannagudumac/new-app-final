package com.musicwall.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicwall.dto.CatalogImportResultDTO;
import com.musicwall.dto.CuratedArtistDTO;
import com.musicwall.exception.BusinessException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class CatalogImportService {

    private final MusicBrainzService musicBrainzService;
    private final ObjectMapper objectMapper;

    public CatalogImportService(
            MusicBrainzService musicBrainzService,
            ObjectMapper objectMapper
    ) {
        this.musicBrainzService = musicBrainzService;
        this.objectMapper = objectMapper;
    }

    public CatalogImportResultDTO importConfiguredArtists() {
        CatalogImportResultDTO total = new CatalogImportResultDTO();

        for (CuratedArtistDTO artist : readArtists()) {
            try {
                CatalogImportResultDTO imported = musicBrainzService.importArtistCatalogue(
                        artist.getMusicBrainzId(),
                        artist.getName(),
                        artist.getMaxAlbums()
                );
                if (imported.getArtists() > 0) total.addArtist();
                total.addAlbums(imported.getAlbums());
                total.addTracks(imported.getTracks());
                imported.getErrors().forEach(total::addError);
            } catch (BusinessException exception) {
                total.addError(artist.getName() + ": MusicBrainz was unavailable");
            }
        }
        return total;
    }

    List<CuratedArtistDTO> readArtists() {
        try {
            return objectMapper.readValue(
                    new ClassPathResource("catalog-artists.json").getInputStream(),
                    new TypeReference<>() { }
            );
        } catch (IOException exception) {
            throw new BusinessException("Could not read the curated artist list");
        }
    }
}
