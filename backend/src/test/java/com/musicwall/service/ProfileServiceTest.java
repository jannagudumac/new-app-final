package com.musicwall.service;
import com.musicwall.entity.ArtistEntity;
import com.musicwall.entity.ArtistFavouriteEntity;
import com.musicwall.entity.UserEntity;
import com.musicwall.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {
    @Mock UserRepository users; @Mock TrackRepository tracks; @Mock AlbumRepository albums; @Mock ArtistRepository artists;
    @Mock TrackFavouriteRepository trackFavourites; @Mock AlbumFavouriteRepository albumFavourites;
    @Mock ArtistFavouriteRepository artistFavourites;
    ProfileService service;
    @BeforeEach void setUp(){service=new ProfileService(users,tracks,albums,artists,trackFavourites,albumFavourites,artistFavourites);}

    @Test void removingFavouriteOnlyDeletesCurrentUsersRelationship(){
        var favourite=new com.musicwall.entity.TrackFavouriteEntity();
        when(trackFavourites.findByUserUsernameAndTrackId("janna",3L)).thenReturn(Optional.of(favourite));
        service.removeFavourite("janna","tracks",3L);
        verify(trackFavourites).delete(favourite);
    }

    @Test void addingArtistFavouriteUsesAuthenticatedUsername(){
        UserEntity user=new UserEntity(); user.setUsername("janna");
        ArtistEntity artist=new ArtistEntity(); artist.setId(4L); artist.setName("David Bowie");
        when(users.findByUsername("janna")).thenReturn(Optional.of(user));
        when(artistFavourites.existsByUserUsernameAndArtistId("janna",4L)).thenReturn(false);
        when(artists.findById(4L)).thenReturn(Optional.of(artist));

        service.addFavourite("janna","artists",4L);

        var saved=org.mockito.ArgumentCaptor.forClass(ArtistFavouriteEntity.class);
        verify(artistFavourites).save(saved.capture());
        assertSame(user,saved.getValue().getUser());
        assertSame(artist,saved.getValue().getArtist());
    }

}
