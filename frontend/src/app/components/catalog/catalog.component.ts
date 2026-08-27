import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, finalize, switchMap } from 'rxjs/operators';

import { CatalogSearchResult, CatalogSuggestion } from '../../models/catalog.model';
import { AuthService } from '../../services/auth.service';
import { CatalogService } from '../../services/catalog.service';
import { FavouriteType, ProfileService } from '../../services/profile.service';

@Component({
  selector: 'app-catalog',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.css'
})
export class CatalogComponent implements OnInit, OnDestroy {

  result: CatalogSearchResult = { artists: [], albums: [], tracks: [], genres: [], warnings: [] };
  searchForm: FormGroup;
  loading = false;
  errorMessage = '';
  suggestions: CatalogSuggestion[] = [];
  suggestionsLoading = false;
  showSuggestions = false;
  activeSuggestionIndex = -1;
  activeFilter: 'all' | 'tracks' | 'albums' | 'artists' = 'all';
  favouriteIds: Record<FavouriteType, Set<number>> = {
    artists: new Set<number>(), albums: new Set<number>(), tracks: new Set<number>()
  };
  favouriteSaving = '';
  private suggestionSubscription?: Subscription;

  constructor(
    private formBuilder: FormBuilder,
    private catalogService: CatalogService,
    private profileService: ProfileService,
    public authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.searchForm = this.formBuilder.group({ query: [''] });
  }

  ngOnInit(): void {
    const initialQuery = this.route.snapshot.queryParamMap.get('query') || '';
    this.searchForm.patchValue({ query: initialQuery });
    this.setupSuggestions();
    this.loadFavourites();
    this.search();
  }

  ngOnDestroy(): void {
    this.suggestionSubscription?.unsubscribe();
  }

  search(): void {
    this.showSuggestions = false;
    this.activeSuggestionIndex = -1;
    this.loading = true;
    this.errorMessage = '';
    const query = this.searchForm.value.query || '';

    this.catalogService.search(query).subscribe({
      next: result => {
        this.result = result;
        this.loading = false;
      },
      error: error => {
        this.loading = false;
        this.errorMessage = error.error?.message || 'Could not load the catalogue';
      }
    });
  }

  clearSearch(): void {
    this.searchForm.reset({ query: '' });
    this.suggestions = [];
    this.showSuggestions = false;
    this.search();
  }

  setFilter(filter: 'all' | 'tracks' | 'albums' | 'artists'): void {
    this.activeFilter = filter;
  }

  visibleResultCount(): number {
    if (this.activeFilter === 'tracks') {
      return this.result.tracks.length;
    }
    if (this.activeFilter === 'albums') {
      return this.result.albums.length;
    }
    if (this.activeFilter === 'artists') {
      return this.result.artists.length;
    }
    return this.result.artists.length + this.result.albums.length + this.result.tracks.length;
  }

  onSearchFocus(): void {
    if (this.currentQuery().length >= 2) {
      this.showSuggestions = true;
    }
  }

  onSearchBlur(): void {
    window.setTimeout(() => this.showSuggestions = false, 150);
  }

  onSearchKeydown(event: KeyboardEvent): void {
    if (!this.showSuggestions || this.suggestions.length === 0) {
      return;
    }

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.activeSuggestionIndex = Math.min(
        this.activeSuggestionIndex + 1,
        this.suggestions.length - 1
      );
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.activeSuggestionIndex = Math.max(this.activeSuggestionIndex - 1, 0);
    } else if (event.key === 'Enter' && this.activeSuggestionIndex >= 0) {
      event.preventDefault();
      this.selectSuggestion(this.suggestions[this.activeSuggestionIndex]);
    } else if (event.key === 'Escape') {
      event.preventDefault();
      this.showSuggestions = false;
      this.activeSuggestionIndex = -1;
    }
  }

  selectSuggestion(suggestion: CatalogSuggestion): void {
    this.showSuggestions = false;
    const routeType = suggestion.type === 'ARTIST'
      ? 'artists'
      : suggestion.type === 'ALBUM' ? 'albums' : 'tracks';
    this.openResult(routeType, suggestion.id);
  }

  openResult(type: 'artists' | 'albums' | 'tracks', id: string | number | null): void {
    if (!id) return;
    this.router.navigate(['/catalog', type, id]);
  }

  isFavourite(type: FavouriteType, id: number | null): boolean {
    return id !== null && this.favouriteIds[type].has(id);
  }

  toggleFavourite(event: Event, type: FavouriteType, id: number | null): void {
    event.preventDefault();
    event.stopPropagation();
    if (id === null || !this.authService.isLoggedIn()) return;

    const key = type + '-' + id;
    if (this.favouriteSaving === key) return;
    this.favouriteSaving = key;
    const favourite = this.isFavourite(type, id);
    const request = favourite
      ? this.profileService.removeFavourite(type, id)
      : this.profileService.addFavourite(type, id);

    request.subscribe({
      next: () => {
        if (favourite) this.favouriteIds[type].delete(id);
        else this.favouriteIds[type].add(id);
        this.favouriteSaving = '';
      },
      error: error => {
        this.errorMessage = error.error?.message || 'Could not update favourite';
        this.favouriteSaving = '';
      }
    });
  }

  formatDuration(seconds: number | null): string {
    if (!seconds) {
      return '';
    }
    const minutes = Math.floor(seconds / 60);
    return minutes + ':' + String(seconds % 60).padStart(2, '0');
  }

  private setupSuggestions(): void {
    const queryControl = this.searchForm.get('query');
    if (!queryControl) {
      return;
    }

    this.suggestionSubscription = queryControl.valueChanges.pipe(
      debounceTime(650),
      distinctUntilChanged(),
      switchMap(value => {
        const query = String(value || '').trim();
        this.activeSuggestionIndex = -1;

        if (query.length < 2) {
          this.suggestionsLoading = false;
          this.showSuggestions = false;
          return of([] as CatalogSuggestion[]);
        }

        this.suggestionsLoading = true;
        this.showSuggestions = true;
        return this.catalogService.getSuggestions(query).pipe(
          catchError(() => of([] as CatalogSuggestion[])),
          finalize(() => this.suggestionsLoading = false)
        );
      })
    ).subscribe(suggestions => {
      this.suggestions = suggestions;
      this.showSuggestions = this.currentQuery().length >= 2;
    });
  }

  private currentQuery(): string {
    return String(this.searchForm.get('query')?.value || '').trim();
  }

  private loadFavourites(): void {
    if (!this.authService.isLoggedIn()) return;
    this.profileService.getFavouriteIds().subscribe({
      next: ids => {
        this.favouriteIds.artists = new Set(ids.artists);
        this.favouriteIds.albums = new Set(ids.albums);
        this.favouriteIds.tracks = new Set(ids.tracks);
      }
    });
  }
}
