import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  Album,
  Artist,
  ArtistDetail,
  CatalogSearchResult,
  CatalogSuggestion,
  Track
} from '../models/catalog.model';

@Injectable({ providedIn: 'root' })
export class CatalogService {

  private apiUrl = environment.apiUrl + '/catalog';

  constructor(private http: HttpClient) {
  }

  search(query = ''): Observable<CatalogSearchResult> {
    const params = new HttpParams().set('query', query);
    return this.http.get<CatalogSearchResult>(this.apiUrl + '/search', { params });
  }

  getSuggestions(query: string): Observable<CatalogSuggestion[]> {
    const params = new HttpParams().set('query', query);
    return this.http.get<CatalogSuggestion[]>(this.apiUrl + '/suggestions', { params });
  }

  getArtist(id: number): Observable<ArtistDetail> {
    return this.http.get<ArtistDetail>(this.apiUrl + '/artists/' + id);
  }

  getAlbum(id: number): Observable<Album> {
    return this.http.get<Album>(this.apiUrl + '/albums/' + id);
  }

  getTrack(id: number): Observable<Track> {
    return this.http.get<Track>(this.apiUrl + '/tracks/' + id);
  }

}
