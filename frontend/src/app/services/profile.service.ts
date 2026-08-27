import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { FavouriteIds, UpdateProfile, UserProfile } from '../models/community.model';

export type FavouriteType = 'artists' | 'albums' | 'tracks';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private api = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getProfile(username: string): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.api}/profiles/${username}`);
  }

  updateProfile(request: UpdateProfile): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.api}/profiles/me`, request);
  }

  uploadAvatar(file: File): Observable<UserProfile> {
    const data = new FormData();
    data.append('file', file);
    return this.http.post<UserProfile>(`${this.api}/profiles/me/avatar`, data);
  }

  getFavouriteIds(): Observable<FavouriteIds> {
    return this.http.get<FavouriteIds>(`${this.api}/favourites`);
  }

  isFavourite(type: FavouriteType, itemId: number): Observable<{ favourite: boolean }> {
    return this.http.get<{ favourite: boolean }>(`${this.api}/favourites/${type}/${itemId}`);
  }

  addFavourite(type: FavouriteType, itemId: number): Observable<void> {
    return this.http.post<void>(`${this.api}/favourites/${type}/${itemId}`, {});
  }

  removeFavourite(type: FavouriteType, itemId: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/favourites/${type}/${itemId}`);
  }
}
