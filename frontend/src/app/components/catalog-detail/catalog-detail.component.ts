import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Album, ArtistDetail, Track } from '../../models/catalog.model';
import { AuthService } from '../../services/auth.service';
import { CatalogService } from '../../services/catalog.service';
import { FavouriteType, ProfileService } from '../../services/profile.service';
import { NavigationHistoryService } from '../../services/navigation-history.service';

@Component({
  selector: 'app-catalog-detail',
  imports: [CommonModule, RouterLink],
  templateUrl: './catalog-detail.component.html',
  styleUrl: './catalog-detail.component.css'
})
export class CatalogDetailComponent implements OnInit {

  type = '';
  artistDetail: ArtistDetail | null = null;
  album: Album | null = null;
  track: Track | null = null;
  loading = true;
  errorMessage = '';
  favourite = false;
  constructor(
    private route: ActivatedRoute,
    private catalogService: CatalogService,
    private profileService: ProfileService,
    private navigationHistory: NavigationHistoryService,
    public authService: AuthService
  ) {
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.type = params.get('type') || '';
      const rawId = params.get('id') || '';
      if (!rawId || !['artists', 'albums', 'tracks'].includes(this.type)) {
        this.errorMessage = 'This catalogue page does not exist';
        this.loading = false;
        return;
      }
      this.loadDetail(Number(rawId));
    });
  }

  get backLabel(): string {
    return 'Back to ' + this.navigationHistory.getBackLabel('catalogue');
  }

  goBack(): void {
    this.navigationHistory.goBack('/catalog');
  }

  formatDuration(seconds: number | null): string {
    if (!seconds) {
      return 'Duration not specified';
    }
    return Math.floor(seconds / 60) + ':' + String(seconds % 60).padStart(2, '0');
  }

  toggleFavourite(): void {
    const id = this.currentFavouriteId();
    const type = this.type as FavouriteType;
    if (!id) return;
    const request = this.favourite
      ? this.profileService.removeFavourite(type, id)
      : this.profileService.addFavourite(type, id);
    request.subscribe({
      next: () => this.favourite = !this.favourite,
      error: e => this.errorMessage = e.error?.message || 'Could not update favourite'
    });
  }

  private loadDetail(id: number): void {
    this.loading = true;
    this.errorMessage = '';
    this.artistDetail = null;
    this.album = null;
    this.track = null;

    if (this.type === 'artists') {
      this.catalogService.getArtist(id).subscribe({
        next: detail => { this.artistDetail = detail; this.loading = false; this.loadFavourite(detail.artist.id); },
        error: error => this.showError(error)
      });
    } else if (this.type === 'albums') {
      this.catalogService.getAlbum(id).subscribe({
        next: album => { this.album = album; this.loading = false; this.loadFavourite(album.id); },
        error: error => this.showError(error)
      });
    } else {
      this.catalogService.getTrack(id).subscribe({
        next: track => {
          this.track = track;
          this.loading = false;
          if (track.id) {
            this.loadFavourite(track.id);
          }
        },
        error: error => this.showError(error)
      });
    }
  }

  private showError(error: any): void {
    this.loading = false;
    this.errorMessage = error.error?.message || 'Could not load this catalogue item';
  }

  private loadFavourite(id: number | null): void {
    this.favourite = false;
    if (!id || !this.authService.isLoggedIn()) return;
    this.profileService.isFavourite(this.type as FavouriteType, id)
      .subscribe(value => this.favourite = value.favourite);
  }

  private currentFavouriteId(): number | null {
    if (this.artistDetail) return this.artistDetail.artist.id;
    if (this.album) return this.album.id;
    return this.track?.id || null;
  }
}
