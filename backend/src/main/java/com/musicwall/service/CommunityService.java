package com.musicwall.service;

import com.musicwall.dto.*;
import com.musicwall.entity.*;
import com.musicwall.exception.BusinessException;
import com.musicwall.exception.ResourceNotFoundException;
import com.musicwall.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

import java.util.*;

@Service
public class CommunityService {
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;
    private static final Set<String> AVATAR_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private final UserRepository userRepository;
    private final TrackRepository trackRepository;
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final TrackFavouriteRepository trackFavouriteRepository;
    private final AlbumFavouriteRepository albumFavouriteRepository;
    private final ArtistFavouriteRepository artistFavouriteRepository;

    public CommunityService(UserRepository userRepository, TrackRepository trackRepository,
                            AlbumRepository albumRepository, ArtistRepository artistRepository,
                            TrackFavouriteRepository trackFavouriteRepository,
                            AlbumFavouriteRepository albumFavouriteRepository,
                            ArtistFavouriteRepository artistFavouriteRepository) {
        this.userRepository = userRepository;
        this.trackRepository = trackRepository;
        this.albumRepository = albumRepository;
        this.artistRepository = artistRepository;
        this.trackFavouriteRepository = trackFavouriteRepository;
        this.albumFavouriteRepository = albumFavouriteRepository;
        this.artistFavouriteRepository = artistFavouriteRepository;
    }

    @Transactional
    public void addFavourite(String username, String type, Long itemId) {
        UserEntity user = findUser(username);
        switch (type) {
            case "tracks" -> {
                if (trackFavouriteRepository.existsByUserUsernameAndTrackId(username, itemId)) return;
                TrackFavouriteEntity favourite = new TrackFavouriteEntity();
                favourite.setUser(user);
                favourite.setTrack(trackRepository.findById(itemId)
                        .orElseThrow(() -> new ResourceNotFoundException("Track not found")));
                trackFavouriteRepository.save(favourite);
            }
            case "albums" -> {
                if (albumFavouriteRepository.existsByUserUsernameAndAlbumId(username, itemId)) return;
                AlbumFavouriteEntity favourite = new AlbumFavouriteEntity();
                favourite.setUser(user);
                favourite.setAlbum(albumRepository.findById(itemId)
                        .orElseThrow(() -> new ResourceNotFoundException("Album not found")));
                albumFavouriteRepository.save(favourite);
            }
            case "artists" -> {
                if (artistFavouriteRepository.existsByUserUsernameAndArtistId(username, itemId)) return;
                ArtistFavouriteEntity favourite = new ArtistFavouriteEntity();
                favourite.setUser(user);
                favourite.setArtist(artistRepository.findById(itemId)
                        .orElseThrow(() -> new ResourceNotFoundException("Artist not found")));
                artistFavouriteRepository.save(favourite);
            }
            default -> throw new ResourceNotFoundException("Favourite type not found");
        }
    }

    @Transactional
    public void removeFavourite(String username, String type, Long itemId) {
        switch (type) {
            case "tracks" -> trackFavouriteRepository.findByUserUsernameAndTrackId(username, itemId)
                    .ifPresent(trackFavouriteRepository::delete);
            case "albums" -> albumFavouriteRepository.findByUserUsernameAndAlbumId(username, itemId)
                    .ifPresent(albumFavouriteRepository::delete);
            case "artists" -> artistFavouriteRepository.findByUserUsernameAndArtistId(username, itemId)
                    .ifPresent(artistFavouriteRepository::delete);
            default -> throw new ResourceNotFoundException("Favourite type not found");
        }
    }

    public boolean isFavourite(String username, String type, Long itemId) {
        return switch (type) {
            case "tracks" -> trackFavouriteRepository.existsByUserUsernameAndTrackId(username, itemId);
            case "albums" -> albumFavouriteRepository.existsByUserUsernameAndAlbumId(username, itemId);
            case "artists" -> artistFavouriteRepository.existsByUserUsernameAndArtistId(username, itemId);
            default -> throw new ResourceNotFoundException("Favourite type not found");
        };
    }

    @Transactional(readOnly = true)
    public FavouriteIdsDTO getFavouriteIds(String username) {
        findUser(username);
        FavouriteIdsDTO dto = new FavouriteIdsDTO();
        dto.setArtists(artistFavouriteRepository.findByUserUsernameOrderByCreatedAtDesc(username)
                .stream().map(item -> item.getArtist().getId()).toList());
        dto.setAlbums(albumFavouriteRepository.findByUserUsernameOrderByCreatedAtDesc(username)
                .stream().map(item -> item.getAlbum().getId()).toList());
        dto.setTracks(trackFavouriteRepository.findByUserUsernameOrderByCreatedAtDesc(username)
                .stream().map(item -> item.getTrack().getId()).toList());
        return dto;
    }

    @Transactional(readOnly = true)
    public ProfileDTO getProfile(String username, String viewerUsername) {
        UserEntity user = findUser(username);
        boolean ownerIsViewing = username.equals(viewerUsername);
        List<ArtistEntity> artists = artistFavouriteRepository.findByUserUsernameOrderByCreatedAtDesc(username)
                .stream().map(ArtistFavouriteEntity::getArtist).toList();
        List<AlbumEntity> albums = albumFavouriteRepository.findByUserUsernameOrderByCreatedAtDesc(username)
                .stream().map(AlbumFavouriteEntity::getAlbum).toList();
        List<TrackEntity> tracks = trackFavouriteRepository.findByUserUsernameOrderByCreatedAtDesc(username)
                .stream().map(TrackFavouriteEntity::getTrack).toList();
        Map<String, Long> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        tracks.forEach(track -> track.getGenres().forEach(genre -> counts.merge(genre.getName(), 1L, Long::sum)));
        albums.forEach(album -> album.getGenres().forEach(genre -> counts.merge(genre.getName(), 1L, Long::sum)));

        ProfileDTO dto = new ProfileDTO();
        dto.setUsername(username);
        dto.setDisplayName(user.getDisplayName() == null || user.getDisplayName().isBlank()
                ? username : user.getDisplayName());
        dto.setBio(user.getBio());
        dto.setAvatarUrl(user.getAvatarImage() == null ? null : "/api/profiles/" + username + "/avatar");
        dto.setShowArtists(user.isShowArtists());
        dto.setShowAlbums(user.isShowAlbums());
        dto.setShowTracks(user.isShowTracks());
        dto.setShowTasteProfile(user.isShowTasteProfile());
        if (ownerIsViewing || user.isShowArtists()) {
            dto.setFavouriteArtists(artists.stream().map(this::convertArtist).toList());
        }
        if (ownerIsViewing || user.isShowAlbums()) {
            dto.setFavouriteAlbums(albums.stream().map(this::convertAlbum).toList());
        }
        if (ownerIsViewing || user.isShowTracks()) {
            dto.setFavouriteTracks(tracks.stream().map(this::convertTrack).toList());
        }
        if (ownerIsViewing || user.isShowTasteProfile()) dto.setGenreStatistics(counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> new GenreStatDTO(entry.getKey(), entry.getValue())).toList());
        return dto;
    }

    @Transactional
    public ProfileDTO updateProfile(String username, UpdateProfileDTO request) {
        UserEntity user = findUser(username);
        user.setDisplayName(request.getDisplayName().trim());
        user.setBio(cleanOptional(request.getBio()));
        user.setShowArtists(request.isShowArtists());
        user.setShowAlbums(request.isShowAlbums());
        user.setShowTracks(request.isShowTracks());
        user.setShowTasteProfile(request.isShowTasteProfile());
        userRepository.save(user);
        return getProfile(username, username);
    }

    @Transactional
    public ProfileDTO updateAvatar(String username, MultipartFile file) {
        if (file.isEmpty()) throw new BusinessException("Choose an image first");
        if (file.getSize() > MAX_AVATAR_SIZE) throw new BusinessException("Avatar image must be smaller than 2 MB");
        if (!AVATAR_TYPES.contains(file.getContentType())) {
            throw new BusinessException("Avatar must be a JPG, PNG or WebP image");
        }

        UserEntity user = findUser(username);
        try {
            user.setAvatarImage(file.getBytes());
        } catch (IOException exception) {
            throw new BusinessException("Could not read avatar image");
        }
        user.setAvatarContentType(file.getContentType());
        userRepository.save(user);
        return getProfile(username, username);
    }

    @Transactional(readOnly = true)
    public ProfileAvatarDTO getAvatar(String username) {
        UserEntity user = findUser(username);
        if (user.getAvatarImage() == null) throw new ResourceNotFoundException("Avatar not found");
        return new ProfileAvatarDTO(user.getAvatarImage(), user.getAvatarContentType());
    }

    private String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private UserEntity findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private TrackDTO convertTrack(TrackEntity track) {
        TrackDTO dto = new TrackDTO();
        dto.setId(track.getId()); dto.setTitle(track.getTitle()); dto.setDurationSeconds(track.getDurationSeconds());
        dto.setArtistId(track.getArtist().getId()); dto.setArtistName(track.getArtist().getName());
        if (track.getAlbum() != null) {
            dto.setAlbumId(track.getAlbum().getId());
            dto.setAlbumTitle(track.getAlbum().getTitle());
            dto.setAlbumCoverUrl(track.getAlbum().getCoverUrl());
        }
        dto.setGenres(track.getGenres().stream().map(genre -> {
            GenreDTO item = new GenreDTO(); item.setId(genre.getId()); item.setName(genre.getName()); return item;
        }).toList());
        return dto;
    }

    private ArtistDTO convertArtist(ArtistEntity artist) {
        ArtistDTO dto = new ArtistDTO();
        dto.setId(artist.getId()); dto.setName(artist.getName());
        dto.setMusicBrainzId(artist.getMusicBrainzId());
        return dto;
    }

    private AlbumDTO convertAlbum(AlbumEntity album) {
        AlbumDTO dto = new AlbumDTO();
        dto.setId(album.getId()); dto.setTitle(album.getTitle()); dto.setCoverUrl(album.getCoverUrl());
        dto.setReleaseYear(album.getReleaseYear()); dto.setArtistId(album.getArtist().getId());
        dto.setArtistName(album.getArtist().getName()); dto.setMusicBrainzId(album.getMusicBrainzId());
        dto.setGenres(album.getGenres().stream().map(genre -> {
            GenreDTO item = new GenreDTO(); item.setId(genre.getId()); item.setName(genre.getName()); return item;
        }).toList());
        return dto;
    }
}
