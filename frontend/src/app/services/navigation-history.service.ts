import { Injectable } from '@angular/core';
import { NavigationEnd, NavigationStart, Router } from '@angular/router';
import { filter } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class NavigationHistoryService {
  private readonly storageKey = 'music-wall-navigation-history';
  private history: string[] = this.readHistory();
  private navigatingBack = false;
  private browserNavigation = false;

  constructor(private router: Router) {
    this.router.events
      .pipe(filter((event): event is NavigationStart => event instanceof NavigationStart))
      .subscribe(event => this.browserNavigation = event.navigationTrigger === 'popstate');

    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(event => this.recordNavigation(event.urlAfterRedirects));
  }

  goBack(fallbackUrl = '/catalog'): void {
    if (this.history.length < 2) {
      void this.router.navigateByUrl(fallbackUrl);
      return;
    }

    this.history.pop();
    const previousUrl = this.history[this.history.length - 1];
    this.saveHistory();
    this.navigatingBack = true;
    void this.router.navigateByUrl(previousUrl).catch(() => {
      this.navigatingBack = false;
      void this.router.navigateByUrl(fallbackUrl);
    });
  }

  getBackLabel(fallbackLabel = 'catalogue'): string {
    return this.history.length < 2
      ? fallbackLabel
      : this.pageLabel(this.history[this.history.length - 2]);
  }

  private recordNavigation(url: string): void {
    if (this.navigatingBack) {
      this.navigatingBack = false;
    } else if (this.browserNavigation) {
      const existingIndex = this.history.lastIndexOf(url);
      this.history = existingIndex >= 0
        ? this.history.slice(0, existingIndex + 1)
        : [...this.history, url];
    } else if (this.history[this.history.length - 1] !== url) {
      this.history.push(url);
    }

    this.browserNavigation = false;
    this.history = this.history.slice(-30);
    this.saveHistory();
  }

  private pageLabel(url: string): string {
    const path = url.split(/[?#]/)[0];
    if (path === '/profile' || path.startsWith('/users/')) return 'profile';
    if (/^\/walls\/\d+$/.test(path)) return 'wall';
    if (path === '/walls') return 'my walls';
    if (/^\/catalog\/artists\/\d+$/.test(path)) return 'artist';
    if (/^\/catalog\/albums\/\d+$/.test(path)) return 'album';
    if (/^\/catalog\/tracks\/\d+$/.test(path)) return 'track';
    if (path === '/catalog') return 'catalogue';
    if (path === '/friends') return 'friends';
    if (path === '/dashboard' || path === '/') return 'dashboard';
    return 'previous page';
  }

  private readHistory(): string[] {
    try {
      const stored: unknown = JSON.parse(sessionStorage.getItem(this.storageKey) || '[]');
      return Array.isArray(stored) && stored.every(item => typeof item === 'string')
        ? stored.slice(-30)
        : [];
    } catch {
      return [];
    }
  }

  private saveHistory(): void {
    sessionStorage.setItem(this.storageKey, JSON.stringify(this.history));
  }
}
