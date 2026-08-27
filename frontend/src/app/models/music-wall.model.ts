export type WallWallpaper =
  'NONE' |
  'IMAGE_1' |
  'IMAGE_2' |
  'IMAGE_3' |
  'IMAGE_4' |
  'IMAGE_5' |
  'IMAGE_6' |
  'IMAGE_7' |
  'IMAGE_8' |
  'IMAGE_9';

export interface MusicWall {
  id: number;
  name: string;
  description: string | null;
  ownerUsername: string;
  wallpaper: WallWallpaper;
  wallColor: string;
}

export interface CreateMusicWallRequest {
  name: string;
  description: string;
  wallpaper: WallWallpaper;
  wallColor: string;
}

export type MusicItemType = 'TRACK' | 'ALBUM';
export type ListeningStatus = 'TO_LISTEN' | 'LISTENED';
export type SectionNoteColor = 'CREAM' | 'ROSE' | 'PEACH' | 'MINT' | 'SKY' | 'LAVENDER';

export interface MusicItem {
  id: number;
  title: string;
  artist: string;
  itemType: MusicItemType;
  status: ListeningStatus;
  catalogTrackId: number | null;
  catalogAlbumId: number | null;
}

export interface MusicSection {
  id: number;
  name: string;
  noteColor: SectionNoteColor;
  items: MusicItem[];
}

export interface MusicWallDetail extends MusicWall {
  sections: MusicSection[];
}

export interface CreateMusicSectionRequest {
  name: string;
  noteColor: SectionNoteColor;
}

export interface CreateMusicItemRequest {
  status: ListeningStatus;
  catalogTrackId?: number | null;
  catalogAlbumId?: number | null;
}
