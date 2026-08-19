package com.musicwall.service;

import com.musicwall.dto.CatalogImportResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class CatalogImportRunner implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogImportRunner.class);

    private final CatalogImportService catalogImportService;
    private final boolean importEnabled;
    private final boolean exitAfterImport;
    private final ConfigurableApplicationContext applicationContext;

    public CatalogImportRunner(
            CatalogImportService catalogImportService,
            ConfigurableApplicationContext applicationContext,
            @Value("${catalog.import.enabled:false}") boolean importEnabled,
            @Value("${catalog.import.exit-after-run:false}") boolean exitAfterImport
    ) {
        this.catalogImportService = catalogImportService;
        this.applicationContext = applicationContext;
        this.importEnabled = importEnabled;
        this.exitAfterImport = exitAfterImport;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!importEnabled) return;

        LOGGER.info("Starting curated MusicBrainz catalogue import");
        CatalogImportResultDTO result = catalogImportService.importConfiguredArtists();
        LOGGER.info(
                "Catalogue import finished: {} artists, {} albums, {} tracks, {} warnings",
                result.getArtists(), result.getAlbums(), result.getTracks(), result.getErrors().size()
        );
        result.getErrors().forEach(error -> LOGGER.warn("Catalogue import: {}", error));
        if (exitAfterImport) applicationContext.close();
    }
}
