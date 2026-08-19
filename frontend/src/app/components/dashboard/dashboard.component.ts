import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { MusicWall } from '../../models/music-wall.model';
import { AuthService } from '../../services/auth.service';
import { CommunityService } from '../../services/community.service';
import { MusicWallService } from '../../services/music-wall.service';

@Component({
  selector: 'app-dashboard',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {

  walls: MusicWall[] = [];
  loading = false;
  errorMessage = '';
  pendingInvitationCount = 0;
  favouriteTrackCount = 0;
  searchQuery = '';

  constructor(
    private musicWallService: MusicWallService,
    private communityService: CommunityService,
    private authService: AuthService,
    private router: Router
  ) {
  }

  ngOnInit(): void {
    this.loadWalls();
    this.loadInvitations();
    this.loadProfileSummary();
  }

  get username(): string {
    return this.authService.getUsername() || 'music lover';
  }

  get recentWalls(): MusicWall[] {
    return this.walls.slice(0, 3);
  }

  loadWalls(): void {
    this.loading = true;
    this.errorMessage = '';

    this.musicWallService.getMyWalls().subscribe({
      next: walls => {
        this.walls = walls;
        this.loading = false;
      },
      error: error => {
        this.loading = false;
        this.errorMessage = error.error?.message || 'Could not load walls';
      }
    });
  }

  loadInvitations(): void {
    this.communityService.getInvitations().subscribe({
      next: invitations => {
        this.pendingInvitationCount = invitations.filter(
          invitation => invitation.status === 'PENDING'
        ).length;
      },
      error: () => {
        this.pendingInvitationCount = 0;
      }
    });
  }

  loadProfileSummary(): void {
    const username = this.authService.getUsername();
    if (!username) {
      return;
    }

    this.communityService.getProfile(username).subscribe({
      next: profile => this.favouriteTrackCount = profile.favouriteTracks.length,
      error: () => this.favouriteTrackCount = 0
    });
  }

  searchCatalog(): void {
    const query = this.searchQuery.trim();
    this.router.navigate(['/catalog'], {
      queryParams: query ? { query: query } : {}
    });
  }

}
