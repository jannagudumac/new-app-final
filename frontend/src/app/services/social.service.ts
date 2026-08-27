import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Friend, FriendRequest, Invitation, UserSearchResult, WallMember } from '../models/community.model';

@Injectable({ providedIn: 'root' })
export class SocialService {
  private api = environment.apiUrl;

  constructor(private http: HttpClient) {}

  invite(wallId: number, username: string): Observable<Invitation> {
    return this.http.post<Invitation>(`${this.api}/walls/${wallId}/invitations`, { username });
  }

  getInvitations(): Observable<Invitation[]> {
    return this.http.get<Invitation[]>(`${this.api}/invitations`);
  }

  answerInvitation(id: number, accept: boolean): Observable<void> {
    const answer = accept ? 'accept' : 'reject';
    return this.http.post<void>(`${this.api}/invitations/${id}/${answer}`, {});
  }

  getMembers(wallId: number): Observable<WallMember[]> {
    return this.http.get<WallMember[]>(`${this.api}/walls/${wallId}/members`);
  }

  getFriends(): Observable<Friend[]> {
    return this.http.get<Friend[]>(`${this.api}/friends`);
  }

  searchUsers(query: string): Observable<UserSearchResult[]> {
    const params = new HttpParams().set('query', query);
    return this.http.get<UserSearchResult[]>(`${this.api}/friends/search`, { params });
  }

  getFriendRequests(): Observable<FriendRequest[]> {
    return this.http.get<FriendRequest[]>(`${this.api}/friend-requests`);
  }

  sendFriendRequest(username: string): Observable<FriendRequest> {
    return this.http.post<FriendRequest>(
      `${this.api}/friend-requests/${encodeURIComponent(username)}`,
      {}
    );
  }

  answerFriendRequest(id: number, accept: boolean): Observable<void> {
    const answer = accept ? 'accept' : 'reject';
    return this.http.post<void>(`${this.api}/friend-requests/${id}/${answer}`, {});
  }

  removeFriend(username: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/friends/${encodeURIComponent(username)}`);
  }
}
