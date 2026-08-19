import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter, Subscription } from 'rxjs';

import { AuthService } from '../../services/auth.service';
import { CommunityService } from '../../services/community.service';

@Component({
  selector: 'app-sidebar',
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css'
})
export class SidebarComponent implements OnInit, OnDestroy {

  @Input() collapsed = false;
  @Output() collapseRequested = new EventEmitter<void>();
  pendingInvitationCount = 0;
  pendingFriendRequestCount = 0;
  private pendingInvitationIds: number[] = [];
  private pendingFriendRequestIds: number[] = [];
  private navigationSubscription?: Subscription;

  constructor(
    public authService: AuthService,
    private router: Router,
    private communityService: CommunityService
  ) {
  }

  ngOnInit(): void {
    this.communityService.getInvitations().subscribe({
      next: invitations => {
        this.pendingInvitationIds = invitations
          .filter(invitation => invitation.status === 'PENDING')
          .map(invitation => invitation.id);
        this.updateInvitationBadge();
      },
      error: () => this.pendingInvitationCount = 0
    });
    this.communityService.getFriendRequests().subscribe({
      next: requests => {
        this.pendingFriendRequestIds = requests.map(request => request.id);
        this.updateFriendRequestBadge();
      },
      error: () => this.pendingFriendRequestCount = 0
    });

    this.navigationSubscription = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(event => this.markCurrentPageSeen(event.urlAfterRedirects));
  }

  ngOnDestroy(): void {
    this.navigationSubscription?.unsubscribe();
  }

  get usernameInitial(): string {
    return (this.authService.getUsername() || 'U').charAt(0).toUpperCase();
  }

  toggleCollapse(): void {
    this.collapseRequested.emit();
  }

  markFriendRequestsSeen(): void {
    this.saveSeenIds('friendRequests', this.pendingFriendRequestIds);
    this.pendingFriendRequestCount = 0;
  }

  markFriendsPageSeen(): void {
    this.markFriendRequestsSeen();
    this.markInvitationsSeen();
  }

  markInvitationsSeen(): void {
    this.saveSeenIds('wallInvitations', this.pendingInvitationIds);
    this.pendingInvitationCount = 0;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private updateFriendRequestBadge(): void {
    if (this.router.url.startsWith('/friends')) {
      this.markFriendRequestsSeen();
      return;
    }
    this.pendingFriendRequestCount = this.countUnread(
      'friendRequests',
      this.pendingFriendRequestIds
    );
  }

  private updateInvitationBadge(): void {
    if (this.router.url.startsWith('/friends')) {
      this.markInvitationsSeen();
      return;
    }
    this.pendingInvitationCount = this.countUnread(
      'wallInvitations',
      this.pendingInvitationIds
    );
  }

  private markCurrentPageSeen(url: string): void {
    if (url.startsWith('/friends')) this.markFriendsPageSeen();
  }

  private countUnread(type: string, currentIds: number[]): number {
    const seenIds = new Set(this.readSeenIds(type));
    return currentIds.filter(id => !seenIds.has(id)).length;
  }

  private readSeenIds(type: string): number[] {
    try {
      return JSON.parse(localStorage.getItem(this.seenStorageKey(type)) || '[]');
    } catch {
      return [];
    }
  }

  private saveSeenIds(type: string, ids: number[]): void {
    const seenIds = new Set([...this.readSeenIds(type), ...ids]);
    localStorage.setItem(this.seenStorageKey(type), JSON.stringify([...seenIds]));
  }

  private seenStorageKey(type: string): string {
    return 'music-wall.seen.' + this.authService.getUsername() + '.' + type;
  }
}
