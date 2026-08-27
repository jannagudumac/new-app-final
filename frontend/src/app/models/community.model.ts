import { Album, Artist, Track } from './catalog.model';
export interface GenreStatistic { genre: string; count: number; }
export interface FavouriteIds { artists: number[]; albums: number[]; tracks: number[]; }
export interface UserProfile {
  username: string;
  bio: string | null;
  avatarUrl: string | null;
  showArtists: boolean;
  showAlbums: boolean;
  showTracks: boolean;
  showTasteProfile: boolean;
  favouriteArtists: Artist[];
  favouriteAlbums: Album[];
  favouriteTracks: Track[];
  genreStatistics: GenreStatistic[];
}
export interface UpdateProfile {
  bio: string;
  showArtists: boolean;
  showAlbums: boolean;
  showTracks: boolean;
  showTasteProfile: boolean;
}
export interface Invitation { id: number; wallId: number; wallName: string; invitedByUsername: string; status: string; }
export interface WallMember { username: string; role: 'OWNER' | 'MEMBER'; }
export interface Friend { username: string; }
export interface FriendRequest { id: number; username: string; }
export type FriendshipStatus = 'NONE' | 'FRIEND' | 'PENDING_SENT' | 'PENDING_RECEIVED';
export interface UserSearchResult { username: string; friendshipStatus: FriendshipStatus; }
export interface Concert { name: string; date: string; venue: string; city: string; url: string; }
