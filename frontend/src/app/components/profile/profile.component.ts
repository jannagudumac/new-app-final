import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { GenreStatistic, UpdateProfile, UserProfile } from '../../models/community.model';
import { AuthService } from '../../services/auth.service';
import { CommunityService } from '../../services/community.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-profile',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {

  profile: UserProfile | null = null;
  errorMessage = '';
  loading = true;
  showAllArtists = false;
  showAllAlbums = false;
  showAllTracks = false;
  editing = false;
  saving = false;
  uploadingAvatar = false;
  avatarVersion = 0;
  editError = '';
  editModel: UpdateProfile | null = null;
  readonly genreColours = ['#f36c5f', '#2f8f99', '#7b5aa6', '#e3a75c', '#5f8f72', '#cf7692', '#7198b8'];

  constructor(
    private route: ActivatedRoute,
    private service: CommunityService,
    public auth: AuthService
  ) {
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.profile = null;
      this.load(params.get('username') || this.auth.getUsername() || '');
    });
  }

  get favouriteCount(): number {
    if (!this.profile) return 0;
    return this.profile.favouriteArtists.length
      + this.profile.favouriteAlbums.length
      + this.profile.favouriteTracks.length;
  }

  get isOwnProfile(): boolean {
    return !!this.profile && this.profile.username === this.auth.getUsername();
  }

  get avatarSrc(): string | null {
    if (!this.profile?.avatarUrl) return null;
    const server = environment.apiUrl.replace(/\/api$/, '');
    return `${server}${this.profile.avatarUrl}?v=${this.avatarVersion}`;
  }

  openEditor(): void {
    if (!this.profile || !this.isOwnProfile) return;
    this.editModel = {
      displayName: this.profile.displayName,
      bio: this.profile.bio || '',
      showArtists: this.profile.showArtists,
      showAlbums: this.profile.showAlbums,
      showTracks: this.profile.showTracks,
      showTasteProfile: this.profile.showTasteProfile
    };
    this.editError = '';
    this.editing = true;
  }

  chooseAvatar(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file || !this.isOwnProfile) return;

    this.uploadingAvatar = true;
    this.errorMessage = '';
    this.service.uploadAvatar(file).subscribe({
      next: profile => {
        this.profile = profile;
        this.avatarVersion = Date.now();
        this.uploadingAvatar = false;
      },
      error: error => {
        this.errorMessage = error.error?.message || 'Could not upload avatar';
        this.uploadingAvatar = false;
      }
    });
  }

  closeEditor(): void {
    if (this.saving) return;
    this.editing = false;
    this.editModel = null;
  }

  saveProfile(): void {
    if (!this.editModel || !this.editModel.displayName.trim()) return;
    this.saving = true;
    this.editError = '';
    this.service.updateProfile(this.editModel).subscribe({
      next: profile => {
        this.profile = profile;
        this.saving = false;
        this.closeEditor();
      },
      error: error => {
        this.editError = error.error?.message || 'Could not update profile';
        this.saving = false;
      }
    });
  }

  get visibleArtists() {
    const artists = this.profile?.favouriteArtists || [];
    return this.showAllArtists ? artists : artists.slice(0, 6);
  }

  get visibleAlbums() {
    const albums = this.profile?.favouriteAlbums || [];
    return this.showAllAlbums ? albums : albums.slice(0, 6);
  }

  get visibleTracks() {
    const tracks = this.profile?.favouriteTracks || [];
    return this.showAllTracks ? tracks : tracks.slice(0, 6);
  }

  get visibleGenres(): GenreStatistic[] {
    const statistics = this.profile?.genreStatistics || [];
    if (statistics.length <= 7) return statistics;

    const leading = statistics.slice(0, 7);
    const otherCount = statistics.slice(7).reduce((sum, stat) => sum + stat.count, 0);
    return [...leading, { genre: 'Other', count: otherCount }];
  }

  get genrePieBackground(): string {
    const statistics = this.visibleGenres;
    const total = statistics.reduce((sum, stat) => sum + stat.count, 0);
    if (!total) return '#d8e9eb';

    let start = 0;
    const slices = statistics.map((stat, index) => {
      const end = start + stat.count / total * 100;
      const slice = `${this.genreColour(index)} ${start}% ${end}%`;
      start = end;
      return slice;
    });
    return `conic-gradient(${slices.join(', ')})`;
  }

  genreColour(index: number): string {
    return this.genreColours[index % this.genreColours.length];
  }

  scrollFavourites(container: HTMLElement, direction: -1 | 1): void {
    const card = container.querySelector<HTMLElement>('.favourite-card');
    const step = card ? card.offsetWidth + 16 : container.clientWidth * 0.75;
    const maximum = container.scrollWidth - container.clientWidth;
    const atStart = container.scrollLeft <= 2;
    const atEnd = container.scrollLeft >= maximum - 2;

    if (direction === 1 && atEnd) {
      container.scrollTo({ left: 0, behavior: 'smooth' });
    } else if (direction === -1 && atStart) {
      container.scrollTo({ left: maximum, behavior: 'smooth' });
    } else {
      container.scrollBy({ left: direction * step, behavior: 'smooth' });
    }
  }

  load(username: string): void {
    this.loading = true;
    this.errorMessage = '';
    this.service.getProfile(username).subscribe({
      next: value => {
        this.profile = value;
        this.showAllArtists = false;
        this.showAllAlbums = false;
        this.showAllTracks = false;
        this.loading = false;
      },
      error: error => {
        this.errorMessage = error.error?.message || 'Could not load profile';
        this.loading = false;
      }
    });
  }
}
