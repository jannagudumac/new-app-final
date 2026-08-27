import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Concert } from '../models/community.model';

@Injectable({ providedIn: 'root' })
export class ConcertService {
  private api = environment.apiUrl;

  constructor(private http: HttpClient) {}

  search(artist: string, city: string): Observable<Concert[]> {
    let params = new HttpParams().set('artist', artist);
    if (city) params = params.set('city', city);
    return this.http.get<Concert[]>(`${this.api}/concerts`, { params });
  }
}
