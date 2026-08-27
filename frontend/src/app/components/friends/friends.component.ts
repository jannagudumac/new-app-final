import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Friend, FriendRequest, Invitation, UserSearchResult } from '../../models/community.model';
import { SocialService } from '../../services/social.service';

@Component({
  selector: 'app-friends',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './friends.component.html',
  styleUrl: './friends.component.css'
})
export class FriendsComponent implements OnInit {

  friends: Friend[] = [];
  requests: FriendRequest[] = [];
  invitations: Invitation[] = [];
  searchResults: UserSearchResult[] = [];
  query = '';
  loading = true;
  searching = false;
  message = '';
  errorMessage = '';

  constructor(private socialService: SocialService) {
  }

  ngOnInit(): void {
    this.loadPage();
  }

  search(): void {
    const query = this.query.trim();
    if (query.length < 2) {
      this.errorMessage = 'Enter at least two characters.';
      return;
    }

    this.searching = true;
    this.message = '';
    this.errorMessage = '';
    this.socialService.searchUsers(query).subscribe({
      next: results => {
        this.searchResults = results;
        this.searching = false;
        if (!results.length) this.message = 'No users found.';
      },
      error: error => {
        this.searching = false;
        this.errorMessage = error.error?.message || 'Could not search users';
      }
    });
  }

  sendRequest(result: UserSearchResult): void {
    this.clearMessages();
    this.socialService.sendFriendRequest(result.username).subscribe({
      next: () => {
        result.friendshipStatus = 'PENDING_SENT';
        this.message = 'Friend request sent to ' + result.username + '.';
      },
      error: error => this.errorMessage = error.error?.message || 'Could not send friend request'
    });
  }

  answerRequest(request: FriendRequest, accept: boolean): void {
    this.clearMessages();
    this.socialService.answerFriendRequest(request.id, accept).subscribe({
      next: () => {
        this.message = accept ? request.username + ' is now your friend.' : 'Friend request declined.';
        this.loadPage(false);
        this.refreshSearchStatus(request.username, accept ? 'FRIEND' : 'NONE');
      },
      error: error => this.errorMessage = error.error?.message || 'Could not answer friend request'
    });
  }

  answerInvitation(invitation: Invitation, accept: boolean): void {
    this.clearMessages();
    this.socialService.answerInvitation(invitation.id, accept).subscribe({
      next: () => {
        this.invitations = this.invitations.filter(item => item.id !== invitation.id);
        this.message = accept
          ? invitation.wallName + ' was added to My walls.'
          : 'Wall invitation declined.';
      },
      error: error => this.errorMessage = error.error?.message || 'Could not answer wall invitation'
    });
  }

  removeFriend(friend: Friend): void {
    if (!window.confirm('Remove ' + friend.username + ' from your friends?')) return;

    this.clearMessages();
    this.socialService.removeFriend(friend.username).subscribe({
      next: () => {
        this.friends = this.friends.filter(item => item.username !== friend.username);
        this.refreshSearchStatus(friend.username, 'NONE');
        this.message = friend.username + ' was removed from your friends.';
      },
      error: error => this.errorMessage = error.error?.message || 'Could not remove friend'
    });
  }

  private loadPage(showLoading = true): void {
    if (showLoading) this.loading = true;
    this.socialService.getFriends().subscribe({
      next: friends => {
        this.friends = friends;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Could not load friends';
      }
    });
    this.socialService.getFriendRequests().subscribe({
      next: requests => this.requests = requests,
      error: () => this.errorMessage = 'Could not load friend requests'
    });
    this.socialService.getInvitations().subscribe({
      next: invitations => this.invitations = invitations.filter(
        invitation => invitation.status === 'PENDING'
      ),
      error: () => this.errorMessage = 'Could not load wall invitations'
    });
  }

  private refreshSearchStatus(username: string, status: UserSearchResult['friendshipStatus']): void {
    const result = this.searchResults.find(item => item.username === username);
    if (result) result.friendshipStatus = status;
  }

  private clearMessages(): void {
    this.message = '';
    this.errorMessage = '';
  }
}
