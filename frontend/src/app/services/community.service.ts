import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Concert, FavouriteIds, Friend, FriendRequest, Invitation, UpdateProfile, UserProfile, UserSearchResult, WallMember } from '../models/community.model';

export type FavouriteType = 'artists' | 'albums' | 'tracks';

@Injectable({ providedIn: 'root' })
export class CommunityService {
  private api = environment.apiUrl;
  constructor(private http: HttpClient) {}
  getProfile(username: string): Observable<UserProfile> { return this.http.get<UserProfile>(this.api + '/profiles/' + username); }
  updateProfile(request: UpdateProfile): Observable<UserProfile> { return this.http.put<UserProfile>(this.api + '/profiles/me', request); }
  uploadAvatar(file: File): Observable<UserProfile> {
    const data = new FormData();
    data.append('file', file);
    return this.http.post<UserProfile>(this.api + '/profiles/me/avatar', data);
  }
  getFavouriteIds(): Observable<FavouriteIds> { return this.http.get<FavouriteIds>(this.api + '/favourites'); }
  isFavourite(type: FavouriteType, itemId: number): Observable<{ favourite: boolean }> { return this.http.get<{ favourite: boolean }>(this.api + '/favourites/' + type + '/' + itemId); }
  addFavourite(type: FavouriteType, itemId: number): Observable<void> { return this.http.post<void>(this.api + '/favourites/' + type + '/' + itemId, {}); }
  removeFavourite(type: FavouriteType, itemId: number): Observable<void> { return this.http.delete<void>(this.api + '/favourites/' + type + '/' + itemId); }
  invite(wallId: number, username: string): Observable<Invitation> { return this.http.post<Invitation>(this.api + '/walls/' + wallId + '/invitations', { username }); }
  getInvitations(): Observable<Invitation[]> { return this.http.get<Invitation[]>(this.api + '/invitations'); }
  answerInvitation(id: number, accept: boolean): Observable<void> { return this.http.post<void>(this.api + '/invitations/' + id + '/' + (accept ? 'accept' : 'reject'), {}); }
  getMembers(wallId: number): Observable<WallMember[]> { return this.http.get<WallMember[]>(this.api + '/walls/' + wallId + '/members'); }
  getFriends(): Observable<Friend[]> { return this.http.get<Friend[]>(this.api + '/friends'); }
  searchUsers(query: string): Observable<UserSearchResult[]> {
    return this.http.get<UserSearchResult[]>(this.api + '/friends/search', {
      params: new HttpParams().set('query', query)
    });
  }
  getFriendRequests(): Observable<FriendRequest[]> { return this.http.get<FriendRequest[]>(this.api + '/friend-requests'); }
  sendFriendRequest(username: string): Observable<FriendRequest> { return this.http.post<FriendRequest>(this.api + '/friend-requests/' + encodeURIComponent(username), {}); }
  answerFriendRequest(id: number, accept: boolean): Observable<void> { return this.http.post<void>(this.api + '/friend-requests/' + id + '/' + (accept ? 'accept' : 'reject'), {}); }
  removeFriend(username: string): Observable<void> { return this.http.delete<void>(this.api + '/friends/' + encodeURIComponent(username)); }
  searchConcerts(artist: string, city: string): Observable<Concert[]> {
    let params = new HttpParams().set('artist', artist); if (city) params = params.set('city', city);
    return this.http.get<Concert[]>(this.api + '/concerts', { params });
  }
}
